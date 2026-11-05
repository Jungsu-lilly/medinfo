# GitHub Actions가 AWS에 접속하는 역할 (OIDC, 액세스 키 없음)
# main 브랜치의 워크플로만 이 역할을 쓸 수 있고, 할 수 있는 일은 배포에 필요한 것뿐:
#   ECR 이미지 올리기 / EC2 찾기 / EC2에 SSM 명령 보내기 (색 전환은 서버 안 Caddy가 함)

data "aws_region" "current" {}

# GitHub OIDC 공급자는 계정에 하나만 만들 수 있다. Flicker 때 만든 게 있으면 그대로 쓴다
resource "aws_iam_openid_connect_provider" "github" {
  count = var.create_oidc_provider ? 1 : 0

  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1", "1c58a3a8518e8759bf075b76b750d4f2df264fcd"]
}

data "aws_iam_openid_connect_provider" "github" {
  count = var.create_oidc_provider ? 0 : 1
  url   = "https://token.actions.githubusercontent.com"
}

locals {
  oidc_provider_arn = var.create_oidc_provider ? aws_iam_openid_connect_provider.github[0].arn : data.aws_iam_openid_connect_provider.github[0].arn

  # 토큰의 sub 값. 2026-07-15 이후 만든 저장소는 이름 뒤에 바뀌지 않는 ID가 붙는다
  #   예전: repo:Jungsu-lilly/medinfo:ref:refs/heads/main
  #   지금: repo:Jungsu-lilly@56336436/medinfo@1428310945:ref:refs/heads/main
  # (이름만 보면 저장소를 지우고 같은 이름으로 다시 만든 사람도 통과 → ID로 막음)
  # https://github.blog/changelog/2026-04-23-immutable-subject-claims-for-github-actions-oidc-tokens
  owner = split("/", var.github_repo)[0]
  repo  = split("/", var.github_repo)[1]
  subjects = [
    "repo:${local.owner}@${var.github_owner_id}/${local.repo}@${var.github_repo_id}:ref:refs/heads/${var.branch}",
  ]
}

data "aws_iam_policy_document" "trust" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [local.oidc_provider_arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = local.subjects
    }
  }
}

resource "aws_iam_role" "this" {
  name               = "${var.project_name}-github-deploy"
  assume_role_policy = data.aws_iam_policy_document.trust.json
}

data "aws_iam_policy_document" "deploy" {
  statement {
    sid       = "EcrLogin"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "EcrPush"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:CompleteLayerUpload",
      "ecr:DescribeImages",
      "ecr:GetDownloadUrlForLayer",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
    ]
    resources = var.ecr_repository_arns
  }

  # Describe 계열은 리소스 지정이 안 되는 API라 *
  statement {
    sid = "Describe"
    actions = [
      "ec2:DescribeInstances",
      "ssm:DescribeInstanceInformation",
      "ssm:GetCommandInvocation",
    ]
    resources = ["*"]
  }

  statement {
    sid     = "RunDeployScript"
    actions = ["ssm:SendCommand"]
    resources = [
      var.instance_arn,
      "arn:aws:ssm:${data.aws_region.current.name}::document/AWS-RunShellScript",
    ]
  }
}

resource "aws_iam_role_policy" "deploy" {
  name   = "${var.project_name}-github-deploy"
  role   = aws_iam_role.this.id
  policy = data.aws_iam_policy_document.deploy.json
}
