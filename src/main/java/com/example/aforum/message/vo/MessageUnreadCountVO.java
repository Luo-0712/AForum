package com.example.aforum.message.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageUnreadCountVO {

    private Long notificationUnreadCount;

    private Long messageUnreadCount;
}
