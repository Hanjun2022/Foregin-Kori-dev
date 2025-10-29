-- 이 파일에는 "CONCURRENTLY" 딱 하나만!
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS uk_chatroom_user_idx
    ON chat_participant (chatroom_id, user_id);
