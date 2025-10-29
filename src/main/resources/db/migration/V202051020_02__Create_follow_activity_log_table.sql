-- V202051020_02__Create_follow_activity_log_table.sql (PostgreSQL용 수정본)

-- 1. 테이블 생성 (PostgreSQL 문법: BIGSERIAL 사용, COMMENT 제거)
CREATE TABLE follow_activity_log
(
    -- 기본 키: BIGSERIAL은 자동 증가 기능이 있는 BIGINT의 약어입니다.
    log_id                BIGSERIAL PRIMARY KEY,

    -- --- 기본 이벤트 정보 ---
    follower_id           BIGINT       NOT NULL,
    following_id          BIGINT       NOT NULL,
    action_type           VARCHAR(255) NOT NULL,
    created_at            TIMESTAMP    NOT NULL,
    source                VARCHAR(255) NULL,

    -- --- 팔로워(Follower)의 스냅샷 정보 ---
    follower_is_in_korea  BOOLEAN      NULL,
    follower_nationality  VARCHAR(255) NULL,
    follower_sex          VARCHAR(255) NULL,
    follower_birth_date   VARCHAR(255) NULL,
    follower_language     VARCHAR(500) NULL,

    -- --- 팔로잉(Following)의 스냅샷 정보 ---
    following_is_in_korea BOOLEAN      NULL,
    following_nationality VARCHAR(255) NULL,
    following_sex         VARCHAR(255) NULL,
    following_birth_date  VARCHAR(255) NULL,
    following_language    VARCHAR(500) NULL
);

-- 2. 테이블 주석 추가
COMMENT ON TABLE follow_activity_log IS '사용자 팔로우 활동에 대한 상세 로그 테이블';

-- 3. 컬럼 주석 추가 (컬럼별로 COMMENT ON COLUMN 사용)
COMMENT ON COLUMN follow_activity_log.log_id IS '기본 키';
COMMENT ON COLUMN follow_activity_log.follower_id IS '행동을 한 사용자(팔로워)의 ID';
COMMENT ON COLUMN follow_activity_log.following_id IS '행동의 대상이 된 사용자(팔로잉)의 ID';
COMMENT ON COLUMN follow_activity_log.action_type IS '활동 유형 (e.g., FOLLOW, UNFOLLOW)';
COMMENT ON COLUMN follow_activity_log.created_at IS '활동이 발생한 시간';
COMMENT ON COLUMN follow_activity_log.source IS '활동이 발생한 소스 (e.g., PROFILE, RECOMMEND)';
COMMENT ON COLUMN follow_activity_log.follower_is_in_korea IS '팔로워의 한국 거주 여부 (이벤트 시점)';
COMMENT ON COLUMN follow_activity_log.follower_nationality IS '팔로워의 국적 (이벤트 시점)';
COMMENT ON COLUMN follow_activity_log.follower_sex IS '팔로워의 성별 (이벤트 시점)';
COMMENT ON COLUMN follow_activity_log.follower_birth_date IS '팔로워의 생년월일 (이벤트 시점)';
COMMENT ON COLUMN follow_activity_log.follower_language IS '팔로워가 사용하는 언어 (이벤트 시점)';
COMMENT ON COLUMN follow_activity_log.following_is_in_korea IS '팔로잉의 한국 거주 여부 (이벤트 시점)';
COMMENT ON COLUMN follow_activity_log.following_nationality IS '팔로잉의 국적 (이벤트 시점)';
COMMENT ON COLUMN follow_activity_log.following_sex IS '팔로잉의 성별 (이벤트 시점)';
COMMENT ON COLUMN follow_activity_log.following_birth_date IS '팔로잉의 생년월일 (이벤트 시점)';
COMMENT ON COLUMN follow_activity_log.following_language IS '팔로잉이 사용하는 언어 (이벤트 시점)';