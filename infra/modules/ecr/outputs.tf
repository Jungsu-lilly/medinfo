output "repository_arns" {
  value = [for r in aws_ecr_repository.this : r.arn]
}

output "registry" {
  description = "<계정>.dkr.ecr.<리전>.amazonaws.com"
  value       = split("/", values(aws_ecr_repository.this)[0].repository_url)[0]
}
