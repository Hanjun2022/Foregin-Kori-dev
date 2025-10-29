ALTER TABLE notification
    ADD COLUMN actor_id BIGINT NULL;

ALTER TABLE notification
    ADD CONSTRAINT fk_notification_actor
        FOREIGN KEY (actor_id) REFERENCES users(user_id);