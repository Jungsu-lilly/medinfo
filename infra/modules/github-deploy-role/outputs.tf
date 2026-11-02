output "role_arn" {
  description = "GitHub 저장소 변수 AWS_ROLE_ARN에 넣을 값"
  value       = aws_iam_role.this.arn
}
