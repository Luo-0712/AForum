package com.example.aforum.message.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateNotificationCommand {

    private Long userId;

    private Long senderId;

    private String type;

    private String title;

    private String content;

    private Long targetId;

    private String targetType;
}
