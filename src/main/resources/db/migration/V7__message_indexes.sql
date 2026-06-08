ALTER TABLE `notification`
    ADD INDEX `idx_notification_user_read_created` (`user_id`, `read_status`, `created_at`),
    ADD INDEX `idx_notification_user_type_created` (`user_id`, `type`, `created_at`),
    ADD INDEX `idx_notification_target` (`target_type`, `target_id`);

ALTER TABLE `private_message`
    ADD INDEX `idx_private_message_receiver_read_created` (`receiver_id`, `read_status`, `created_at`),
    ADD INDEX `idx_private_message_sender_receiver_created` (`sender_id`, `receiver_id`, `created_at`);
