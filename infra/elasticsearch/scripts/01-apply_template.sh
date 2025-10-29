#!/usr/bin/env bash
set -euo pipefail

# === required ===
: "${ES_URL:?Set ES_URL (e.g. http://10.0.1.25:9200)}"
# === optional ===
TEMPLATE_NAME="${TEMPLATE_NAME:-posts-template-v1}"

ES="$ES_URL"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TPL="$ROOT_DIR/posts-template.json"

# curl opts
CURL_OPTS=(--fail-with-body -sS)
[ "${ES_INSECURE:-0}" = "1" ] && CURL_OPTS+=(-k)

# auth opts: prefer API key over basic
AUTH_OPT=()
if [ -n "${ES_API_KEY:-}" ]; then
  AUTH_OPT=(-H "Authorization: ApiKey ${ES_API_KEY}")
elif [ -n "${ES_AUTH:-}" ]; then
  AUTH_OPT=(-u "${ES_AUTH}")
fi

echo "[ES] Applying index template '${TEMPLATE_NAME}' to ${ES}"
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" \
  -X PUT "${ES}/_index_template/${TEMPLATE_NAME}" \
  -H 'Content-Type: application/json' \
  --data-binary @"${TPL}"

echo "[OK] applied index template '${TEMPLATE_NAME}'"
