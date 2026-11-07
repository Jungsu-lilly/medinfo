# 비용 안전장치: 봇·공격으로 트래픽이 튀어도 요금이 계속 쌓이지 않게
#   1. 차단기: EC2가 밖으로 내보내는 데이터가 비정상적으로 많으면 서버를 자동으로 멈춤 (5분 단위로 감시)
#      → 최악의 경우에도 과금이 몇 달러에서 멈춤. 멈추면 메일이 오고, 원인 확인 후 콘솔에서 다시 시작
#   2. 경고: 그 전 단계(평소보다 많음)와 로그 수집량 급증은 메일만
#   3. 예산: 이번 달 실제 80%, 예상 100% 넘으면 메일 (청구 데이터 기준이라 몇 시간 늦음)
#      (비용 이상 감지는 계정에 AWS 기본 모니터가 이미 있을 수 있어 여기서 만들지 않음 → 콘솔 Cost Anomaly Detection에서 확인)
# AWS에는 "얼마가 되면 결제를 막는" 한도가 없어서, 실시간 지표(1)로 막고 청구 기준(3)으로 한 번 더 본다

data "aws_region" "current" {}

# ── 알림 받을 곳 ─────────────────────────────────────

resource "aws_sns_topic" "alerts" {
  name = "${var.project_name}-alerts"
}

# 메일 구독은 받은 메일의 "Confirm subscription"을 눌러야 시작됨
resource "aws_sns_topic_subscription" "email" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

# ── 1·2. 실시간 지표 알람 (EC2 기본 모니터링 = 5분 단위) ─────────

# 경고: 5분에 300MB 넘게 나감 (페이지 약 0.5MB 기준 5분에 600번 이상) → 메일
resource "aws_cloudwatch_metric_alarm" "egress_warn" {
  alarm_name          = "${var.project_name}-egress-warn"
  alarm_description   = "EC2 outbound traffic is unusually high (possible bot traffic)"
  namespace           = "AWS/EC2"
  metric_name         = "NetworkOut"
  dimensions          = { InstanceId = var.instance_id }
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = var.egress_warn_bytes_per_5min
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]
}

# 차단기: 5분에 1GB 넘게 나가는 상태가 10분 계속 → 서버 정지 + 메일
#   1GB/5분 ≈ 27Mbps. 지금 사용자 수로는 나올 수 없는 양 (공격 아니면 바이럴)
#   최대 속도(약 32MB/s)로 퍼부어도 10~15분 안에 멈춤 → 약 25GB, 3달러 수준에서 끝남
resource "aws_cloudwatch_metric_alarm" "egress_stop" {
  alarm_name          = "${var.project_name}-egress-circuit-breaker"
  alarm_description   = "EC2 outbound traffic far above normal for 10 minutes. Instance stopped to cap data transfer cost. Check Caddy logs, then start the instance again."
  namespace           = "AWS/EC2"
  metric_name         = "NetworkOut"
  dimensions          = { InstanceId = var.instance_id }
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 2
  datapoints_to_alarm = 2
  threshold           = var.egress_stop_bytes_per_5min
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = ["arn:aws:automate:${data.aws_region.current.name}:ec2:stop", aws_sns_topic.alerts.arn]
}

# 로그 수집량 급증 (요청이 몰리면 접속 기록도 같이 늘어 CloudWatch Logs 요금이 붙음) → 메일
resource "aws_cloudwatch_metric_alarm" "logs_warn" {
  alarm_name          = "${var.project_name}-log-ingest-warn"
  alarm_description   = "CloudWatch Logs ingestion for the app log group is unusually high"
  namespace           = "AWS/Logs"
  metric_name         = "IncomingBytes"
  dimensions          = { LogGroupName = var.log_group_name }
  statistic           = "Sum"
  period              = 3600
  evaluation_periods  = 1
  threshold           = var.logs_warn_bytes_per_hour
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

# ── 3. 예산 (청구 데이터 기준) ─────────────────────────

resource "aws_budgets_budget" "monthly" {
  name         = "${var.project_name}-monthly"
  budget_type  = "COST"
  limit_amount = tostring(var.monthly_budget_usd)
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  # 이번 달 실제 사용이 80%
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.alert_email]
  }

  # 이 속도면 이번 달이 예산을 넘을 것 같음
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.alert_email]
  }
}
