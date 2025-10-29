
ALTER TABLE public.user_notification_setting
DROP CONSTRAINT user_notification_setting_notification_type_check;

ALTER TABLE public.user_notification_setting
    ADD CONSTRAINT user_notification_setting_notification_type_check
        CHECK (notification_type IN ('post', 'comment', 'chat', 'follow', 'receive'));