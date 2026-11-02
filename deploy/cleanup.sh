#!/usr/bin/env bash
# 전환이 끝난 뒤(또는 실패한 배포의) 한 색을 내린다: bash cleanup.sh <blue|green>
set -euo pipefail
COLOR=$1
case "$COLOR" in blue|green) ;; *) echo "색은 blue 또는 green: $COLOR"; exit 2 ;; esac
cd /opt/medinfo
# compose.yml의 이미지 변수는 down에 필요 없지만, 비어 있으면 경고가 나서 채워 둠
export API_IMAGE=none WEB_IMAGE=none COLOR WEB_PORT=0 MGMT_PORT=0 AWS_REGION=${AWS_REGION:-ap-northeast-2}
docker compose -p "medinfo-$COLOR" -f compose.yml down --timeout 40
docker image prune -af --filter "until=72h" > /dev/null || true
echo "==> $COLOR 내림"
