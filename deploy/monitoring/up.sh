#!/usr/bin/env bash
# 모니터링 묶음 띄우기·갱신: bash /opt/medinfo/monitoring/up.sh
# GitHub Actions가 배포 끝에 SSM으로 실행 (설정 파일은 바로 전에 저장소에서 받아 /opt/medinfo/monitoring 에 풀어 둠)
#   - 처음이면 데이터 폴더를 만들고 띄움
#   - 설정 파일이 바뀌었으면 해당 컨테이너를 다시 시작 (bind mount라 up -d만으로는 다시 안 읽음)
set -euo pipefail

cd /opt/medinfo/monitoring
DATA=/opt/medinfo/monitoring-data

# 컨테이너마다 실행 사용자가 달라서 데이터 폴더 주인을 맞춰 둠
mkdir -p "$DATA"/prometheus "$DATA"/loki "$DATA"/grafana
chown -R 65534:65534 "$DATA"/prometheus # prometheus: nobody
chown -R 10001:10001 "$DATA"/loki       # loki
chown -R 472:0 "$DATA"/grafana          # grafana

mon() { docker compose -p medinfo-monitoring -f compose.yml "$@"; }

echo "==> 모니터링 묶음 띄우기 (없거나 이미지가 바뀐 것만)"
mon up -d --remove-orphans

# 설정이 바뀐 서비스만 다시 시작
SUMS=/opt/medinfo/monitoring-data/config.sum
touch "$SUMS"
for svc in prometheus loki alloy grafana; do
  dir=$svc
  new=$(find "$dir" -type f -print0 | sort -z | xargs -0 sha256sum | sha256sum | cut -d' ' -f1)
  old=$(grep "^$svc " "$SUMS" | cut -d' ' -f2 || true)
  if [ "$new" != "$old" ]; then
    if [ -n "$old" ]; then
      echo "==> $svc 설정 바뀜 → 다시 시작"
      mon restart "$svc"
    fi
    grep -v "^$svc " "$SUMS" > "$SUMS.tmp" || true
    echo "$svc $new" >> "$SUMS.tmp"
    mv "$SUMS.tmp" "$SUMS"
  fi
done

echo "==> 준비 확인"
check() {
  for _ in $(seq 1 30); do
    curl -fsS -o /dev/null --max-time 3 "$2" && { echo "$1 정상"; return 0; }
    sleep 2
  done
  echo "!! $1 응답 없음 ($2)"
  mon logs --tail 30 "$1" || true
  return 1
}
check prometheus http://127.0.0.1:9090/-/ready
check loki http://127.0.0.1:3100/ready
check grafana http://127.0.0.1:3300/api/health
check alloy http://127.0.0.1:12345/-/ready

mon ps --format 'table {{.Name}}\t{{.Status}}'
