package com.example.aforum.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aforum.common.exception.BusinessException;
import com.example.aforum.common.result.ErrorCode;
import com.example.aforum.common.result.PageResult;
import com.example.aforum.message.dto.CreateNotificationCommand;
import com.example.aforum.message.dto.NotificationQueryDTO;
import com.example.aforum.message.dto.SystemNotificationDTO;
import com.example.aforum.message.entity.Notification;
import com.example.aforum.message.mapper.NotificationMapper;
import com.example.aforum.message.service.MessagePushService;
import com.example.aforum.message.service.NotificationService;
import com.example.aforum.message.vo.NotificationVO;
import com.example.aforum.user.entity.User;
import com.example.aforum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int UNREAD = 0;
    private static final int READ = 1;
    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final MessagePushService messagePushService;

    @Override
    @Transactional
    public NotificationVO createNotification(CreateNotificationCommand command) {
        if (command.getUserId() == null || sameUser(command.getUserId(), command.getSenderId())) {
            return null;
        }
        if (userMapper.selectById(command.getUserId()) == null) {
            return null;
        }

        Notification notification = new Notification();
        notification.setUserId(command.getUserId());
        notification.setSenderId(command.getSenderId());
        notification.setType(limit(command.getType(), 20));
        notification.setTitle(limit(command.getTitle(), 100));
        notification.setContent(limit(command.getContent(), 500));
        notification.setTargetId(command.getTargetId());
        notification.setTargetType(limit(command.getTargetType(), 20));
        notification.setReadStatus(UNREAD);
        notification.setDeleted(0);
        notificationMapper.insert(notification);

        NotificationVO vo = toVO(notification);
        messagePushService.pushNotification(notification.getUserId(), vo);
        return vo;
    }

    @Override
    public void notifyComment(Long senderId, Long postAuthorId, Long postId, Long commentId) {
        createNotification(CreateNotificationCommand.builder()
                .userId(postAuthorId)
                .senderId(senderId)
                .type("comment")
                .title("收到新评论")
                .content(displayName(senderId) + " 评论了你的帖子")
                .targetId(postId)
                .targetType("post")
                .build());
    }

    @Override
    public void notifyReply(Long senderId, Long receiverId, Long postId, Long commentId) {
        createNotification(CreateNotificationCommand.builder()
                .userId(receiverId)
                .senderId(senderId)
                .type("reply")
                .title("收到新回复")
                .content(displayName(senderId) + " 回复了你的评论")
                .targetId(commentId)
                .targetType("comment")
                .build());
    }

    @Override
    public void notifyLike(Long senderId, Long receiverId, String targetType, Long targetId) {
        createNotification(CreateNotificationCommand.builder()
                .userId(receiverId)
                .senderId(senderId)
                .type("like")
                .title("收到点赞")
                .content(displayName(senderId) + " 点赞了你的内容")
                .targetId(targetId)
                .targetType(targetType)
                .build());
    }

    @Override
    public void notifyFavorite(Long senderId, Long receiverId, Long postId) {
        createNotification(CreateNotificationCommand.builder()
                .userId(receiverId)
                .senderId(senderId)
                .type("favorite")
                .title("帖子被收藏")
                .content(displayName(senderId) + " 收藏了你的帖子")
                .targetId(postId)
                .targetType("post")
                .build());
    }

    @Override
    public void notifyFollow(Long senderId, Long receiverId) {
        createNotification(CreateNotificationCommand.builder()
                .userId(receiverId)
                .senderId(senderId)
                .type("follow")
                .title("新增关注")
                .content(displayName(senderId) + " 关注了你")
                .targetId(senderId)
                .targetType("user")
                .build());
    }

    @Override
    public void notifyAudit(Long receiverId, String targetType, Long targetId, boolean approved, String reason) {
        String title = approved ? "内容已发布" : "内容未通过审核";
        String content = approved ? "你的内容已通过审核并发布" : "你的内容未通过审核：" + blankFallback(reason, "请调整后重新提交");
        createNotification(CreateNotificationCommand.builder()
                .userId(receiverId)
                .type("audit")
                .title(title)
                .content(content)
                .targetId(targetId)
                .targetType(targetType)
                .build());
    }

    @Override
    @Transactional
    public void notifySystem(SystemNotificationDTO dto) {
        if (Boolean.TRUE.equals(dto.getBroadcast())) {
            userMapper.selectList(new LambdaQueryWrapper<User>())
                    .forEach(user -> createSystemNotification(user.getId(), dto));
            return;
        }
        if (dto.getReceiverIds() == null || dto.getReceiverIds().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "接收人不能为空");
        }
        dto.getReceiverIds().stream().distinct().forEach(userId -> createSystemNotification(userId, dto));
    }

    @Override
    public PageResult<NotificationVO> listNotifications(Long userId, NotificationQueryDTO query) {
        long pageNum = sanitizePageNum(query.getPageNum());
        long pageSize = sanitizePageSize(query.getPageSize());
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt)
                .orderByDesc(Notification::getId);
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(Notification::getType, query.getType().trim());
        }
        if (query.getReadStatus() != null) {
            wrapper.eq(Notification::getReadStatus, query.getReadStatus());
        }
        Page<Notification> page = notificationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<NotificationVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    public long countUnread(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadStatus, UNREAD));
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification notification = requireOwnedNotification(userId, notificationId);
        if (!Integer.valueOf(READ).equals(notification.getReadStatus())) {
            notification.setReadStatus(READ);
            notificationMapper.updateById(notification);
            messagePushService.pushUnreadCount(userId);
        }
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        List<Notification> unread = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadStatus, UNREAD));
        for (Notification notification : unread) {
            notification.setReadStatus(READ);
            notificationMapper.updateById(notification);
        }
        messagePushService.pushUnreadCount(userId);
    }

    @Override
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = requireOwnedNotification(userId, notificationId);
        notificationMapper.deleteById(notification.getId());
        messagePushService.pushUnreadCount(userId);
    }

    private void createSystemNotification(Long userId, SystemNotificationDTO dto) {
        createNotification(CreateNotificationCommand.builder()
                .userId(userId)
                .type("system")
                .title(dto.getTitle())
                .content(dto.getContent())
                .targetType("system")
                .build());
    }

    private Notification requireOwnedNotification(Long userId, Long notificationId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !userId.equals(notification.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        return notification;
    }

    private NotificationVO toVO(Notification notification) {
        User sender = notification.getSenderId() == null ? null : userMapper.selectById(notification.getSenderId());
        return NotificationVO.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .senderId(notification.getSenderId())
                .senderName(displayName(sender))
                .senderAvatar(sender == null ? null : sender.getAvatar())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .targetId(notification.getTargetId())
                .targetType(notification.getTargetType())
                .readStatus(notification.getReadStatus())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private String displayName(Long userId) {
        return displayName(userId == null ? null : userMapper.selectById(userId));
    }

    private String displayName(User user) {
        if (user == null) {
            return "系统";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private String blankFallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private boolean sameUser(Long first, Long second) {
        return first != null && second != null && first.equals(second);
    }

    private long sanitizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    private long sanitizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
