variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "project_name" {
  description = "리소스 이름 앞에 붙는 값. 배포 워크플로가 이 이름으로 리소스를 찾으니 바꾸면 워크플로도 같이"
  type        = string
  default     = "medinfo"
}

variable "domain" {
  description = "병원값.com의 퓨니코드. Route 53에 같은 이름의 호스팅 영역이 있어야 함 (도메인을 Route 53에서 사면 자동 생성)"
  type        = string
  default     = "xn--639ao13amqh.com"
}

variable "github_repo" {
  type    = string
  default = "Jungsu-lilly/medinfo"
}

# GitHub OIDC 토큰 sub에 들어가는 바뀌지 않는 ID (저장소를 다시 만들면 바뀜)
variable "github_owner_id" {
  type    = string
  default = "56336436"
}

variable "github_repo_id" {
  type    = string
  default = "1428310945"
}

variable "create_github_oidc_provider" {
  description = "계정에 GitHub OIDC 공급자가 없을 때만 true"
  type        = bool
  default     = false
}

variable "app_instance_type" {
  description = "ARM(t4g). x86(t3)로 바꾸면 app_server의 architecture와 deploy.yml 빌드 플랫폼도 같이"
  type        = string
  default     = "t4g.medium"
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.micro"
}
