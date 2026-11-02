# 병원값 인프라 (Terraform)

```
인터넷 ─HTTPS─▶ ALB ─▶ EC2 t3.medium ─────────────────────────▶ RDS PostgreSQL 16 (db.t4g.micro)
 병원값.com      │      ├ medinfo-blue : web :3000 ─▶ api        (private 서브넷)
 (ACM 인증서)    │      └ medinfo-green: web :3001 ─▶ api
                 └ HTTPS 리스너가 blue·green 대상 그룹 중 하나를 가리킴 → 배포 때 바꿔 끼움
GitHub Actions ─OIDC─▶ ECR(medinfo-api, medinfo-web) / SSM으로 EC2에 배포 명령 / ALB 전환
```

| 파일 | 내용 |
|---|---|
| `modules/network` | VPC, public·private 서브넷 2개씩, 보안 그룹 (ALB → EC2 → RDS) |
| `modules/certificate` | ACM 인증서 (병원값.com + www), Route 53 DNS 검증 |
| `modules/alb` | ALB, 대상 그룹 blue·green, HTTP→HTTPS, www→대표 도메인 |
| `modules/ecr` | 이미지 저장소 2개, 20개만 남김 |
| `modules/rds` | PostgreSQL 16, 백업 7일, 삭제 보호 |
| `modules/app-server` | EC2(Docker), IAM 역할, CloudWatch 로그 그룹 |
| `modules/github-deploy-role` | GitHub Actions용 역할 (main 브랜치만, 배포에 필요한 권한만) |
| `environments/prod` | 위 모듈 조립 + DNS 레코드 + DB 비밀번호(SSM) |
| `../deploy/` | EC2에서 쓰는 compose·배포 스크립트 (배포 때마다 내려받음) |
| `../.github/workflows/` | `ci.yml`(PR 테스트), `deploy.yml`(main 머지 → 배포) |

## Flicker에서 바꾼 것

| | Flicker | 병원값 | 이유 |
|---|---|---|---|
| NAT 게이트웨이 | 있음 | **없음** | 월 약 45달러. EC2는 public 서브넷 + 공인 IP로 나가고, 들어오는 건 보안 그룹이 ALB만 허용 |
| EC2 | 콘솔에서 생성 | **Terraform** | 서버도 코드로 다시 만들 수 있게 |
| DB | MySQL | **PostgreSQL 16** | PostGIS(거리 검색), pg_trgm(이름 유사도) |
| 삭제 보호·마지막 스냅샷 | 꺼짐(학습용) | **켬** | 실서비스 |
| ElastiCache·S3 | 있음 | 없음 | 아직 안 씀 (캐시는 장애 시나리오 5에서) |
| 배포 대상 | 앱 1개 | **web + api 한 쌍** | 한 색 = 한 쌍. ALB는 web만 보고, `/api/*`도 web이 같은 색 api로 넘김 |
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
3. 만들기 (인증서 검증·RDS 때문에 15~20분)
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
| 되돌리기 | Actions → deploy → Run workflow → `tag`에 예전 커밋 앞 7자리 |
| 로그 보기 | CloudWatch → 로그 그룹 `/medinfo/prod` (스트림 = 컨테이너 이름) |
| 서버 접속 | `aws ssm start-session --target $(terraform output -raw app_instance_id)` (Session Manager 플러그인 필요) |
| 지금 어느 색인지 | EC2 → 대상 그룹 → `medinfo-blue`/`medinfo-green` 중 healthy 대상이 있는 쪽 |
| DB 접속 (내 PC에서) | SSM 포트 포워딩: `aws ssm start-session --target <EC2> --document-name AWS-StartPortForwardingSessionToRemoteHost --parameters host=<db_address>,portNumber=5432,localPortNumber=15432` → `localhost:15432`. 비밀번호는 SSM `/medinfo/prod/DB_PASSWORD` |

## 비용 (서울 리전, 월 대략)

| | 달러 |
|---|---|
| EC2 t3.medium | 38 |
| RDS db.t4g.micro + 20GB | 21 |
| ALB | 20 |
| 공인 IPv4 3개 (ALB 2 + EC2 1) | 11 |
| Route 53, ECR, CloudWatch Logs | 3 |
| **합계** | **약 93달러 (약 13만 원)** — 예산 월 20만 원 안 |

## Flicker 내리기 (병원값 배포 확인 뒤)

1. `flicker-infra/environments/prod`에서 `terraform destroy` (VPC·NAT·ALB·RDS·ElastiCache·ECR·S3)
   - Flicker RDS는 `skip_final_snapshot = true`라 **데이터가 스냅샷 없이 지워짐**. 남길 게 있으면 먼저 콘솔에서 스냅샷
   - S3 이미지 버킷이 비어 있지 않으면 삭제가 실패함 → 버킷을 비우고 다시 실행
2. 콘솔에서 만든 Flicker EC2 종료
3. **GitHub OIDC 공급자는 지우지 않는다** (병원값이 같이 씀). Flicker용 GitHub 역할은 지워도 됨
4. 상태 버킷 `flicker-tfstate-…`와 잠금 테이블도 지우지 않는다 (병원값 상태가 같이 들어 있음)
