package com.example.aforum.message.controller;

import com.example.aforum.common.request.PageRequest;
import com.example.aforum.common.result.PageResult;
import com.example.aforum.common.result.Result;
import com.example.aforum.common.util.SecurityUtils;
import com.example.aforum.message.dto.SendPrivateMessageDTO;
import com.example.aforum.message.service.PrivateMessageService;
import com.example.aforum.message.vo.ConversationVO;
import com.example.aforum.message.vo.PrivateMessageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;

    @GetMapping("/conversations")
    public Result<PageResult<ConversationVO>> listConversations(PageRequest request) {
        return Result.success(privateMessageService.listConversations(SecurityUtils.getCurrentUserId(), request));
    }

    @GetMapping("/conversations/{peerUserId}")
    public Result<PageResult<PrivateMessageVO>> listConversationMessages(@PathVariable Long peerUserId,
                                                                         PageRequest request) {
        return Result.success(privateMessageService.listConversationMessages(
                SecurityUtils.getCurrentUserId(), peerUserId, request));
    }

    @PostMapping
    public Result<PrivateMessageVO> sendMessage(@Valid @RequestBody SendPrivateMessageDTO dto) {
        return Result.success(privateMessageService.sendMessage(SecurityUtils.getCurrentUserId(), dto));
    }

    @PutMapping("/conversations/{peerUserId}/read")
    public Result<Void> markConversationRead(@PathVariable Long peerUserId) {
        privateMessageService.markConversationRead(SecurityUtils.getCurrentUserId(), peerUserId);
        return Result.success();
    }

    @GetMapping("/unread-count")
    public Result<Long> countUnread() {
        return Result.success(privateMessageService.countUnread(SecurityUtils.getCurrentUserId()));
    }
}
