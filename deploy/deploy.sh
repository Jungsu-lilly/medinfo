#!/usr/bin/env bash
# 운영 EC2에서 한 색(blue·green)을 새 이미지로 띄우고 준비될 때까지 기다린다.
# GitHub Actions가 SSM으로 실행: bash deploy.sh <blue|green> <이미지 태그> <ECR 레지스트리>
# 실패하면 띄운 색을 내리고 1로 끝남 → 지금 서비스 중인 색은 건드리지 않음
set -euo pipefail

COLOR=$1
TAG=$2
REGISTRY=$3
export AWS_REGION=${AWS_REGION:-ap-northeast-2}

case "$COLOR" in
  blue)  WEB_PORT=3000; MGMT_PORT=9000 ;;
  green) WEB_PORT=3001; MGMT_PORT=9001 ;;
  *) echo "색은 blue 또는 green: $COLOR"; exit 2 ;;
esac
export COLOR WEB_PORT MGMT_PORT
export API_IMAGE="$REGISTRY/medinfo-api:$TAG"
export WEB_IMAGE="$REGISTRY/medinfo-web:$TAG"
PROJECT="medinfo-$COLOR"
cd /opt/medinfo

echo "==> 비밀값: SSM /medinfo/prod/* → .env"
umask 077
aws ssm get-parameters-by-path --path /medinfo/prod/ --with-decryption --region "$AWS_REGION" \
  --query 'Parameters[].[Name,Value]' --output text \
  | while IFS=$'\t' read -r name value; do echo "${name##*/}=${value}"; done > .env.new
for key in DB_URL DB_USERNAME DB_PASSWORD PUBLIC_DATA_SERVICE_KEY; do
  grep -q "^${key}=" .env.new || { echo "SSM에 /medinfo/prod/${key} 없음"; exit 1; }
done
mv .env.new .env

echo "==> ECR 로그인, 이미지 받기 ($TAG)"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY"
docker compose -p "$PROJECT" -f compose.yml pull

echo "==> $COLOR 띄우기 (web :$WEB_PORT, api 관리 :$MGMT_PORT)"
docker compose -p "$PROJECT" -f compose.yml up -d --force-recreate

fail() {
  echo "!! $1"
  docker compose -p "$PROJECT" -f compose.yml logs --tail 80 || true
  docker compose -p "$PROJECT" -f compose.yml down --timeout 40 || true
  exit 1
}

# 첫 배포는 Flyway 마이그레이션 + 수집 시작이라 오래 걸릴 수 있음 → 최대 5분
echo "==> api 준비 대기"
for i in $(seq 1 100); do
  if curl -fsS "http://127.0.0.1:$MGMT_PORT/actuator/health/readiness" > /dev/null 2>&1; then
    echo "api 준비됨 (${i}번째 확인)"; break
  fi
  [ "$i" -eq 100 ] && fail "api가 5분 안에 준비되지 않음"
  sleep 3
done

echo "==> web 준비 대기"
for i in $(seq 1 40); do
  if curl -fsS "http://127.0.0.1:$WEB_PORT/healthz" > /dev/null 2>&1; then
    echo "web 준비됨 (${i}번째 확인)"; break
  fi
  [ "$i" -eq 40 ] && fail "web이 2분 안에 준비되지 않음"
  sleep 3
done

# web → api 연결까지 확인 (같은 색끼리 붙었는지)
curl -fsS "http://127.0.0.1:$WEB_PORT/api/regions" > /dev/null || fail "web에서 api 호출 실패"

docker compose -p "$PROJECT" -f compose.yml ps
echo "==> $COLOR 준비 완료"
