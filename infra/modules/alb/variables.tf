variable "project_name" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "security_group_id" {
  type = string
}

variable "certificate_arn" {
  type = string
}

variable "domain" {
  description = "대표 도메인 (퓨니코드)"
  type        = string
}

variable "color_ports" {
  description = "색별 web 포트 (EC2 호스트 포트)"
  type        = map(number)
  default     = { blue = 3000, green = 3001 }
}

variable "health_check_path" {
  type    = string
  default = "/healthz"
}
