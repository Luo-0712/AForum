package com.example.aforum.message.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PrivateMessageVO {

    private Long id;

    private Long senderId;

    private String senderName;

    private String senderAvatar;

    private Long receiverId;

    private String receiverName;

    private String receiverAvatar;

    private String content;

    private String messageType;

    private Integer readStatus;

    private LocalDateTime createdAt;
}
