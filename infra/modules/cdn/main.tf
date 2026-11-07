# CloudFront 앞단 (정액 요금제 Free 플랜으로 가입 → 트래픽이 몰려도 초과 요금 없음)
#   사용자 ─HTTPS─▶ CloudFront(인증서 ACM, WAF·DDoS 방어) ─HTTP─▶ origin.<도메인> = EC2 고정 IP의 Caddy :80
#   - Caddy는 CloudFront가 붙이는 비밀 헤더(X-Origin-Verify)가 있는 요청만 통과시킴
#   - EC2 → CloudFront 전송은 무료. 밖으로 나가는 데이터 요금은 CloudFront 플랜이 정액으로 덮음
#   - 정액 요금제 가입·WAF 규칙은 콘솔에서 (가입할 때 콘솔이 WAF를 붙임 → web_acl_id는 Terraform이 건드리지 않음)
# CloudFront 인증서는 us-east-1에만 만들 수 있어서 provider 별칭을 받는다

terraform {
  required_providers {
    aws = {
      source                = "hashicorp/aws"
      configuration_aliases = [aws.us_east_1]
    }
  }
}

# ── 인증서 (us-east-1) ───────────────────────────────

resource "aws_acm_certificate" "this" {
  provider = aws.us_east_1

  domain_name               = var.domain
  subject_alternative_names = ["www.${var.domain}"]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "validation" {
  for_each = {
    for o in aws_acm_certificate.this.domain_validation_options : o.domain_name => {
      name   = o.resource_record_name
      type   = o.resource_record_type
      record = o.resource_record_value
    }
  }

  zone_id         = var.zone_id
  name            = each.value.name
  type            = each.value.type
  ttl             = 60
  records         = [each.value.record]
  allow_overwrite = true
}

resource "aws_acm_certificate_validation" "this" {
  provider = aws.us_east_1

  certificate_arn         = aws_acm_certificate.this.arn
  validation_record_fqdns = [for r in aws_route53_record.validation : r.fqdn]
}

# ── origin 주소 (CloudFront가 찾아갈 EC2) ─────────────────

resource "aws_route53_record" "origin" {
  zone_id = var.zone_id
  name    = "origin.${var.domain}"
  type    = "A"
  ttl     = 300
  records = [var.origin_ip]
}

# ── 배포 (distribution) ──────────────────────────────

# AWS 관리형 정책 (정액 요금제는 사용자 정의 캐시·origin 요청 정책을 Business 플랜부터 허용)
data "aws_cloudfront_cache_policy" "disabled" {
  name = "Managed-CachingDisabled"
}

data "aws_cloudfront_cache_policy" "optimized" {
  name = "Managed-CachingOptimized"
}

data "aws_cloudfront_origin_request_policy" "all_viewer" {
  name = "Managed-AllViewer"
}

resource "aws_cloudfront_distribution" "this" {
  enabled         = true
  is_ipv6_enabled = true
  http_version    = "http2and3"
  price_class     = "PriceClass_All"
  aliases         = [var.domain, "www.${var.domain}"]
  comment         = "${var.project_name} (flat-rate plan)"

  origin {
    origin_id   = "app"
    domain_name = aws_route53_record.origin.fqdn

    custom_origin_config {
      http_port                = 80
      https_port               = 443
      origin_protocol_policy   = "http-only"
      origin_ssl_protocols     = ["TLSv1.2"]
      origin_read_timeout      = 30
      origin_keepalive_timeout = 5
    }

    # Caddy가 이 값이 있는 요청만 통과 → 다른 경로로 EC2에 직접 오는 요청은 거절
    custom_header {
      name  = "X-Origin-Verify"
      value = var.origin_verify_secret
    }
  }

  # 페이지·API: 캐시하지 않음 (가격·응급실 병상이 계속 바뀜). Host·쿼리·쿠키를 그대로 넘김
  default_cache_behavior {
    target_origin_id         = "app"
    viewer_protocol_policy   = "redirect-to-https"
    allowed_methods          = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods           = ["GET", "HEAD"]
    compress                 = true
    cache_policy_id          = data.aws_cloudfront_cache_policy.disabled.id
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer.id
  }

  # Next.js 빌드 파일(파일 이름에 해시 → 바뀌지 않음): 엣지에서 캐시 → EC2까지 안 옴
  ordered_cache_behavior {
    path_pattern           = "/_next/static/*"
    target_origin_id       = "app"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true
    cache_policy_id        = data.aws_cloudfront_cache_policy.optimized.id
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate_validation.this.certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  # 정액 요금제 가입 때 콘솔이 WAF web ACL을 붙인다 (플랜 필수 조건)
  lifecycle {
    ignore_changes = [web_acl_id]
  }
}
