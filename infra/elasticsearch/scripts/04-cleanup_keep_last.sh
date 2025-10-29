#!/usr/bin/env bash
set -euo pipefail

: "${ES_URL:?}"
INDEX_PREFIX="${INDEX_PREFIX:-posts-lab}"
KEEP_LAST="${KEEP_LAST:-1}"     # 최신부터 n개 보관
DRY_RUN="${DRY_RUN:-1}"         # 1=미리보기, 0=실삭제
NEW_INDEX="${NEW_INDEX:-}"      # 새 인덱스는 보호

CURL_OPTS=(--fail-with-body -sS)
[ "${ES_INSECURE:-0}" = "1" ] && CURL_OPTS+=(-k)

AUTH_OPT=()
if [ -n "${ES_API_KEY:-}" ]; then
  AUTH_OPT=(-H "Authorization: ApiKey ${ES_API_KEY}")
elif [ -n "${ES_AUTH:-}" ]; then
  AUTH_OPT=(-u "${ES_AUTH}")
fi

# 1) 보호 목록(별칭이 가리키는 인덱스 + NEW_INDEX)
mapfile -t protected < <(curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" \
  "$ES_URL/_cat/aliases/posts_search,posts_suggest,posts_write?h=index" 2>/dev/null \
  | awk 'NF' | sort -u || true)

if [ -n "${NEW_INDEX}" ]; then
  protected+=("${NEW_INDEX}")
fi

# 2) 후보 인덱스: 생성시각 내림차순 정렬(서버 측 정렬)
mapfile -t candidates < <(curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" \
  "$ES_URL/_cat/indices/${INDEX_PREFIX}-*?h=index,creation.date&s=creation.date:desc" 2>/dev/null \
  | awk '{print $1}' | awk 'NF' || true)

# 3) 보호 제외
filtered=()
for idx in "${candidates[@]}"; do
  skip=0
  for p in "${protected[@]:-}"; do
    [[ "$idx" == "$p" ]] && { skip=1; break; }
  done
  ((skip)) || filtered+=("$idx")
done

# 4) KEEP_LAST 만큼 보관, 나머지 삭제 대상
to_delete=()
if ((${#filtered[@]} > KEEP_LAST)); then
  to_delete=("${filtered[@]:KEEP_LAST}")
fi

if [ ${#to_delete[@]} -eq 0 ]; then
  echo "[OK] 삭제 대상 없음"
  exit 0
fi

echo "[TARGET] 삭제 대상:"
printf '  %s\n' "${to_delete[@]}"

if [ "$DRY_RUN" = "1" ]; then
  echo "[DRY-RUN] 실제 삭제하지 않았습니다. DRY_RUN=0 으로 실행하면 삭제합니다."
  exit 0
fi

for idx in "${to_delete[@]}"; do
  echo "[DEL] $idx"
  curl "${CURL_OPTS[@]}" "${AUTH_OPT[@]}" -X DELETE "$ES_URL/$idx" || true
done

echo "[DONE]"
