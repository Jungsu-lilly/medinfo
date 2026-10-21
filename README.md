# medinfo — 병원 가기 전에 찾아보는 서비스

비급여 진료비 비교 · 병원 검색 · 응급실 실시간 현황. 공공데이터 수집 파이프라인 + DevOps 종합 프로젝트.

- 설계 문서: claude.ai 프로젝트 `claude/의료-프로젝트-전환-계획.md`, `claude/의료-DB-스키마.sql`
- 이름(`medinfo`, 패키지 `com.medinfo`)은 임시. 바꾸려면 `settings.gradle`, 패키지 폴더, `application.yml`의 `spring.application.name`

## 기술 스택

| 영역 | 선택 | 메모 |
|---|---|---|
| 언어·프레임워크 | Java 21, Spring Boot 4.1 | 3.5는 2026-06 무료 지원 종료라 4.x로 시작 |
| DB | PostgreSQL 16 + PostGIS | 반경 검색, 일 단위 파티션 |
| 스키마 관리 | Flyway | `src/main/resources/db/migration` |
| 스케줄러 | Quartz (JDBC, 클러스터 모드) | Blue/Green 중 중복 실행 방지 |
| 지표 | Actuator + Micrometer Prometheus | `/actuator/prometheus` |
| 대량 적재 | Spring Batch | 다음 단계에서 추가 |

## 로컬 세팅 (처음 한 번)

### 1. 설치할 것

| 도구 | 용도 | 메모 |
|---|---|---|
| **JDK 21** | 빌드·실행 | IntelliJ에서 받는 게 제일 쉬움 (아래 3번) |
| **IntelliJ IDEA** | 개발 | 쓰던 것 그대로 |
| **Docker Desktop** | 로컬 PostgreSQL 실행 | 설치 후 실행해 두기 (고래 아이콘) |
| **DB 클라이언트** | 테이블·데이터 보기 | IntelliJ에 Database 탭이 있으면 그걸로. 없으면 **DBeaver Community**(무료). PostGIS 좌표를 지도로 보여줘서 이 프로젝트에 잘 맞음 |
| Git | 버전 관리 | |

### 2. DB 띄우기

프로젝트 폴더에서:

```bash
docker compose up -d
```

`docker ps`에 `medinfo-postgres`가 `healthy`로 보이면 OK.
테이블은 앱을 처음 실행할 때 Flyway가 자동으로 만든다.

### 3. IntelliJ로 열기

1. **File > Open** → 이 폴더의 `build.gradle` 선택 → **Open as Project**
2. JDK가 없다고 나오면: **File > Project Structure > Project > SDK > Download JDK** → Version **21**, Vendor **Eclipse Temurin**
3. **Settings > Build, Execution, Deployment > Build Tools > Gradle** → Gradle JVM을 21로
4. 오른쪽 Gradle 탭에서 새로고침(🔄) → 의존성 다운로드 끝날 때까지 대기

### 4. 인증키 넣기

```bash
cp .env.example .env
```

`.env`를 열어 `PUBLIC_DATA_SERVICE_KEY=` 뒤에 공공데이터포털 일반 인증키를 붙인다.
`.env`는 `.gitignore`에 있어서 GitHub에 올라가지 않는다. **키를 코드나 문서에 직접 쓰지 말 것.**

### 5. 실행

`src/main/java/com/medinfo/MedinfoApplication.java` 열고 `main` 옆 ▶ 클릭.
(프로필을 안 정해도 기본이 `local`)

로그에 아래가 보이면 성공:

```
Successfully applied 2 migrations ...      ← Flyway가 테이블 생성
Scheduler medinfo-scheduler ... started    ← Quartz 시작
응급실 수집 완료: 저장 415곳 ...            ← 짝수 분(:00, :02, ...)마다
```

### 6. 확인

- 브라우저: http://localhost:8080/actuator/health → `{"status":"UP"}`
- DB 클라이언트 접속: host `localhost`, port `5432`, DB `medinfo`, user `medinfo`, password `medinfo-local`

```sql
-- 응급실별 최신 병상
SELECT h.name, l.hvec AS 가용, l.hvs01 AS 기준, l.hvidate AS 병원입력시각, l.collected_at AS 수집시각
FROM er_bed_latest l JOIN er_hospital h USING (hpid)
ORDER BY l.hvec DESC NULLS LAST;

-- 오늘 API 호출량
SELECT * FROM api_usage_daily ORDER BY use_date DESC;

-- 파티션 목록
SELECT tablename FROM pg_tables WHERE tablename LIKE 'er_bed_snapshot_%' ORDER BY 1;

-- 병원정보 (첫 기동 후 수 분 뒤 약 8만 곳)
SELECT cl_cd_nm, count(*) FROM hospital GROUP BY 1 ORDER BY 2 DESC;

-- 응급기관 ↔ 심평원 병원 매칭 결과
SELECT match_method, count(*) FROM er_hospital GROUP BY 1;

-- 매칭 안 된 응급기관 (수동 지정 검토용)
SELECT hpid, name, norm_hosp_name(name) AS 정리된이름, addr
FROM er_hospital WHERE hospital_id IS NULL ORDER BY name;

```

**수동 매칭**은 DB에 직접 UPDATE하지 말고 `src/main/resources/db/migration/R__er_manual_match.sql`에 한 줄 추가한다.
그래야 로컬·운영 DB 모두 같은 결과가 나온다. 파일이 바뀌면 다음 기동 때 Flyway가 다시 실행하고, 매칭기가 목록을 우선 적용한다.

### API를 직접 호출해 보고 싶을 때

브라우저 대신 `http/publicdata.http`를 IntelliJ에서 열고 요청 옆 ▶ 클릭.

1. `http/http-client.private.env.json.example`을 같은 폴더에 `http-client.private.env.json`으로 복사하고 키 입력 (git 제외됨)
2. 실행할 때 환경을 `local`로 선택

브라우저로 열면 주소창이 `.../004046165486/openapi/service/...&SG_APIM=...`로 바뀌는데,
그 주소를 다시 쓰면 에러 12가 난다. 원 주소(`/B551182/...`, `/B552657/...`)만 쓸 것.

## 테스트

```bash
./gradlew test        # Windows: gradlew.bat test
```

`src/test/resources/samples`의 XML은 2026-10-19 실제 응답에서 잘라낸 것.

## 자주 막히는 곳

| 증상 | 원인·해결 |
|---|---|
| `Connection refused` (5432) | Docker Desktop이 꺼져 있거나 `docker compose up -d` 안 함 |
| 5432 포트 사용 중 | PC에 PostgreSQL이 이미 설치돼 있음 → 그걸 끄거나 compose의 포트를 `5433:5432`로, `application-local.yml` URL도 5433으로 |
| SQL에서 `relation ... does not exist` | DB 툴이 `postgres` DB에 붙어 있음. 접속 설정의 Database를 `medinfo`로 (IntelliJ: 콘솔 오른쪽 위 스키마 선택에서 `medinfo.public`) |
| `PUBLIC_DATA_SERVICE_KEY 미설정` | `.env`가 프로젝트 루트에 없음. IntelliJ 실행 시 Working directory가 프로젝트 루트인지 확인 |
| 응급실 수집 `[12]` | 주소 오타. `ApiOperation.java` 경로 확인 |
| 응급실 수집 `[30]` | 키가 틀렸거나 활용신청 직후(1시간 정도 대기) |
| `[22]` | 오늘 한도 초과. 자정(KST)에 초기화 |

## 구조

```
src/main/java/com/medinfo
├── publicdata/   공공데이터 공통: 클라이언트(타임아웃·재시도·리다이렉트), XML 파서, 한도 가드
├── er/           응급실 실시간 병상 수집(2분), 응급기관 목록 수집 + 매칭
├── hospital/     심평원 병원정보 전국 수집
├── partition/    응급실 스냅샷 일 단위 파티션 생성·삭제
└── config/       Quartz 스케줄, Clock(KST)
src/main/resources/db/migration
├── V1__init_schema.sql   서비스 테이블
├── V2__quartz_tables.sql Quartz 테이블
├── V3__hospital_sync.sql 병원 전화·홈페이지 컬럼, 기관명 정규화 함수 norm_hosp_name
├── V4__match_rules_v2.sql 정규화에 "부속" 제거 추가
├── V5__norm_fix_and_manual_match.sql 정규화 버그 수정, 수동 매칭 목록 테이블
└── R__er_manual_match.sql 수동 매칭 목록 (내용 바뀌면 재실행)
```

## 지금 들어간 것 / 다음

- [x] 응급실 실시간 수집 (2분, Quartz 클러스터)
- [x] 공공데이터 공통 클라이언트 + 일일 한도 가드 + 지표
- [x] 스냅샷 파티션 관리 (7일 보관)
- [x] 응급의료기관 목록 수집(매일) + hpid↔ykiho 자동 매칭 3단계 (300m+유사도 0.5 / 2km+이름 일치 / 100m+이름 포함), 기동 때 재매칭
- [x] 병원정보 전국 수집(매주), 첫 기동 때 비어 있으면 즉시 실행
- [ ] 비급여 요약·상세 대량 적재 (Spring Batch)
- [ ] 조회 API, 화면
- [ ] Dockerfile, CI/CD, Terraform 이식 (Flicker)
