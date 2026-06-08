package com.example.aforum.message.service;

public interface MessagePushService {

    void pushNotification(Long userId, Object payload);

    void pushPrivateMessage(Long userId, Object payload);

    void pushUnreadCount(Long userId);
}
