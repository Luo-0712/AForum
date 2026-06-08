package com.example.aforum.message.service.impl;

import com.example.aforum.message.service.MessagePushService;
import com.example.aforum.message.service.NotificationService;
import com.example.aforum.message.service.PrivateMessageService;
import com.example.aforum.message.vo.MessageUnreadCountVO;
import com.example.aforum.message.vo.RealtimeMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WebSocketMessagePushService implements MessagePushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectProvider<NotificationService> notificationServiceProvider;
    private final ObjectProvider<PrivateMessageService> privateMessageServiceProvider;

    @Override
    public void pushNotification(Long userId, Object payload) {
        RealtimeMessageVO message = envelope("notification.created", userId, payload);
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/notifications", message);
        pushUnreadCount(userId);
    }

    @Override
    public void pushPrivateMessage(Long userId, Object payload) {
        RealtimeMessageVO message = envelope("message.created", userId, payload);
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/messages", message);
        pushUnreadCount(userId);
    }

    @Override
    public void pushUnreadCount(Long userId) {
        if (userId == null) {
            return;
        }
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/unread-count",
                new MessageUnreadCountVO(countNotifications(userId), countMessages(userId)));
    }

    private RealtimeMessageVO envelope(String type, Long userId, Object payload) {
        return RealtimeMessageVO.builder()
                .type(type)
                .userId(userId)
                .payload(payload)
                .unreadNotificationCount(countNotifications(userId))
                .unreadMessageCount(countMessages(userId))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private long countNotifications(Long userId) {
        NotificationService notificationService = notificationServiceProvider.getIfAvailable();
        return notificationService == null ? 0L : notificationService.countUnread(userId);
    }

    private long countMessages(Long userId) {
        PrivateMessageService privateMessageService = privateMessageServiceProvider.getIfAvailable();
        return privateMessageService == null ? 0L : privateMessageService.countUnread(userId);
    }
}
