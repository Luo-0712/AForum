package com.example.aforum.message.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeMessageVO {

    private String type;

    private Long userId;

    private Object payload;

    private Long unreadNotificationCount;

    private Long unreadMessageCount;

    private LocalDateTime createdAt;
}
