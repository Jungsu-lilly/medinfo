output "instance_id" {
  value = aws_instance.this.id
}

output "instance_arn" {
  value = aws_instance.this.arn
}

output "log_group_name" {
  value = aws_cloudwatch_log_group.app.name
}
