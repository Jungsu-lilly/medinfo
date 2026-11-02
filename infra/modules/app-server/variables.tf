variable "project_name" {
  type = string
}

variable "subnet_id" {
  type = string
}

variable "security_group_id" {
  type = string
}

variable "instance_type" {
  description = "web·api 한 쌍 약 1.5GB, 전환 중엔 두 쌍 → 4GB"
  type        = string
  default     = "t3.medium"
}

variable "root_volume_gb" {
  type    = number
  default = 30
}

variable "compose_version" {
  description = "docker compose 플러그인 버전 (GitHub 릴리스 태그)"
  type        = string
  default     = "v2.29.7"
}

variable "log_retention_days" {
  type    = number
  default = 30
}
