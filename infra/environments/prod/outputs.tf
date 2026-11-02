output "github_deploy_role_arn" {
  description = "GitHub 저장소 Settings → Variables → AWS_ROLE_ARN"
  value       = module.github_deploy_role.role_arn
}

output "site_url" {
  value = "https://${var.domain}"
}

output "alb_dns_name" {
  value = module.alb.alb_dns_name
}

output "app_instance_id" {
  description = "접속: aws ssm start-session --target <이 값>"
  value       = module.app_server.instance_id
}

output "ecr_registry" {
  value = module.ecr.registry
}

output "db_address" {
  value = module.rds.address
}

output "log_group" {
  value = module.app_server.log_group_name
}
