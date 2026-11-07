variable "project_name" {
  type = string
}

variable "domain" {
  type = string
}

variable "zone_id" {
  type = string
}

variable "origin_ip" {
  description = "EC2 고정 IP (origin.<도메인> A 레코드)"
  type        = string
}

variable "origin_verify_secret" {
  description = "CloudFront → Caddy 비밀 헤더 값"
  type        = string
  sensitive   = true
}
