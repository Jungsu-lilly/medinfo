# RDS PostgreSQL 16 (PostGIS·pg_trgm은 V1 마이그레이션이 CREATE EXTENSION. 마스터 사용자로 접속하면 됨)
# Flicker와 다른 점: MySQL → PostgreSQL, 실서비스라 삭제 보호·마지막 스냅샷 켬

resource "aws_db_subnet_group" "this" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = var.subnet_ids
  tags       = { Name = "${var.project_name}-db-subnet-group" }
}

resource "aws_db_instance" "this" {
  identifier = "${var.project_name}-db"

  engine                     = "postgres"
  engine_version             = var.engine_version
  auto_minor_version_upgrade = true
  instance_class             = var.instance_class

  allocated_storage     = var.allocated_storage
  max_allocated_storage = var.max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password
  port     = 5432

  vpc_security_group_ids = [var.security_group_id]
  db_subnet_group_name   = aws_db_subnet_group.this.name
  publicly_accessible    = false
  multi_az               = false # 비용. 장애 시 스냅샷 복구로 감수 (월 약 18달러 → 36달러)

  backup_retention_period = 7
  backup_window           = "17:00-18:00"         # UTC = KST 02:00-03:00
  maintenance_window      = "sun:18:30-sun:19:30" # UTC = KST 월 03:30-04:30
  copy_tags_to_snapshot   = true

  deletion_protection       = var.deletion_protection
  skip_final_snapshot       = false
  final_snapshot_identifier = "${var.project_name}-db-final"

  tags = { Name = "${var.project_name}-db" }
}
