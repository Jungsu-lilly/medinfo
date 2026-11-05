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
  description = "web·api 한 쌍 약 1.5GB, 전환 중엔 두 쌍 → 4GB. architecture와 맞아야 함 (t4g = arm64)"
  type        = string
  default     = "t4g.medium"
}

variable "architecture" {
  description = "AMI 아키텍처: arm64(t4g·Graviton) 또는 x86_64(t3). 이미지 빌드(deploy.yml platforms)와 같아야 함"
  type        = string
  default     = "arm64"

  validation {
    condition     = contains(["arm64", "x86_64"], var.architecture)
    error_message = "arm64 또는 x86_64"
  }
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
