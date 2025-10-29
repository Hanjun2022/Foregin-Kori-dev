-- 절대 DO $$ 같은 블록 넣지 말고, 순수 DDL만
ALTER TABLE chat_participant
DROP CONSTRAINT IF EXISTS uk35bojfpxnaxph2c9htc9oqkh6;

ALTER TABLE chat_participant
    ADD CONSTRAINT uk_chatroom_user UNIQUE USING INDEX uk_chatroom_user_idx;
