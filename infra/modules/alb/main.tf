# ALB 하나에 web 대상 그룹 2개(blue·green). 배포 때 GitHub Actions가 HTTPS 리스너의 대상을 바꿔 끼운다
#   - api(Spring)는 ALB에 직접 붙이지 않는다: 브라우저의 /api/* 요청도 web(Next.js)이 받아 같은 색의 api로 넘긴다
#     (next.config rewrites). 그래서 한 색 = web + api 한 쌍이고, 전환도 대상 그룹 하나만 바꾸면 된다
#   - HTTP(80)는 HTTPS로 넘기기만 한다

resource "aws_lb" "this" {
  name                       = "${var.project_name}-alb"
  internal                   = false
  load_balancer_type         = "application"
  security_groups            = [var.security_group_id]
  subnets                    = var.public_subnet_ids
  drop_invalid_header_fields = true

  tags = { Name = "${var.project_name}-alb" }
}

resource "aws_lb_target_group" "color" {
  for_each = var.color_ports

  name        = "${var.project_name}-${each.key}"
  port        = each.value
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "instance"

  health_check {
    enabled             = true
    path                = var.health_check_path
    matcher             = "200"
    interval            = 10
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  # 앱의 graceful shutdown(30초)보다 조금 길게 → 처리 중인 요청을 끊지 않음
  deregistration_delay = 35

  tags = { Name = "${var.project_name}-${each.key}", Color = each.key }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"
    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.this.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.certificate_arn

  # 처음엔 blue. 이후로는 배포가 바꾸므로 Terraform은 이 값을 건드리지 않는다
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.color["blue"].arn
  }

  lifecycle {
    ignore_changes = [default_action]
  }
}

# www.병원값.com → 병원값.com (주소 하나로 모아 검색 노출을 나누지 않음)
resource "aws_lb_listener_rule" "www_redirect" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 10

  action {
    type = "redirect"
    redirect {
      host        = var.domain
      status_code = "HTTP_301"
    }
  }

  condition {
    host_header {
      values = ["www.${var.domain}"]
    }
  }
}
