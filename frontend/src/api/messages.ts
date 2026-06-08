import api from './axios';
import type { ApiResponse, Numeric, PageResult } from './posts';

export interface ConversationItem {
  peerUserId: Numeric;
  peerName: string;
  peerAvatar?: string;
  lastMessageId: Numeric;
  lastSenderId: Numeric;
  lastMessageContent: string;
  lastMessageAt: string;
  unreadCount: Numeric;
}

export interface PrivateMessageItem {
  id: Numeric;
  senderId: Numeric;
  senderName: string;
  senderAvatar?: string;
  receiverId: Numeric;
  receiverName: string;
  receiverAvatar?: string;
  content: string;
  messageType: string;
  readStatus: number;
  createdAt: string;
}

export interface SendMessagePayload {
  receiverId: Numeric;
  content: string;
}

export function listConversations(params: { pageNum?: number; pageSize?: number } = {}) {
  return api.get<unknown, ApiResponse<PageResult<ConversationItem>>>('/messages/conversations', { params });
}

export function listConversationMessages(
  peerUserId: Numeric,
  params: { pageNum?: number; pageSize?: number } = {},
) {
  return api.get<unknown, ApiResponse<PageResult<PrivateMessageItem>>>(
    `/messages/conversations/${peerUserId}`,
    { params },
  );
}

export function sendPrivateMessage(payload: SendMessagePayload) {
  return api.post<unknown, ApiResponse<PrivateMessageItem>>('/messages', {
    ...payload,
    messageType: 'text',
  });
}

export function markConversationRead(peerUserId: Numeric) {
  return api.put<unknown, ApiResponse<void>>(`/messages/conversations/${peerUserId}/read`);
}

export function countUnreadMessages() {
  return api.get<unknown, ApiResponse<Numeric>>('/messages/unread-count');
}
