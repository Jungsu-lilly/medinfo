variable "project_name" {
  type = string
}

variable "github_repo" {
  description = "owner/repo"
  type        = string
}

variable "github_owner_id" {
  description = "저장소 소유자 숫자 ID (gh api repos/<owner>/<repo> --jq .owner.id)"
  type        = string
}

variable "github_repo_id" {
  description = "저장소 숫자 ID (gh api repos/<owner>/<repo> --jq .id)"
  type        = string
}

variable "branch" {
  type    = string
  default = "main"
}

variable "create_oidc_provider" {
  description = "계정에 GitHub OIDC 공급자가 없을 때만 true (Flicker 때 만든 게 있으면 false)"
  type        = bool
  default     = false
}

variable "ecr_repository_arns" {
  type = list(string)
}

variable "https_listener_arn" {
  type = string
}

variable "target_group_arns" {
  type = list(string)
}

variable "instance_arn" {
  type = string
}
