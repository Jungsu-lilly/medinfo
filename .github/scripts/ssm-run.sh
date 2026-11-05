#!/usr/bin/env bash
# 운영 EC2에 SSM으로 셸 명령을 보내고 끝날 때까지 기다린다 (deploy.yml에서 사용)
#   ssm-run.sh <인스턴스 ID> <설명> <명령1> [명령2 …]
# 명령 출력(stdout)은 그대로 stdout으로, 오류 출력은 stderr로. 명령이 성공하면 0
set -euo pipefail

INSTANCE=$1
COMMENT=$2
shift 2

PARAMS=$(jq -n --args '{commands: $ARGS.positional, executionTimeout: ["900"]}' "$@")
CMD=$(aws ssm send-command --instance-ids "$INSTANCE" --document-name AWS-RunShellScript \
  --comment "$COMMENT" --parameters "$PARAMS" --query Command.CommandId --output text)
echo "SSM 명령 $CMD ($COMMENT)" >&2

STATUS=Pending
for _ in $(seq 1 190); do # 최대 약 16분 (executionTimeout 15분 + 여유)
  sleep 5
  STATUS=$(aws ssm get-command-invocation --command-id "$CMD" --instance-id "$INSTANCE" \
    --query Status --output text 2> /dev/null || echo Pending)
  case "$STATUS" in
    Success | Failed | Cancelled | TimedOut) break ;;
  esac
done

aws ssm get-command-invocation --command-id "$CMD" --instance-id "$INSTANCE" \
  --query StandardOutputContent --output text || true
ERR=$(aws ssm get-command-invocation --command-id "$CMD" --instance-id "$INSTANCE" \
  --query StandardErrorContent --output text 2> /dev/null || true)
[ -n "$ERR" ] && [ "$ERR" != "None" ] && echo "$ERR" >&2
echo "결과: $STATUS ($COMMENT)" >&2
[ "$STATUS" = "Success" ]
