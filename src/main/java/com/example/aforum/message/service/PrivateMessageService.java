package com.example.aforum.message.service;

import com.example.aforum.common.request.PageRequest;
import com.example.aforum.common.result.PageResult;
import com.example.aforum.message.dto.SendPrivateMessageDTO;
import com.example.aforum.message.vo.ConversationVO;
import com.example.aforum.message.vo.PrivateMessageVO;

public interface PrivateMessageService {

    PageResult<ConversationVO> listConversations(Long currentUserId, PageRequest request);

    PageResult<PrivateMessageVO> listConversationMessages(Long currentUserId, Long peerUserId, PageRequest request);

    PrivateMessageVO sendMessage(Long senderId, SendPrivateMessageDTO dto);

    void markConversationRead(Long currentUserId, Long peerUserId);

    long countUnread(Long currentUserId);
}
