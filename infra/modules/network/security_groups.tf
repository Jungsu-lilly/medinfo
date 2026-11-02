# 인터넷 → ALB(80·443) → EC2(web 포트) → RDS(5432). 각 단계는 바로 앞 단계 보안 그룹에서 오는 것만 허용
# (보안 그룹·규칙 description은 AWS가 ASCII만 받아서 영어로 씀)

resource "aws_security_group" "alb" {
  name        = "${var.project_name}-alb-sg"
  description = "HTTP/HTTPS from internet"
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "HTTP (redirected to HTTPS)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-alb-sg" }
}

resource "aws_security_group" "app" {
  name        = "${var.project_name}-app-sg"
  description = "web blue/green ports from ALB only (no SSH, use SSM)"
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "web blue/green"
    from_port       = var.app_port_from
    to_port         = var.app_port_to
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
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
