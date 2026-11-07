# 병원값 운영 환경
#   인터넷 ─HTTPS─▶ EC2 고정 IP ─▶ Caddy ─▶ web blue|green ─▶ api 같은 색 ─▶ RDS PostgreSQL
#   (2026-11 ALB를 빼고 Caddy로: 서버 1대라 ALB가 고가용성을 못 주는데 비용의 약 35% → infra/README.md)
# 비밀값 중 공공데이터 인증키는 Terraform 밖에서 SSM에 직접 넣는다 (상태 파일에 남지 않게) → infra/README.md

locals {
  name        = var.project_name
  db_name     = "medinfo"
  db_username = "medinfo"
}

data "aws_route53_zone" "this" {
  name         = var.domain
  private_zone = false
}

module "network" {
  source = "../../modules/network"

  project_name         = local.name
  vpc_cidr             = "10.1.0.0/16" # Flicker(10.0.0.0/16)와 안 겹치게
  azs                  = ["ap-northeast-2a", "ap-northeast-2c"]
  public_subnet_cidrs  = ["10.1.1.0/24", "10.1.2.0/24"]
  private_subnet_cidrs = ["10.1.11.0/24", "10.1.12.0/24"]
}

# 병원값.com, www.병원값.com → EC2 고정 IP (HTTPS 인증서는 서버 안 Caddy가 Let's Encrypt로 발급)
# CloudFront로 옮기기 직전이라 TTL을 60초로 낮춤 (전환 때 옛 주소가 빨리 사라지게)
resource "aws_route53_record" "site" {
  for_each = toset([var.domain, "www.${var.domain}"])

  zone_id = data.aws_route53_zone.this.zone_id
  name    = each.value
  type    = "A"
  ttl     = 60
  records = [module.app_server.public_ip]
}

module "ecr" {
  source = "../../modules/ecr"
  names  = ["${local.name}-api", "${local.name}-web"]
}

# DB 비밀번호는 Terraform이 만들어 SSM에 넣는다 (상태 파일에도 남음 → 상태 버킷은 암호화·비공개)
# 특수문자는 .env·JDBC에서 문제없는 것만
resource "random_password" "db" {
  length           = 32
  special          = true
  override_special = "-_=+"
}

module "rds" {
  source = "../../modules/rds"

  project_name      = local.name
  subnet_ids        = module.network.private_subnet_ids
  security_group_id = module.network.db_security_group_id
  instance_class    = var.db_instance_class
  db_name           = local.db_name
  db_username       = local.db_username
  db_password       = random_password.db.result
}

# 앱 환경변수 (배포 스크립트가 /medinfo/prod/* 를 읽어 .env로 만든다. 이름 = 환경변수 이름)
resource "aws_ssm_parameter" "app" {
  for_each = {
    DB_URL      = "jdbc:postgresql://${module.rds.address}:${module.rds.port}/${local.db_name}"
    DB_USERNAME = local.db_username
  }

  name  = "/${local.name}/prod/${each.key}"
  type  = "String"
  value = each.value
}

resource "aws_ssm_parameter" "db_password" {
  name  = "/${local.name}/prod/DB_PASSWORD"
  type  = "SecureString"
  value = random_password.db.result
}

module "app_server" {
  source = "../../modules/app-server"

  project_name      = local.name
  subnet_id         = module.network.public_subnet_ids[0]
  security_group_id = module.network.app_security_group_id
  instance_type     = var.app_instance_type
}

module "github_deploy_role" {
  source = "../../modules/github-deploy-role"

  project_name         = local.name
  github_repo          = var.github_repo
  github_owner_id      = var.github_owner_id
  github_repo_id       = var.github_repo_id
  create_oidc_provider = var.create_github_oidc_provider
  ecr_repository_arns  = module.ecr.repository_arns
  instance_arn         = module.app_server.instance_arn
}

# 비용 안전장치: 트래픽 급증 시 경고 → 차단기(서버 정지), 월 예산 알림
module "cost_guard" {
  source = "../../modules/cost-guard"

  project_name   = local.name
  alert_email    = var.alert_email
  instance_id    = module.app_server.instance_id
  log_group_name = module.app_server.log_group_name
}

# CloudFront → Caddy 비밀 헤더. SSM에 넣어 두면 배포 스크립트가 .env로 받아 Caddy에 넘긴다
resource "random_password" "origin_verify" {
  length  = 40
  special = false
}

resource "aws_ssm_parameter" "origin_verify" {
  name  = "/${local.name}/prod/ORIGIN_VERIFY"
  type  = "SecureString"
  value = random_password.origin_verify.result
}

# CloudFront (정액 요금제 Free 플랜은 콘솔에서 가입 → infra/README.md)
module "cdn" {
  source = "../../modules/cdn"
  providers = {
    aws           = aws
    aws.us_east_1 = aws.us_east_1
  }

  project_name         = local.name
  domain               = var.domain
  zone_id              = data.aws_route53_zone.this.zone_id
  origin_ip            = module.app_server.public_ip
  origin_verify_secret = random_password.origin_verify.result
}
