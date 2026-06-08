package com.example.aforum.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aforum.common.constant.UserStatus;
import com.example.aforum.common.exception.BusinessException;
import com.example.aforum.common.request.PageRequest;
import com.example.aforum.common.result.ErrorCode;
import com.example.aforum.common.result.PageResult;
import com.example.aforum.message.dto.CreateNotificationCommand;
import com.example.aforum.message.dto.SendPrivateMessageDTO;
import com.example.aforum.message.entity.PrivateMessage;
import com.example.aforum.message.mapper.PrivateMessageMapper;
import com.example.aforum.message.service.MessagePushService;
import com.example.aforum.message.service.NotificationService;
import com.example.aforum.message.service.PrivateMessageService;
import com.example.aforum.message.vo.ConversationVO;
import com.example.aforum.message.vo.PrivateMessageVO;
import com.example.aforum.user.entity.User;
import com.example.aforum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PrivateMessageServiceImpl implements PrivateMessageService {

    private static final int UNREAD = 0;
    private static final int READ = 1;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int USER_RATE_LIMIT_PER_MINUTE = 20;
    private static final int PEER_RATE_LIMIT_PER_MINUTE = 10;

    private final PrivateMessageMapper privateMessageMapper;
    private final UserMapper userMapper;
    private final MessagePushService messagePushService;
    private final NotificationService notificationService;
    private final Map<String, MinuteCounter> rateCounters = new ConcurrentHashMap<>();

    @Override
    public PageResult<ConversationVO> listConversations(Long currentUserId, PageRequest request) {
        requireUser(currentUserId);
        long pageNum = sanitizePageNum(request.getPageNum());
        long pageSize = sanitizePageSize(request.getPageSize());
        List<PrivateMessage> messages = privateMessageMapper.selectList(new LambdaQueryWrapper<PrivateMessage>()
                .and(wrapper -> wrapper.eq(PrivateMessage::getSenderId, currentUserId)
                        .or()
                        .eq(PrivateMessage::getReceiverId, currentUserId))
                .orderByDesc(PrivateMessage::getCreatedAt)
                .orderByDesc(PrivateMessage::getId));

        Map<Long, PrivateMessage> latestByPeer = new LinkedHashMap<>();
        for (PrivateMessage message : messages) {
            Long peerId = message.getSenderId().equals(currentUserId) ? message.getReceiverId() : message.getSenderId();
            latestByPeer.putIfAbsent(peerId, message);
        }

        List<ConversationVO> all = latestByPeer.entrySet().stream()
                .map(entry -> toConversationVO(currentUserId, entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ConversationVO::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int from = Math.toIntExact(Math.min((pageNum - 1) * pageSize, all.size()));
        int to = Math.toIntExact(Math.min(from + pageSize, all.size()));
        return new PageResult<>(all.subList(from, to), (long) all.size(), pageSize, pageNum);
    }

    @Override
    public PageResult<PrivateMessageVO> listConversationMessages(Long currentUserId, Long peerUserId, PageRequest request) {
        requireUser(currentUserId);
        requireUser(peerUserId);
        long pageNum = sanitizePageNum(request.getPageNum());
        long pageSize = sanitizePageSize(request.getPageSize());
        Page<PrivateMessage> page = privateMessageMapper.selectPage(new Page<>(pageNum, pageSize),
                conversationWrapper(currentUserId, peerUserId)
                        .orderByDesc(PrivateMessage::getCreatedAt)
                        .orderByDesc(PrivateMessage::getId));
        List<PrivateMessageVO> records = page.getRecords().stream()
                .sorted(Comparator.comparing(PrivateMessage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PrivateMessage::getId))
                .map(this::toVO)
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    @Transactional
    public PrivateMessageVO sendMessage(Long senderId, SendPrivateMessageDTO dto) {
        User sender = requireUser(senderId);
        User receiver = requireUser(dto.getReceiverId());
        if (senderId.equals(receiver.getId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能给自己发送私信");
        }
        if (!UserStatus.ACTIVE.getCode().equals(receiver.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "接收人不可用");
        }
        if (!"text".equalsIgnoreCase(StringUtils.hasText(dto.getMessageType()) ? dto.getMessageType().trim() : "text")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "暂仅支持文本私信");
        }
        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "私信内容不能为空");
        }
        if (content.length() > 2000) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "私信内容最长2000个字符");
        }
        enforceRateLimit(senderId, receiver.getId());

        PrivateMessage message = new PrivateMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiver.getId());
        message.setContent(content);
        message.setMessageType("text");
        message.setReadStatus(UNREAD);
        message.setDeleted(0);
        privateMessageMapper.insert(message);

        PrivateMessageVO vo = toVO(message);
        notificationService.createNotification(CreateNotificationCommand.builder()
                .userId(receiver.getId())
                .senderId(sender.getId())
                .type("message")
                .title("收到新私信")
                .content(displayName(sender) + " 给你发来一条私信")
                .targetId(sender.getId())
                .targetType("user")
                .build());
        messagePushService.pushPrivateMessage(receiver.getId(), vo);
        return vo;
    }

    @Override
    @Transactional
    public void markConversationRead(Long currentUserId, Long peerUserId) {
        requireUser(currentUserId);
        requireUser(peerUserId);
        privateMessageMapper.markConversationRead(currentUserId, peerUserId);
        messagePushService.pushUnreadCount(currentUserId);
    }

    @Override
    public long countUnread(Long currentUserId) {
        if (currentUserId == null) {
            return 0L;
        }
        return privateMessageMapper.selectCount(new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getReceiverId, currentUserId)
                .eq(PrivateMessage::getReadStatus, UNREAD));
    }

    private LambdaQueryWrapper<PrivateMessage> conversationWrapper(Long currentUserId, Long peerUserId) {
        return new LambdaQueryWrapper<PrivateMessage>()
                .and(wrapper -> wrapper.eq(PrivateMessage::getSenderId, currentUserId)
                        .eq(PrivateMessage::getReceiverId, peerUserId)
                        .or()
                        .eq(PrivateMessage::getSenderId, peerUserId)
                        .eq(PrivateMessage::getReceiverId, currentUserId));
    }

    private ConversationVO toConversationVO(Long currentUserId, Long peerId, PrivateMessage lastMessage) {
        User peer = userMapper.selectById(peerId);
        Long unreadCount = privateMessageMapper.selectCount(new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getSenderId, peerId)
                .eq(PrivateMessage::getReceiverId, currentUserId)
                .eq(PrivateMessage::getReadStatus, UNREAD));
        return ConversationVO.builder()
                .peerUserId(peerId)
                .peerName(displayName(peer))
                .peerAvatar(peer == null ? null : peer.getAvatar())
                .lastMessageId(lastMessage.getId())
                .lastSenderId(lastMessage.getSenderId())
                .lastMessageContent(lastMessage.getContent())
                .lastMessageAt(lastMessage.getCreatedAt())
                .unreadCount(unreadCount)
                .build();
    }

    private PrivateMessageVO toVO(PrivateMessage message) {
        User sender = userMapper.selectById(message.getSenderId());
        User receiver = userMapper.selectById(message.getReceiverId());
        return PrivateMessageVO.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .senderName(displayName(sender))
                .senderAvatar(sender == null ? null : sender.getAvatar())
                .receiverId(message.getReceiverId())
                .receiverName(displayName(receiver))
                .receiverAvatar(receiver == null ? null : receiver.getAvatar())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .readStatus(message.getReadStatus())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void enforceRateLimit(Long senderId, Long receiverId) {
        checkCounter("user:" + senderId, USER_RATE_LIMIT_PER_MINUTE);
        checkCounter("peer:" + senderId + ":" + receiverId, PEER_RATE_LIMIT_PER_MINUTE);
    }

    private void checkCounter(String key, int limit) {
        long minute = System.currentTimeMillis() / 60_000L;
        MinuteCounter counter = rateCounters.compute(key, (ignored, current) -> {
            if (current == null || current.minute() != minute) {
                return new MinuteCounter(minute, 1);
            }
            return new MinuteCounter(minute, current.count() + 1);
        });
        if (counter.count() > limit) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "发送过于频繁，请稍后再试");
        }
    }

    private String displayName(User user) {
        if (user == null) {
            return "匿名用户";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
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

    private record MinuteCounter(long minute, int count) {
    }
}
