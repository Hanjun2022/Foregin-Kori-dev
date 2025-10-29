#!/usr/bin/env bash
set -euo pipefail
: "${ES_URL:?}"

INDEX_PREFIX="${INDEX_PREFIX:-events-lab}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-60s}"
WAIT_FOR_STATUS="${WAIT_FOR_STATUS:-yellow}"

NEW_INDEX="${INDEX_PREFIX}-$(date +%Y%m%d-%H%M%S)"

CURL_OPTS=(--fail-with-body -sS)
[ "${ES_INSECURE:-0}" = "1" ] && CURL_OPTS+=(-k)

AUTH_OPT=()
if [ -n "${ES_API_KEY:-}" ]; then
  AUTH_OPT=(-H "Authorization: ApiKey ${ES_API_KEY}")
elif [ -n "${ES_AUTH:-}" ]; then
  AUTH_OPT=(-u "${ES_AUTH}")
fi

echo "[ES] Creating index: ${NEW_INDEX}"
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" -X PUT "${ES_URL}/${NEW_INDEX}" \
  -H 'Content-Type: application/json' -d '{}'

# 단일노드/개발 환경 보호: replica 0
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" -X PUT "${ES_URL}/${NEW_INDEX}/_settings" \
  -H 'Content-Type: application/json' -d '{"index.number_of_replicas":"0"}' || true

# 헬스 대기
curl -sS "${ES_URL}/_cluster/health/${NEW_INDEX}?wait_for_status=${WAIT_FOR_STATUS}&timeout=${HEALTH_TIMEOUT}" >/dev/null \
  || echo "[WARN] health wait timed out (continue)"

echo -n "${NEW_INDEX}" > "$(dirname "$0")/../.last_events_index"
echo "[OK] index ready: ${NEW_INDEX}"
