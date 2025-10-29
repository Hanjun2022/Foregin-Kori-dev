/* 
 V4__pgroonga_baseline.sql
 - 확장 설치(있으면 스킵)
 - 검색용 래퍼 함수들 생성
 - 생성(계산) 컬럼 추가 (아랍어 디아크리틱 제거, 유럽어권 unaccent/lower)
*/

-- 1) 확장
CREATE EXTENSION IF NOT EXISTS pgroonga;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- 2) unaccent IMMUTABLE 래퍼 (생성 컬럼에서 IMMUTABLE 필요)
CREATE OR REPLACE FUNCTION unaccent_immutable(text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
SELECT unaccent($1)
           $$;

-- 3) PGroonga 쿼리/점수 래퍼
--    ※ 환경에 따라 pgroonga_query_escape / pgroonga_score 가 schema 없이 노출됨
--      (public 같은). 지금 DB가 그 케이스라서 스키마 없이 호출.
CREATE OR REPLACE FUNCTION pgroonga_match(t text, q text)
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
SELECT ($1 &@~ pgroonga_query_escape($2))::boolean
$$;

CREATE OR REPLACE FUNCTION pgroonga_match_prefix(t text, q text)
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
SELECT ($1 &@~ (pgroonga_query_escape($2) || '*'))::boolean
$$;

-- 점수 래퍼: 해당 post_id 의 실제 tuple (tableoid, ctid)로 pgroonga_score를 찾는 방식
-- (QueryDSL/Hibernate에서 tableoid/ctid 직접 쓰기 난해한 문제 회피)
CREATE OR REPLACE FUNCTION pgroonga_score_of(pid bigint)
RETURNS double precision
LANGUAGE sql
STABLE
AS $$
SELECT pgroonga_score(tableoid, ctid)
FROM post
WHERE post_id = $1
    LIMIT 1
$$;

-- 4) 생성(계산) 컬럼 추가
-- 4-1) 아랍어: 디아크리틱(모음 기호) 제거 버전
ALTER TABLE post
    ADD COLUMN IF NOT EXISTS post_content_ar_norm text
    GENERATED ALWAYS AS (
    regexp_replace(
    post_content,
    '[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED]',  -- 하라카트 등
    '',
    'g'
    )
    ) STORED;

-- 4-2) 유럽어권: 악센트 제거 + 소문자 정규화 버전
ALTER TABLE post
    ADD COLUMN IF NOT EXISTS post_content_norm text
    GENERATED ALWAYS AS (
    lower(unaccent_immutable(post_content))
    ) STORED;
