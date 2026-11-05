# 앱 서버 EC2 한 대: Docker로 web·api를 색(blue·green)별 한 쌍씩 띄우고, 앞단 Caddy가 HTTPS를 받는다
# Flicker와 다른 점: EC2도 Terraform으로 만든다 (Flicker는 콘솔에서 만듦)
#   - SSH 없음: 접속·배포는 SSM (보안 그룹에 22번 없음)
#   - 고정 IP(탄력적 IP): 도메인 A 레코드가 가리킴. 서버를 다시 만들어도 IP는 그대로
#   - 비밀값은 SSM Parameter Store(/<project>/prod/*) → 배포 스크립트가 읽어 .env로

data "aws_region" "current" {}
data "aws_caller_identity" "current" {}

data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-${var.architecture}"
}

locals {
  param_prefix = "/${var.project_name}/prod"
  param_arn    = "arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:parameter${local.param_prefix}"
}

# ── IAM ─────────────────────────────────────────────

data "aws_iam_policy_document" "assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "this" {
  name               = "${var.project_name}-ec2-role"
  assume_role_policy = data.aws_iam_policy_document.assume.json
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.this.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy_attachment" "ecr_read" {
  role       = aws_iam_role.this.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

data "aws_iam_policy_document" "app" {
  statement {
    sid       = "ReadAppParameters"
    actions   = ["ssm:GetParameters", "ssm:GetParametersByPath"]
    resources = [local.param_arn, "${local.param_arn}/*"]
  }

  # SecureString 복호화 (AWS 관리형 키 aws/ssm, SSM을 거칠 때만)
  statement {
    sid       = "DecryptViaSsm"
    actions   = ["kms:Decrypt"]
    resources = ["*"]
    condition {
      test     = "StringEquals"
      variable = "kms:ViaService"
      values   = ["ssm.${data.aws_region.current.name}.amazonaws.com"]
    }
  }

  # 컨테이너 로그 → CloudWatch Logs (docker awslogs 드라이버)
  statement {
    sid       = "WriteContainerLogs"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents", "logs:DescribeLogStreams"]
    resources = ["${aws_cloudwatch_log_group.app.arn}:*"]
  }
}

resource "aws_iam_role_policy" "app" {
  name   = "${var.project_name}-ec2-app"
  role   = aws_iam_role.this.id
  policy = data.aws_iam_policy_document.app.json
}

resource "aws_iam_instance_profile" "this" {
  name = "${var.project_name}-ec2-profile"
  role = aws_iam_role.this.name
}

# ── 로그 ─────────────────────────────────────────────

resource "aws_cloudwatch_log_group" "app" {
  name              = "/${var.project_name}/prod"
  retention_in_days = var.log_retention_days
}

# ── EC2 ─────────────────────────────────────────────

resource "aws_instance" "this" {
  ami                         = data.aws_ssm_parameter.al2023.insecure_value # AMI ID는 비밀이 아님
  instance_type               = var.instance_type
  subnet_id                   = var.subnet_id
  vpc_security_group_ids      = [var.security_group_id]
  iam_instance_profile        = aws_iam_instance_profile.this.name
  associate_public_ip_address = true # NAT 없이 바깥으로 나가려고. 들어오는 건 보안 그룹이 막음

  user_data = templatefile("${path.module}/user_data.sh", {
    compose_version = var.compose_version
  })

  # IMDSv2만 (토큰 없는 메타데이터 요청 차단)
  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = var.root_volume_gb
    encrypted   = true
  }

  # T3·T4g 기본값 unlimited는 CPU를 오래 많이 쓰면 추가 과금 → standard(크레딧 다 쓰면 느려짐)로 예측 가능하게
  credit_specification {
    cpu_credits = "standard"
  }

  # AMI가 새로 나오거나 user_data를 고쳐도 서버를 갈아엎지 않는다 (바꾸려면 terraform apply -replace)
  # 아키텍처(x86↔ARM)를 바꿀 때도 -replace 필요 → infra/README.md
  lifecycle {
    ignore_changes = [ami, user_data]
  }

  tags = { Name = "${var.project_name}-app" }
}

# 고정 공인 IP. 연결은 따로 둬서 서버를 다시 만들어도 IP는 유지 (DNS를 안 바꿔도 됨)
resource "aws_eip" "this" {
  domain = "vpc"
  tags   = { Name = "${var.project_name}-app" }
}

resource "aws_eip_association" "this" {
  instance_id   = aws_instance.this.id
  allocation_id = aws_eip.this.id
}
