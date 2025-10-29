
ALTER TABLE user_notification_setting
DROP CONSTRAINT IF EXISTS user_notification_setting_notification_type_check;

ALTER TABLE user_notification_setting
    ADD CONSTRAINT user_notification_setting_notification_type_check
        CHECK (
            notification_type IN (
                                  'post',
                                  'comment',
                                  'chat',
                                  'follow',
                                  'receive',
                                  'newuser',
                                  'followuserpost'
                )
            );
