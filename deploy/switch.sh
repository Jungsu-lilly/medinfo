#!/usr/bin/env bash
# Caddy가 트래픽을 보내는 색을 바꾼다: bash switch.sh <blue|green>
# GitHub Actions가 SSM으로 실행 (deploy.sh로 새 색이 준비된 뒤)
#   1. active.caddy를 새 색 포트로 바꾸고 caddy reload (처리 중인 요청은 끊지 않음)
#   2. 실제 도메인 주소로 Caddy를 거쳐 응답 확인
#   3. 확인 실패면 이전 색으로 되돌리고 1로 끝남
# 성공하면 /opt/medinfo/active-color 에 지금 색을 기록 (다음 배포가 반대 색을 고르는 기준)
set -euo pipefail

COLOR=$1
DOMAIN=${DOMAIN:-xn--639ao13amqh.com}
export AWS_REGION=${AWS_REGION:-ap-northeast-2}

case "$COLOR" in
  blue)  PORT=3000 ;;
  green) PORT=3001 ;;
  *) echo "색은 blue 또는 green: $COLOR"; exit 2 ;;
esac

cd /opt/medinfo
mkdir -p caddy caddy-data caddy-config
PREV=$(cat active-color 2>/dev/null || echo none)

edge() { docker compose -p medinfo-edge -f edge.yml "$@"; }
caddy_cmd() { edge exec -T caddy caddy "$@"; }

point_to() {
  echo "reverse_proxy 127.0.0.1:$1" > caddy/active.caddy
}

reload() {
  caddy_cmd validate --config /etc/caddy/Caddyfile --adapter caddyfile > /dev/null
  caddy_cmd reload --config /etc/caddy/Caddyfile --adapter caddyfile
}

echo "==> Caddy 설정: $PREV → $COLOR (web :$PORT)"
cp Caddyfile caddy/Caddyfile
point_to "$PORT"

# Caddy가 없으면 띄움 (첫 배포·서버 재생성). 떠 있고 edge.yml이 그대로면 아무것도 안 함
edge up -d
for _ in $(seq 1 20); do
  caddy_cmd version > /dev/null 2>&1 && break
  sleep 1
done
reload

# 첫 배포는 인증서 발급 때문에 수십 초 걸릴 수 있음 → 최대 90초
echo "==> https://$DOMAIN 응답 확인 (Caddy 경유)"
ok=false
for i in $(seq 1 30); do
  if curl -fsS --max-time 5 --resolve "$DOMAIN:443:127.0.0.1" "https://$DOMAIN/healthz" > /dev/null 2>&1 \
    && curl -fsS --max-time 10 --resolve "$DOMAIN:443:127.0.0.1" "https://$DOMAIN/api/regions" > /dev/null 2>&1; then
    echo "응답 정상 (${i}번째 확인)"
    ok=true
    break
  fi
  sleep 3
done

if [ "$ok" != true ]; then
  echo "!! Caddy 경유 응답 확인 실패"
  edge logs --tail 40 caddy || true
  case "$PREV" in
    blue)  point_to 3000; reload && echo "이전 색(blue)으로 되돌림" ;;
    green) point_to 3001; reload && echo "이전 색(green)으로 되돌림" ;;
  esac
  exit 1
fi

echo "$COLOR" > active-color
echo "==> 전환 완료: $PREV → $COLOR"
