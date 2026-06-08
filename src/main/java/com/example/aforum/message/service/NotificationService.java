package com.example.aforum.message.service;

import com.example.aforum.common.result.PageResult;
import com.example.aforum.message.dto.CreateNotificationCommand;
import com.example.aforum.message.dto.NotificationQueryDTO;
import com.example.aforum.message.dto.SystemNotificationDTO;
import com.example.aforum.message.vo.NotificationVO;

public interface NotificationService {

    NotificationVO createNotification(CreateNotificationCommand command);

    void notifyComment(Long senderId, Long postAuthorId, Long postId, Long commentId);

    void notifyReply(Long senderId, Long receiverId, Long postId, Long commentId);

    void notifyLike(Long senderId, Long receiverId, String targetType, Long targetId);

    void notifyFavorite(Long senderId, Long receiverId, Long postId);

    void notifyFollow(Long senderId, Long receiverId);

    void notifyAudit(Long receiverId, String targetType, Long targetId, boolean approved, String reason);

    void notifySystem(SystemNotificationDTO dto);

    PageResult<NotificationVO> listNotifications(Long userId, NotificationQueryDTO query);

    long countUnread(Long userId);

    void markRead(Long userId, Long notificationId);

    void markAllRead(Long userId);

    void deleteNotification(Long userId, Long notificationId);
}
