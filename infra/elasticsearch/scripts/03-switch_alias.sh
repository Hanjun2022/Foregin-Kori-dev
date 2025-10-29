#!/usr/bin/env bash
set -euo pipefail

: "${ES_URL:?Set ES_URL (e.g. http://127.0.0.1:9200)}"
ALIAS_SEARCH="${ALIAS_SEARCH:-posts_search}"
ALIAS_SUGG="${ALIAS_SUGG:-posts_suggest}"
ALIAS_WRITE="${ALIAS_WRITE:-posts_write}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
LAST="$ROOT_DIR/.last_posts_index"
: "${LAST:?missing .last_posts_index}"
NEW_INDEX="$(cat "$LAST")"

CURL_OPTS=(--fail-with-body -sS)
[ "${ES_INSECURE:-0}" = "1" ] && CURL_OPTS+=(-k)

AUTH_OPT=()
if [ -n "${ES_API_KEY:-}" ]; then
  AUTH_OPT=(-H "Authorization: ApiKey ${ES_API_KEY}")
elif [ -n "${ES_AUTH:-}" ]; then
  AUTH_OPT=(-u "${ES_AUTH}")
fi

echo "[ES] Switching aliases to ${NEW_INDEX}"

COMBINED_PAYLOAD=$(cat <<JSON
{
  "actions": [
    { "remove": { "index": "*", "alias": "${ALIAS_SEARCH}", "must_exist": false } },
    { "remove": { "index": "*", "alias": "${ALIAS_SUGG}",  "must_exist": false } },
    { "remove": { "index": "*", "alias": "${ALIAS_WRITE}", "must_exist": false } },
    { "add": { "index": "${NEW_INDEX}", "alias": "${ALIAS_SEARCH}" } },
    { "add": { "index": "${NEW_INDEX}", "alias": "${ALIAS_SUGG}" } },
    { "add": { "index": "${NEW_INDEX}", "alias": "${ALIAS_WRITE}", "is_write_index": true } }
  ]
}
JSON
)

curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" -X POST "${ES_URL}/_aliases" \
  -H 'Content-Type: application/json' -d "${COMBINED_PAYLOAD}"

echo "[OK] aliases -> ${NEW_INDEX}"

