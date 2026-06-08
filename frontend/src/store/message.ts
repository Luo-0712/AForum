import { create } from 'zustand';
import type { Numeric } from '../api/posts';

interface MessageState {
  notificationUnreadCount: number;
  messageUnreadCount: number;
  setUnreadCounts: (counts: { notificationUnreadCount?: Numeric; messageUnreadCount?: Numeric }) => void;
  resetUnreadCounts: () => void;
}

function toNumber(value?: Numeric) {
  const parsed = Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

export const useMessageStore = create<MessageState>((set) => ({
  notificationUnreadCount: 0,
  messageUnreadCount: 0,
  setUnreadCounts: (counts) =>
    set((state) => ({
      notificationUnreadCount:
        counts.notificationUnreadCount === undefined
          ? state.notificationUnreadCount
          : toNumber(counts.notificationUnreadCount),
      messageUnreadCount:
        counts.messageUnreadCount === undefined ? state.messageUnreadCount : toNumber(counts.messageUnreadCount),
    })),
  resetUnreadCounts: () => set({ notificationUnreadCount: 0, messageUnreadCount: 0 }),
}));
