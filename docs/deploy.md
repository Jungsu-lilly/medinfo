# 배포 가이드 (앱 쪽 준비물)

인프라(Terraform·GitHub Actions·Blue/Green)를 만들 때 앱이 기대하는 것을 정리한 문서.
비밀값은 이 문서와 저장소에 쓰지 않는다 → GitHub Secrets / AWS Secrets Manager(또는 SSM Parameter Store)에만.

## 1. 구성

```
사용자 ── HTTPS ──▶ ALB ──┬─ /api/*  ──▶ api (Spring Boot, :8080) ──┐
                          └─ 그 밖     ──▶ web (Next.js, :3000) ─────┤ 서버 렌더링 때 api를 직접 호출 (BACKEND_URL)
                                                                     ▼
                                                    PostgreSQL 16 + PostGIS (RDS)
api 안의 Quartz(클러스터) ── 2분/매일/매주/매시간 ──▶ 공공데이터포털 API
```

| 컨테이너 | 이미지 빌드 | 포트 | 헬스체크 |
|---|---|---|---|
| api | 루트 `Dockerfile` | 8080 서비스 / **8081 관리** | `GET :8081/actuator/health/readiness` |
| web | `web/Dockerfile` | 3000 | `GET :3000/healthz` |

- **ALB 경로 라우팅**: `/api/*` → api 대상 그룹, 기본 → web 대상 그룹. 브라우저의 `/api` 호출이 Next를 거치지 않고 바로 Spring으로 감
- **8081(관리 포트)은 ALB 리스너에 열지 않는다**: `/actuator/prometheus`가 외부에 보이면 안 됨. ALB 대상 그룹의 헬스체크 포트만 8081로 지정하고, 보안 그룹은 ALB → api 8081 허용, Prometheus → api 8081 허용
- 로컬에서 운영 이미지 미리 띄우기: `docker compose -f compose.prod-local.yml up --build`

## 2. 환경변수

**api (Spring)** — 값이 없으면 기동 실패 (설정 누락을 바로 알 수 있게)

| 이름 | 예 | 비밀 | 설명 |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | | Dockerfile 기본값 |
| `DB_URL` | `jdbc:postgresql://<rds-endpoint>:5432/medinfo` | | |
| `DB_USERNAME` | | ✔ | |
| `DB_PASSWORD` | | ✔ | |
| `PUBLIC_DATA_SERVICE_KEY` | | ✔ | 공공데이터포털 인증키 (계정당 1개) |
| `DB_POOL_SIZE` | `10` | | 선택. 인스턴스 수 × 풀 크기가 RDS 최대 연결 수를 넘지 않게 |
| `COLLECTORS_ENABLED` | `true` | | 선택. false면 수집·적재 스케줄러 정지 |

**web (Next)** — 빌드 인자(`--build-arg`)와 실행 환경변수 둘 다

| 이름 | 빌드 | 실행 | 설명 |
|---|---|---|---|
| `BACKEND_URL` | ✔ | ✔ | web → api 내부 주소 (예: `http://api.internal:8080`, 내부 ALB 또는 서비스 디스커버리) |
| `SITE_URL` | ✔ | | `https://xn--639ao13amqh.com` (병원값.com의 퓨니코드. 공유 미리보기·robots.txt) |
| `SITE_INDEXING` | ✔ | | 공개 전 `false`(검색엔진 차단), 오픈할 때 `true`로 다시 빌드 |

## 3. Blue/Green 배포 때 앱 쪽에서 맞춰둔 것

- **스케줄 잡 중복 실행 없음**: Quartz 클러스터 모드(같은 DB). 두 버전이 동시에 떠 있어도 같은 잡은 한 번만
- **종료 신호를 받으면 30초 동안 처리 중인 요청을 마무리** (`server.shutdown=graceful`) → ALB 대상 그룹의 등록 해제 지연(deregistration delay)도 30~35초로 맞출 것
- **비급여 적재(1~2시간)는 종료를 기다리지 않음** → 끊겨도 다음 매시간 실행이 Spring Batch 체크포인트부터 이어받음
- **DB 마이그레이션(Flyway)은 새 버전이 기동할 때 실행** → 구버전이 아직 떠 있는 동안에도 동작하는 변경만 한 번에 배포한다
  - 컬럼 추가·테이블 추가: 바로 가능
  - 컬럼 이름 변경·삭제: 2번에 나눠서 (① 새 컬럼 추가 + 양쪽 쓰기 배포 → ② 옛 컬럼 삭제 배포)

## 4. DB (RDS PostgreSQL 16)

- **PostGIS·pg_trgm 확장**: V1 마이그레이션이 `CREATE EXTENSION`을 실행 → RDS 마스터 사용자(rds_superuser)로 접속하면 됨. 앱 전용 사용자를 따로 만들면 확장은 마스터로 먼저 만들어 둘 것
- 데이터 크기: 병원 8만 + 비급여 요약 20만 + 상세 수백만 + 응급실 스냅샷(2분 × 415곳 × 7일 ≈ 200만 행, 일 단위 파티션 자동 삭제)
- 시간대: 앱이 KST를 직접 다루고 컬럼은 `timestamptz` → RDS 파라미터 그룹의 timezone은 기본(UTC) 그대로 둬도 됨

## 5. 첫 배포 후 일어나는 일

1. Flyway V1~V7 적용 (수 초)
2. 병원정보 동기화 시작 (약 80회 호출, 5분)
3. 응급기관 목록 + 매칭 (1회 호출)
4. 비급여 적재 시작 (요약 약 200회 20~30분 → 상세 수천 회 1~2시간). 진행은 README의 "비급여 배치 실행 이력" SQL로 확인
5. 응급실 2분 수집

**인증키는 로컬과 운영이 같다** → 운영이 돌기 시작하면 **로컬 `.env`에 `COLLECTORS_ENABLED=false`**. 응급실 수집만 해도 운영 720회 + 로컬 720회로 일일 한도(1,000)를 넘는다.

## 6. 리소스 (시작값)

| | 메모리 | 메모 |
|---|---|---|
| api | 1GB | 힙은 컨테이너 메모리의 75% (`-XX:MaxRAMPercentage=75`), 메모리 부족 시 즉시 종료 → 재시작 |
| web | 512MB | |
| RDS | 소형 인스턴스부터 | 비급여 상세 적재 때만 쓰기가 몰림 |

## 7. 로그·지표

- api 로그는 **JSON 한 줄**(ECS 형식, stdout) → CloudWatch Logs / Loki에서 필드로 검색
- 지표: `:8081/actuator/prometheus`. 수집 관련 주요 지표는 README 참고 (`collector_last_success_epoch_seconds`, `public_api_quota_used` …)

## 8. 도메인: 병원값.com (2026-11-02 확정)

한글 도메인(IDN)은 시스템 안에서는 **퓨니코드** `xn--639ao13amqh.com`으로 다룬다.
- Route 53 호스팅 영역, ACM 인증서(`xn--639ao13amqh.com` + `www.`), ALB 리스너 규칙, `SITE_URL` 모두 퓨니코드
- 브라우저 주소창에는 한글(병원값.com)로 보임. 카카오톡 등 일부 앱은 링크를 퓨니코드로 보여줄 수 있음
- 문의용 이메일은 한글 도메인 주소가 깨지는 메일 서비스가 있어 일반 메일 주소를 쓰는 게 안전

## 9. 공개(오픈) 체크리스트

- [x] 서비스 이름·도메인 확정 (병원값 / 병원값.com)
- [ ] HTTPS 인증서(ACM, 퓨니코드 도메인)
- [ ] 개인정보처리방침 내용 채우기 (`/privacy`)
- [ ] `SITE_INDEXING=true`로 web 다시 빌드·배포
- [ ] 구글 서치콘솔·네이버 서치어드바이저 등록, sitemap
