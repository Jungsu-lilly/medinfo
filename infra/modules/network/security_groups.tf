# 인터넷 → EC2(Caddy 80·443) → RDS(5432)
# EC2 안에서 Caddy가 127.0.0.1의 web(blue 3000 | green 3001)으로 넘기므로 web 포트는 밖에 안 엶
# (보안 그룹·규칙 description은 AWS가 ASCII만 받아서 영어로 씀)

resource "aws_security_group" "app" {
  # description은 바꾸면 보안 그룹을 새로 만든다(RDS 규칙까지 영향) → ALB 시절 문구 그대로 둠
  name        = "${var.project_name}-app-sg"
  description = "web blue/green ports from ALB only (no SSH, use SSM)"
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "HTTP (Caddy redirects to HTTPS, ACME challenge)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS (Caddy)"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "ECR, SSM, public data API, RDS, Let's Encrypt"
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
