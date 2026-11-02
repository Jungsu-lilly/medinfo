output "address" {
  description = "접속 주소 (포트 제외)"
  value       = aws_db_instance.this.address
}

output "port" {
  value = aws_db_instance.this.port
}
