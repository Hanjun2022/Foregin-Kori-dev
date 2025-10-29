#!/usr/bin/env bash
set -euo pipefail

# === required ===
: "${ES_URL:?Set ES_URL (e.g. http://10.0.1.25:9200)}"
# === optional ===
INDEX_PREFIX="${INDEX_PREFIX:-posts-lab}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-60s}"        # (cluster wait에서만 사용, non-fatal)
WAIT_FOR_STATUS="${WAIT_FOR_STATUS:-yellow}"   # 최소 yellow까지 대기

ES="$ES_URL"
NEW_INDEX="${INDEX_PREFIX}-$(date +%Y%m%d-%H%M%S)"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
LAST="$ROOT_DIR/.last_posts_index"

CURL_OPTS=(--fail-with-body -sS)
[ "${ES_INSECURE:-0}" = "1" ] && CURL_OPTS+=(-k)

AUTH_OPT=()
if [ -n "${ES_API_KEY:-}" ]; then
  AUTH_OPT=(-H "Authorization: ApiKey ${ES_API_KEY}")
elif [ -n "${ES_AUTH:-}" ]; then
  AUTH_OPT=(-u "${ES_AUTH}")
fi

echo "[ES] Creating index: ${NEW_INDEX}"
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" \
  -X PUT "${ES}/${NEW_INDEX}" \
  -H 'Content-Type: application/json' \
  -d '{}'

echo -n "${NEW_INDEX}" > "${LAST}"
echo "[OK] created index: ${NEW_INDEX} (saved to ${LAST})"

# --- allocation guard (dev/single-node & low-disk 안전장치) ---
# 워터마크를 bytes 기준으로 완화(임시), 환경 따라 퍼센트가 안 먹는 경우를 방지
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" -H 'Content-Type: application/json' \
  -X PUT "${ES}/_cluster/settings" -d '{
    "transient": {
      "cluster.routing.allocation.disk.watermark.low": "2gb",
      "cluster.routing.allocation.disk.watermark.high": "1500mb",
      "cluster.routing.allocation.disk.watermark.flood_stage": "1gb"
    }
  }' || true

# 템플릿이 이미 0이어도 idempotent. 단일 노드에서 안전.
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" \
  -H 'Content-Type: application/json' \
  -X PUT "${ES}/${NEW_INDEX}/_settings" \
  -d '{"index.number_of_replicas":"0"}' || true

# === 헬스 대기(클러스터 wait은 경고만) ===
echo "[ES] Waiting cluster to reach ${WAIT_FOR_STATUS} (timeout=${HEALTH_TIMEOUT})"
if ! curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" \
  "${ES}/_cluster/health?wait_for_status=${WAIT_FOR_STATUS}&timeout=${HEALTH_TIMEOUT}" >/dev/null; then
  echo "[WARN] cluster not ${WAIT_FOR_STATUS}; continue with per-index check"
fi

# === 인덱스 헬스 대기(재시도 루프) ===
echo "[ES] Waiting index ${NEW_INDEX} to reach ${WAIT_FOR_STATUS} (retry loop)"
TRIES=${HEALTH_RETRIES:-6}  # 6회 × 30s = 최대 3분
OK=0
for i in $(seq 1 "$TRIES"); do
  if curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" \
      "${ES}/_cluster/health/${NEW_INDEX}?wait_for_status=${WAIT_FOR_STATUS}&timeout=30s" >/dev/null; then
    OK=1; echo "[ES] ${NEW_INDEX} reached ${WAIT_FOR_STATUS}"; break
  fi
  echo "[ES] not ${WAIT_FOR_STATUS} yet... retry ${i}/${TRIES}"
  sleep 10
done

if [ "$OK" -ne 1 ]; then
  echo "[!] Shard allocation seems stuck. Allocation explain:"
  curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" \
    -H 'Content-Type: application/json' \
    -X POST "${ES}/_cluster/allocation/explain?pretty" \
    -d "{\"index\":\"${NEW_INDEX}\",\"shard\":0,\"primary\":true}" || true
  exit 1
fi

echo "[OK] index ${NEW_INDEX} is ready (${WAIT_FOR_STATUS}+)"
