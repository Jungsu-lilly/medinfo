# 병원값 운영 환경
#   인터넷 ─HTTPS─▶ ALB ─▶ EC2(web blue|green ─▶ api 같은 색) ─▶ RDS PostgreSQL
# 비밀값 중 공공데이터 인증키는 Terraform 밖에서 SSM에 직접 넣는다 (상태 파일에 남지 않게) → infra/README.md

locals {
  name        = var.project_name
  db_name     = "medinfo"
  db_username = "medinfo"
  # 색별 web 포트 (EC2 호스트). 배포 스크립트(deploy/deploy.sh)와 같아야 함
  color_ports = { blue = 3000, green = 3001 }
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
  app_port_from        = local.color_ports.blue
  app_port_to          = local.color_ports.green
}

module "certificate" {
  source = "../../modules/certificate"

  domain  = var.domain
  zone_id = data.aws_route53_zone.this.zone_id
}

module "alb" {
  source = "../../modules/alb"

  project_name      = local.name
  vpc_id            = module.network.vpc_id
  public_subnet_ids = module.network.public_subnet_ids
  security_group_id = module.network.alb_security_group_id
  certificate_arn   = module.certificate.certificate_arn
  domain            = var.domain
  color_ports       = local.color_ports
}

# 병원값.com, www.병원값.com → ALB
resource "aws_route53_record" "site" {
  for_each = toset([var.domain, "www.${var.domain}"])

  zone_id = data.aws_route53_zone.this.zone_id
  name    = each.value
  type    = "A"

  alias {
    name                   = module.alb.alb_dns_name
    zone_id                = module.alb.alb_zone_id
    evaluate_target_health = true
  }
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
  https_listener_arn   = module.alb.https_listener_arn
  target_group_arns    = values(module.alb.target_group_arns)
  instance_arn         = module.app_server.instance_arn
}
