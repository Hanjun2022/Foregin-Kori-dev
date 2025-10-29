-- flyway:executeInTransaction=false
SET lock_timeout = '5s';
SET statement_timeout = '30min';

/*
 V5__pgroonga_indexes_concurrent.sql
 - 모든 인덱스를 CREATE INDEX CONCURRENTLY 로 생성 (무중단)
 - 인덱스 재생성/추가도 안전하게 진행 가능
*/

-- CJK/혼합(한/중/영+기호 포함) 범용 바이그램
CREATE INDEX CONCURRENTLY IF NOT EXISTS post_content_cjk_pgroonga
    ON post USING pgroonga (post_content)
    WITH (
    tokenizer   = 'TokenBigramSplitSymbolAlphaDigit',
    normalizers = 'NormalizerNFKC100'
    );

-- 일본어 정밀(형태소)
CREATE INDEX CONCURRENTLY IF NOT EXISTS post_content_ja_pgroonga
    ON post USING pgroonga (post_content)
    WITH (
    tokenizer   = 'TokenMecab',
    normalizers = 'NormalizerNFKC100'
    );

-- 아랍어(디아크리틱 제거) 보조 컬럼
CREATE INDEX CONCURRENTLY IF NOT EXISTS post_content_ar_norm_pgroonga
    ON post USING pgroonga (post_content_ar_norm)
    WITH (
    tokenizer   = 'TokenBigram',
    normalizers = 'NormalizerNFKC100'
    );

-- 유럽어권 악센트/대소문자 무시 검색용 보조 컬럼
CREATE INDEX CONCURRENTLY IF NOT EXISTS post_content_norm_pgroonga
    ON post USING pgroonga (post_content_norm)
    WITH (
    tokenizer   = 'TokenBigramSplitSymbolAlphaDigit',
    normalizers = 'NormalizerNFKC100'
    );
