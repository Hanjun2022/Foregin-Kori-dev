#!/usr/bin/env bash
set -euo pipefail

# === required ===
: "${ES_URL:?Set ES_URL (e.g. http://10.0.1.25:9200)}"
# === optional ===
ALIAS_SEARCH="${ALIAS_SEARCH:-posts_search}"
ALIAS_SUGG="${ALIAS_SUGG:-posts_suggest}"
ALIAS_WRITE="${ALIAS_WRITE:-posts_write}"
INDEX_PREFIX="${INDEX_PREFIX:-posts-lab}"

ES="$ES_URL"

CURL_OPTS=(--fail-with-body -sS)
[ "${ES_INSECURE:-0}" = "1" ] && CURL_OPTS+=(-k)

AUTH_OPT=()
if [ -n "${ES_API_KEY:-}" ]; then
  AUTH_OPT=(-H "Authorization: ApiKey ${ES_API_KEY}")
elif [ -n "${ES_AUTH:-}" ]; then
  AUTH_OPT=(-u "${ES_AUTH}")
fi

echo "== Aliases =="
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" "${ES}/_cat/aliases?v&h=alias,index,is_write_index" ; echo
echo "== Indices (${INDEX_PREFIX}-*) =="
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" "${ES}/_cat/indices/${INDEX_PREFIX}-*?v" ; echo
echo "== Mapping snapshot (via ${ALIAS_SEARCH}) =="
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" "${ES}/${ALIAS_SEARCH}/_mapping" | head -n 60 ; echo
echo "== Count (via ${ALIAS_SEARCH}) =="
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" "${ES}/${ALIAS_SEARCH}/_count" ; echo

echo "== Quick search smoke =="
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" -X POST "${ES}/${ALIAS_SEARCH}/_search" \
  -H 'Content-Type: application/json' -d @- <<'JSON'
{ "size": 1, "_source": ["content"],
  "query": { "match": { "content": "visa accommodation" } } }
JSON
echo

echo "== Quick suggest smoke =="
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" -X POST "${ES}/${ALIAS_SUGG}/_search" \
  -H 'Content-Type: application/json' -d @- <<'JSON'
{ "size": 0,
  "suggest": {
    "exact": {
      "prefix": "vis",
      "completion": {
        "field": "contentSuggestExact",
        "size": 5,
        "skip_duplicates": true,
        "contexts": { "boardId": ["1"] }
      }
    },
    "fuzzy": {
      "prefix": "vis",
      "completion": {
        "field": "contentSuggest",
        "size": 5,
        "skip_duplicates": true,
        "fuzzy": { "fuzziness": "AUTO" },
        "contexts": { "boardId": ["1"] }
      }
    }
  }
}
JSON
echo

echo "== Write alias is_write_index check =="
curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" "${ES}/_cat/aliases/${ALIAS_WRITE}?v&h=alias,index,is_write_index" ; echo
