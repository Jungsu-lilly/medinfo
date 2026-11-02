# 이미지 저장소: medinfo-api, medinfo-web. 태그 = 커밋 앞 7자리(덮어쓰기 금지 → 어떤 커밋이 떠 있는지 항상 정확)
resource "aws_ecr_repository" "this" {
  for_each = toset(var.names)

  name                 = each.value
  image_tag_mutability = "IMMUTABLE"
  force_delete         = false

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = { Name = each.value }
}

resource "aws_ecr_lifecycle_policy" "this" {
  for_each   = aws_ecr_repository.this
  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "keep last ${var.keep_images} images (rollback)"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = var.keep_images
      }
      action = { type = "expire" }
    }]
  })
}
