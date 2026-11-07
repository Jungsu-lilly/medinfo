variable "project_name" {
  type = string
}

variable "alert_email" {
  description = "알림 받을 메일 (terraform.tfvars에만, 저장소에 올리지 않음)"
  type        = string
}

variable "instance_id" {
  description = "감시하고 멈출 앱 서버"
  type        = string
}

variable "log_group_name" {
  type = string
}

variable "monthly_budget_usd" {
  description = "월 예산 (달러, 부가세 제외). 65 ≈ 부가세 포함 약 10만 원 (1,450원 기준)"
  type        = number
  default     = 65
}

variable "egress_warn_bytes_per_5min" {
  type    = number
  default = 300000000 # 300MB
}

variable "egress_stop_bytes_per_5min" {
  type    = number
  default = 1000000000 # 1GB
}

variable "logs_warn_bytes_per_hour" {
  type    = number
  default = 200000000 # 200MB
}
