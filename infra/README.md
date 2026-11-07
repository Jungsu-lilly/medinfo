# 병원값 인프라 (Terraform)

```
인터넷 ─HTTPS─▶ CloudFront (정액 Free 플랜: 초과 요금 없음, WAF·요청 제한, ACM 인증서, /_next/static 캐시)
 병원값.com        │ HTTP + 비밀 헤더(X-Origin-Verify)
                   ▼
                 EC2 t4g.medium (ARM, 고정 IP = origin.도메인) ───────────▶ RDS PostgreSQL 16 (db.t4g.micro)
                 ├ Caddy :80 (비밀 헤더 없으면 403)                           (private 서브넷)
                 │   └ active.caddy가 가리키는 색으로 → 배포 때 바꾸고 reload
                 ├ medinfo-blue : web 127.0.0.1:3000 ─▶ api
                 └ medinfo-green: web 127.0.0.1:3001 ─▶ api
GitHub Actions ─OIDC─▶ ECR(medinfo-api, medinfo-web, arm64) / SSM으로 EC2에 배포·전환 명령
```

| 파일 | 내용 |
|---|---|
| `modules/network` | VPC, public·private 서브넷 2개씩, 보안 그룹 (인터넷 80·443 → EC2 → RDS) |
| `modules/ecr` | 이미지 저장소 2개, 20개만 남김 |
| `modules/rds` | PostgreSQL 16, 백업 7일, 삭제 보호 |
| `modules/app-server` | EC2(Docker, ARM), 고정 IP, IAM 역할, CloudWatch 로그 그룹 |
| `modules/github-deploy-role` | GitHub Actions용 역할 (main 브랜치만, 배포에 필요한 권한만) |
| `environments/prod` | 위 모듈 조립 + DNS 레코드(→ 고정 IP) + DB 비밀번호(SSM) |
| `../deploy/` | EC2에서 쓰는 compose·배포 스크립트·Caddyfile (배포 때마다 내려받음) |
| `../.github/workflows/` | `ci.yml`(PR 테스트), `deploy.yml`(main 머지 → 배포) |

## Flicker에서 바꾼 것

| | Flicker | 병원값 | 이유 |
|---|---|---|---|
| NAT 게이트웨이 | 있음 | **없음** | 월 약 45달러. EC2는 public 서브넷 + 공인 IP로 나가고, 들어오는 건 보안 그룹이 80·443만 허용 |
| EC2 | 콘솔에서 생성 | **Terraform** | 서버도 코드로 다시 만들 수 있게 |
| DB | MySQL | **PostgreSQL 16** | PostGIS(거리 검색), pg_trgm(이름 유사도) |
| 삭제 보호·마지막 스냅샷 | 꺼짐(학습용) | **켬** | 실서비스 |
| ElastiCache·S3 | 있음 | 없음 | 아직 안 씀 (캐시는 장애 시나리오 5에서) |
| 배포 대상 | 앱 1개 | **web + api 한 쌍** | 한 색 = 한 쌍. 앞단은 web만 보고, `/api/*`도 web이 같은 색 api로 넘김 |
| 앞단 | ALB | **EC2 안 Caddy** (2026-11) | 서버 1대라 ALB가 고가용성을 못 줌. HTTPS·전환만 하는데 비용의 약 35% → 아래 "ALB → Caddy" |
| GitHub 시크릿 | ARN 6개 | **변수 1개** (`AWS_ROLE_ARN`) | 워크플로가 이름으로 리소스를 찾음 |
| 비밀값 | 서버의 .env | **SSM Parameter Store** | 배포 때마다 SSM → .env |

## 처음 만들기

준비물: Flicker 때 쓰던 AWS CLI 자격 증명, Terraform 1.6 이상

1. Route 53에 도메인 호스팅 영역이 있는지 확인 (도메인을 Route 53에서 샀으면 자동으로 있음)
   ```
   aws route53 list-hosted-zones-by-name --dns-name xn--639ao13amqh.com --max-items 1
   ```
2. GitHub OIDC 공급자가 있는지 확인 (Flicker 때 만들었으면 있음)
   ```
   aws iam list-open-id-connect-providers
   ```
   `token.actions.githubusercontent.com`이 없으면 `terraform.tfvars.example`을 `terraform.tfvars`로 복사하고 `create_github_oidc_provider = true`
3. 만들기 (RDS 때문에 10~15분)
   ```
   cd infra/environments/prod
   terraform init
   terraform plan      # 추가만 있고 변경·삭제가 없는지 확인
   terraform apply
   ```
4. 공공데이터 인증키를 SSM에 넣기 (Terraform 상태 파일에 남지 않게 직접)
   ```
   aws ssm put-parameter --name /medinfo/prod/PUBLIC_DATA_SERVICE_KEY --type SecureString --value "인증키"
   ```
5. **로컬 `.env`에 `COLLECTORS_ENABLED=false` 추가** — 인증키가 같아서 로컬과 운영이 같이 수집하면 일일 한도를 넘음
6. GitHub 저장소 → Settings → Secrets and variables → Actions → **Variables** 탭 → New repository variable
   - `AWS_ROLE_ARN` = `terraform output -raw github_deploy_role_arn`
   - `SITE_INDEXING` = `false` (오픈할 때 `true`로 바꾸고 다시 배포)
7. 첫 배포: Actions → deploy → Run workflow (main). 테스트 → 이미지 빌드 → 배포까지 약 10분
8. 확인
   - https://병원값.com
   - 수집 진행: 병원 정보 약 5분, 비급여 요약 20~30분, 상세는 하루~이틀 (일일 한도)

## 평소 쓰는 것

| 하고 싶은 것 | 방법 |
|---|---|
| 배포 | PR 머지 → 자동 |
| 되돌리기 | Actions → deploy → Run workflow → `tag`에 예전 이미지 태그 (ARM 전환 이후 태그만) |
| 로그 보기 | CloudWatch → 로그 그룹 `/medinfo/prod` (스트림 = 컨테이너 이름) |
| 서버 접속 | `aws ssm start-session --target $(terraform output -raw app_instance_id)` (Session Manager 플러그인 필요) |
| 지금 어느 색인지 | 서버의 `/opt/medinfo/active-color` (배포 로그 "지금: blue → …"에도 나옴) |
| Caddy 접속 기록 | CloudWatch → `/medinfo/prod` → 스트림 `medinfo-edge-caddy-1` (JSON 한 줄씩) |
| DB 접속 (내 PC에서) | SSM 포트 포워딩: `aws ssm start-session --target <EC2> --document-name AWS-StartPortForwardingSessionToRemoteHost --parameters host=<db_address>,portNumber=5432,localPortNumber=15432` → `localhost:15432`. 비밀번호는 SSM `/medinfo/prod/DB_PASSWORD` |

## 비용 (서울 리전, 월 대략)

| | ALB 시절 (~2026-11) | 지금 (Caddy) |
|---|---|---|
| EC2 | t3.medium 38 | **t4g.medium 30** |
| EBS 30GB gp3 | 3 | 3 |
| RDS db.t4g.micro + 20GB | 21 | 21 |
| ALB | 18 | **0** |
| 공인 IPv4 | 3개 11 | **1개 4** |
| Route 53, ECR, CloudWatch Logs | 2 | 2 |
| 합계 (달러) | 93 | **59** |
| **부가세 10% 포함 (1,450원)** | **약 14.8만 원** | **약 9.4만 원** — 예산 월 10만 원 안 |

더 줄일 때: 3~6개월 써 보고 1년 약정(Compute Savings Plan, RDS 예약 인스턴스) → 약 7.5만 원.
RDS는 PostgreSQL 최소 크기(db.t4g.micro·20GB). EC2는 배포 중 두 색이 같이 떠서 4GB 필요 → t4g.small(2GB) 불가

## 비용 안전장치 (`modules/cost-guard`)

AWS에는 "얼마가 되면 결제를 멈추는" 한도가 없다. 고정비(EC2·RDS·IP)는 트래픽과 상관없지만,
**밖으로 나가는 데이터(GB당 약 0.13달러)와 로그 수집량은 트래픽만큼 늘어난다** → 봇이 퍼부으면 하루 수십~수백 달러 가능.
(EC2 t4g.medium 지속 대역폭 약 0.26Gbps ≈ 하루 2.7TB ≈ 350달러가 이론상 최대)

| 장치 | 기준 | 동작 | 반응 |
|---|---|---|---|
| 트래픽 경고 | EC2 NetworkOut 5분에 300MB 초과 | 메일 | 5분 |
| **차단기** | 5분에 1GB 초과가 10분 계속 | **EC2 자동 정지** + 메일 | 10~15분 → 최악 약 3달러에서 멈춤 |
| 로그 경고 | `/medinfo/prod` 수집량 1시간 200MB 초과 | 메일 | 1시간 |
| 월 예산 | 65달러(부가세 포함 약 10만 원): 실제 80%, 예상 100% | 메일 | 청구 데이터 기준 몇 시간 |

- 알림 메일: `terraform.tfvars`의 `alert_email` (저장소에 안 올림). 처음 apply 뒤 AWS에서 온 메일의 **Confirm subscription**을 눌러야 받음
- Caddy 접속 기록에서 정적 파일(JS·CSS·폰트·아이콘) 요청은 빼서 로그 양을 줄임 (`deploy/Caddyfile`의 `log_skip`)

**차단기가 작동했을 때**
1. CloudWatch → 로그 그룹 `/medinfo/prod` → `medinfo-edge-caddy-1`에서 어떤 IP·주소로 몰렸는지 확인
2. 공격이면: 막을 방법을 먼저 적용 (CloudFront·WAF 등) / 실제 사용자 급증이면: 기준값(`egress_stop_bytes_per_5min`)을 올림
3. EC2 콘솔에서 `medinfo-app` **시작** → 고정 IP 그대로, 컨테이너(Caddy·blue|green)는 `restart: unless-stopped`라 자동으로 다시 뜸

## CloudFront 전환 (2026-11, 3단계 · 접속 끊김 없이)

왜: 비용 안전장치(차단기)는 응급 브레이크. 근본적으로는 CloudFront **정액 요금제**를 앞에 둬서
트래픽·공격이 몰려도 **초과 요금 자체가 없게** 한다 (Free 플랜 0원: 월 100만 요청·100GB, 넘으면 느려질 뿐 과금 없음.
WAF·IP별 요청 제한·DDoS 방어 포함, 막힌 공격은 사용량에도 안 셈. EC2 → CloudFront 전송은 무료)

```
사용자 ─HTTPS─▶ CloudFront (ACM 인증서, WAF) ─HTTP + 비밀 헤더─▶ origin.도메인 = EC2 Caddy :80 ─▶ web blue|green
```

**1단계: CloudFront 만들기 (DNS는 그대로)** — `modules/cdn`
1. `terraform init` (새 모듈) → `terraform plan` → 추가 8 (인증서·검증 레코드·origin 레코드·배포·비밀값), 변경 2 (사이트 레코드 TTL 300→60), 삭제 0 → `apply` (CloudFront 생성 5~10분)
2. PR 머지 → 배포: Caddy가 CloudFront 요청(비밀 헤더)도 받도록. Caddy 컨테이너를 한 번 다시 만들어서 1~2초 끊김
3. 콘솔 CloudFront → 배포 → **요금제(Pricing plan) → Free** 가입 (WAF가 자동으로 붙음) → 보안 설정에서 **IP별 요청 제한(rate limiting)** 켜기
4. 확인 (DNS 바꾸기 전, CloudFront를 직접 거쳐 봄. PowerShell):
   ```
   curl.exe -sI https://xn--639ao13amqh.com --connect-to "xn--639ao13amqh.com:443:$(terraform output -raw cloudfront_domain_name):443"
   ```
   `HTTP/1.1 200`(또는 HTTP/2 200)과 `x-cache: ... cloudfront`가 나오면 정상

**2단계: DNS를 CloudFront로** — 사이트 레코드 A·AAAA → CloudFront 별칭, Caddy는 CloudFront 요청만 받는 설정으로 (apply 후 2분 기다렸다 머지)

**3단계: 문 잠그기** — EC2 보안 그룹을 CloudFront(관리형 접두사 목록)에서 오는 80번만 허용, 443 닫기

## ALB → Caddy 전환 (2026-11, 한 번만)

바뀌는 것: ALB·대상 그룹·ACM 인증서 삭제 / EC2를 ARM(t4g.medium)으로 다시 만듦 / 고정 IP 추가 / DNS를 고정 IP로.
DB(RDS)와 데이터는 그대로. **전환 중 10~15분 접속 안 됨** (서버 교체 + 첫 배포) → 사용자가 적은 시간에.

1. 이 PR 브랜치를 받아서 계획 확인 (아직 main에 머지하지 않은 상태)
   ```
   git fetch && git checkout feat/caddy-edge
   cd infra/environments/prod
   terraform init
   terraform plan -replace=module.app_server.aws_instance.this
   ```
   - 확인할 것: `module.alb`·`module.certificate` 삭제, `aws_instance.this` **replace**, `aws_eip`·`aws_eip_association` 추가, Route 53 레코드 2개 변경
   - **`module.rds`가 삭제·교체 목록에 있으면 멈출 것** (변경 없음이어야 함)
   - `-replace`가 필요한 이유: AMI는 `ignore_changes`라 x86 → ARM이 자동으로 교체되지 않음
2. 적용 (`yes` 입력 직후 PR 머지 → 이미지 빌드가 서버 교체와 동시에 진행)
   ```
   terraform apply -replace=module.app_server.aws_instance.this
   ```
3. 머지 배포가 새 서버에 첫 배포 (서버 준비 → Caddy 기동 → Let's Encrypt 인증서 발급 → blue)
4. 확인: https://병원값.com, `www.` → 대표 주소로 이동, http → https
5. 이전 이미지(x86) 태그로는 되돌리기 불가 → 문제가 생기면 새 커밋으로 고쳐서 배포

## Flicker 내리기 (2026-11-05 완료)

남겨 둔 것: 상태 버킷·잠금 테이블, GitHub OIDC 공급자, 기본 VPC(무료). `simple-sns.link`는 자동 갱신 끔 → 2027-05 만료


1. `flicker-infra/environments/prod`에서 `terraform destroy` (VPC·NAT·ALB·RDS·ElastiCache·ECR·S3)
   - Flicker RDS는 `skip_final_snapshot = true`라 **데이터가 스냅샷 없이 지워짐**. 남길 게 있으면 먼저 콘솔에서 스냅샷
   - S3 이미지 버킷이 비어 있지 않으면 삭제가 실패함 → 버킷을 비우고 다시 실행
2. 콘솔에서 만든 Flicker EC2 종료
3. **GitHub OIDC 공급자는 지우지 않는다** (병원값이 같이 씀). Flicker용 GitHub 역할은 지워도 됨
4. 상태 버킷 `flicker-tfstate-…`와 잠금 테이블도 지우지 않는다 (병원값 상태가 같이 들어 있음)
