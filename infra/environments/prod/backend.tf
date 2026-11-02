# 상태 파일은 Flicker 때 만든 S3 버킷·DynamoDB 잠금 테이블을 같이 쓴다 (키만 다르게)
terraform {
  required_version = ">= 1.6"

  backend "s3" {
    bucket         = "flicker-tfstate-637423170275"
    key            = "medinfo/prod/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "flicker-tfstate-lock"
    encrypt        = true
  }
}
