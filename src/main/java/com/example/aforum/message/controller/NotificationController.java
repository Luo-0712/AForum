package com.example.aforum.message.controller;

import com.example.aforum.auth.service.AuthorizationService;
import com.example.aforum.common.exception.BusinessException;
import com.example.aforum.common.result.ErrorCode;
import com.example.aforum.common.result.PageResult;
import com.example.aforum.common.result.Result;
import com.example.aforum.common.util.SecurityUtils;
import com.example.aforum.message.dto.NotificationQueryDTO;
import com.example.aforum.message.dto.SystemNotificationDTO;
import com.example.aforum.message.service.NotificationService;
import com.example.aforum.message.service.PrivateMessageService;
import com.example.aforum.message.vo.MessageUnreadCountVO;
import com.example.aforum.message.vo.NotificationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final PrivateMessageService privateMessageService;
    private final AuthorizationService authorizationService;

    @GetMapping("/api/notifications")
    public Result<PageResult<NotificationVO>> listNotifications(NotificationQueryDTO query) {
        return Result.success(notificationService.listNotifications(SecurityUtils.getCurrentUserId(), query));
    }

    @GetMapping("/api/notifications/unread-count")
    public Result<MessageUnreadCountVO> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(new MessageUnreadCountVO(
                notificationService.countUnread(userId),
                privateMessageService.countUnread(userId)
        ));
    }

    @PutMapping("/api/notifications/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(SecurityUtils.getCurrentUserId(), id);
        return Result.success();
    }

    @PutMapping("/api/notifications/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtils.getCurrentUserId());
        return Result.success();
    }

    @DeleteMapping("/api/notifications/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(SecurityUtils.getCurrentUserId(), id);
        return Result.success();
    }

    @PostMapping("/api/admin/notifications/system")
    public Result<Void> sendSystemNotification(@Valid @RequestBody SystemNotificationDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!authorizationService.hasRole(userId, "ADMIN") && !authorizationService.hasRole(userId, "SUPER_ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        notificationService.notifySystem(dto);
        return Result.success();
    }
}
