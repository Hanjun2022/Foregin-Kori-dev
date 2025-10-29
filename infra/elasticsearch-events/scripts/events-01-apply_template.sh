#!/usr/bin/env bash
set -euo pipefail
: "${ES_URL:?Set ES_URL (e.g. http://127.0.0.1:9200)}"

TEMPLATE_NAME="${TEMPLATE_NAME:-events-template-v1}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TPL="$ROOT_DIR/events-template.json"

CURL_OPTS=(--fail-with-body -sS)
[ "${ES_INSECURE:-0}" = "1" ] && CURL_OPTS+=(-k)

AUTH_OPT=()
if [ -n "${ES_API_KEY:-}" ]; then
  AUTH_OPT=(-H "Authorization: ApiKey ${ES_API_KEY}")
elif [ -n "${ES_AUTH:-}" ]; then
  AUTH_OPT=(-u "${ES_AUTH}")
fi

echo "[ES] Applying index template '${TEMPLATE_NAME}'"
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" \
  -X PUT "${ES_URL}/_index_template/${TEMPLATE_NAME}" \
  -H 'Content-Type: application/json' \
  --data-binary @"${TPL}"

echo "[OK] applied template '${TEMPLATE_NAME}'"
