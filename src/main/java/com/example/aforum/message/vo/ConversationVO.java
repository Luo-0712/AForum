package com.example.aforum.message.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationVO {

    private Long peerUserId;

    private String peerName;

    private String peerAvatar;

    private Long lastMessageId;

    private Long lastSenderId;

    private String lastMessageContent;

    private LocalDateTime lastMessageAt;

    private Long unreadCount;
}
