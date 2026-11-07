output "distribution_id" {
  value = aws_cloudfront_distribution.this.id
}

output "distribution_domain_name" {
  description = "xxxx.cloudfront.net (Route 53 별칭 대상)"
  value       = aws_cloudfront_distribution.this.domain_name
}

output "distribution_zone_id" {
  value = aws_cloudfront_distribution.this.hosted_zone_id
}
