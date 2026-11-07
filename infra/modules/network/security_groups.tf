# CloudFront → EC2(Caddy :80) → RDS(5432)
# EC2는 CloudFront 관리형 접두사 목록(CloudFront가 origin에 붙을 때 쓰는 IP 대역)에서 오는 80번만 받는다
#   → 서버 IP를 알아내 바로 오는 요청은 연결 자체가 안 됨 (CloudFront 정액 요금제·WAF를 우회 못 함)
#   → 접두사 목록은 규칙 1개가 약 55개로 계산됨 (보안 그룹 인바운드 기본 한도 60) → 이 규칙 하나만
# EC2 안에서 Caddy가 127.0.0.1의 web(blue 3000 | green 3001)으로 넘기므로 web 포트는 밖에 안 엶
# (보안 그룹·규칙 description은 AWS가 ASCII만 받아서 영어로 씀)

data "aws_ec2_managed_prefix_list" "cloudfront" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

resource "aws_security_group" "app" {
  # description은 바꾸면 보안 그룹을 새로 만든다(RDS 규칙까지 영향) → ALB 시절 문구 그대로 둠
  name        = "${var.project_name}-app-sg"
  description = "web blue/green ports from ALB only (no SSH, use SSM)"
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "HTTP from CloudFront only (Caddy origin)"
    from_port       = 80
    to_port         = 80
    protocol        = "tcp"
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.cloudfront.id]
  }

  egress {
    description = "ECR, SSM, public data API, RDS"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-app-sg" }
}

resource "aws_security_group" "db" {
  name        = "${var.project_name}-db-sg"
  description = "PostgreSQL from app only"
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "PostgreSQL from app"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  tags = { Name = "${var.project_name}-db-sg" }
}
