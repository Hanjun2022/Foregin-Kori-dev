#!/usr/bin/env bash
set -euo pipefail
: "${ES_URL:?}"

ALIAS_SEARCH="${ALIAS_SEARCH:-events_read}"
ALIAS_WRITE="${ALIAS_WRITE:-events_write}"

CURL_OPTS=(--fail-with-body -sS)
[ "${ES_INSECURE:-0}" = "1" ] && CURL_OPTS+=(-k)

AUTH_OPT=()
if [ -n "${ES_API_KEY:-}" ]; then
  AUTH_OPT=(-H "Authorization: ApiKey ${ES_API_KEY}")
elif [ -n "${ES_AUTH:-}" ]; then
  AUTH_OPT=(-u "${ES_AUTH}")
fi

echo "== Aliases(events) =="
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" "${ES_URL}/_cat/aliases/${ALIAS_SEARCH},${ALIAS_WRITE}?v&h=alias,index,is_write_index"; echo
echo "== Count(via ${ALIAS_SEARCH}) =="
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" "${ES_URL}/${ALIAS_SEARCH}/_count"; echo