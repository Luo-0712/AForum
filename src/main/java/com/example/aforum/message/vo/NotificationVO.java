package com.example.aforum.message.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationVO {

    private Long id;

    private Long userId;

    private Long senderId;

    private String senderName;

    private String senderAvatar;

    private String type;

    private String title;

    private String content;

    private Long targetId;

    private String targetType;

    private Integer readStatus;

    private LocalDateTime createdAt;
}
