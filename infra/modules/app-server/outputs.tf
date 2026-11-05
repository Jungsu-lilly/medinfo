output "instance_id" {
  value = aws_instance.this.id
}

output "instance_arn" {
  value = aws_instance.this.arn
}

output "public_ip" {
  description = "고정 공인 IP (도메인 A 레코드)"
  value       = aws_eip.this.public_ip
}

output "log_group_name" {
  value = aws_cloudwatch_log_group.app.name
}
