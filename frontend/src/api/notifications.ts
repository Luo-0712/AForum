import api from './axios';
import type { ApiResponse, Numeric, PageResult } from './posts';

export interface NotificationItem {
  id: Numeric;
  userId: Numeric;
  senderId?: Numeric;
  senderName?: string;
  senderAvatar?: string;
  type: string;
  title: string;
  content: string;
  targetId?: Numeric;
  targetType?: string;
  readStatus: number;
  createdAt: string;
}

export interface UnreadCount {
  notificationUnreadCount: Numeric;
  messageUnreadCount: Numeric;
}

export interface NotificationQuery {
  pageNum?: number;
  pageSize?: number;
  type?: string;
  readStatus?: number;
}

export function listNotifications(params: NotificationQuery = {}) {
  return api.get<unknown, ApiResponse<PageResult<NotificationItem>>>('/notifications', { params });
}

export function getUnreadCount() {
  return api.get<unknown, ApiResponse<UnreadCount>>('/notifications/unread-count');
}

export function markNotificationRead(id: Numeric) {
  return api.put<unknown, ApiResponse<void>>(`/notifications/${id}/read`);
}

export function markAllNotificationsRead() {
  return api.put<unknown, ApiResponse<void>>('/notifications/read-all');
}

export function deleteNotification(id: Numeric) {
  return api.delete<unknown, ApiResponse<void>>(`/notifications/${id}`);
}
