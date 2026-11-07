output "github_deploy_role_arn" {
  description = "GitHub 저장소 Settings → Variables → AWS_ROLE_ARN"
  value       = module.github_deploy_role.role_arn
}

output "site_url" {
  value = "https://${var.domain}"
}

output "app_public_ip" {
  description = "고정 공인 IP (병원값.com A 레코드)"
  value       = module.app_server.public_ip
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

output "alerts_topic_arn" {
  description = "비용·트래픽 알림 SNS 주제 (나중에 다른 알람도 여기로)"
  value       = module.cost_guard.alerts_topic_arn
}

output "log_group" {
  value = module.app_server.log_group_name
}
