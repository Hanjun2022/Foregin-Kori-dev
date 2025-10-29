
ALTER TABLE chat_message
    ADD COLUMN message_type VARCHAR(255) NOT NULL DEFAULT 'TEXT';


ALTER TABLE chat_message
    ALTER COLUMN message_type DROP DEFAULT;