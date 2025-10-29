#!/usr/bin/env bash
set -euo pipefail
: "${ES_URL:?}"

ALIAS_SEARCH="${ALIAS_SEARCH:-events_read}"
ALIAS_WRITE="${ALIAS_WRITE:-events_write}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAST="$SCRIPT_DIR/../.last_events_index"
: "${LAST:?missing .last_events_index}"
NEW_INDEX="$(cat "$LAST")"

CURL_OPTS=(--fail-with-body -sS)
[ "${ES_INSECURE:-0}" = "1" ] && CURL_OPTS+=(-k)

AUTH_OPT=()
if [ -n "${ES_API_KEY:-}" ]; then
  AUTH_OPT=(-H "Authorization: ApiKey ${ES_API_KEY}")
elif [ -n "${ES_AUTH:-}" ]; then
  AUTH_OPT=(-u "${ES_AUTH}")
fi

echo "[ES] Switching aliases -> ${NEW_INDEX}"
PAYLOAD=$(cat <<JSON
{
  "actions": [
    { "remove": { "index": "*", "alias": "${ALIAS_SEARCH}", "must_exist": false } },
    { "remove": { "index": "*", "alias": "${ALIAS_WRITE}",  "must_exist": false } },
    { "add":    { "index": "${NEW_INDEX}", "alias": "${ALIAS_SEARCH}" } },
    { "add":    { "index": "${NEW_INDEX}", "alias": "${ALIAS_WRITE}", "is_write_index": true } }
  ]
}
JSON
)
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" -X POST "${ES_URL}/_aliases" \
  -H 'Content-Type: application/json' -d "${PAYLOAD}"

echo "[OK] aliases set: ${ALIAS_SEARCH}, ${ALIAS_WRITE}"
