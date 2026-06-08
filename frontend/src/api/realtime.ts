import type { UnreadCount } from './notifications';

export interface RealtimeEnvelope {
  type?: string;
  payload?: unknown;
  unreadNotificationCount?: number | string;
  unreadMessageCount?: number | string;
}

interface RealtimeHandlers {
  onUnreadCount?: (counts: UnreadCount) => void;
  onNotification?: (message: RealtimeEnvelope) => void;
  onMessage?: (message: RealtimeEnvelope) => void;
}

export interface RealtimeClient {
  disconnect: () => void;
}

const FRAME_END = '\0';

export function connectRealtime(token: string, handlers: RealtimeHandlers): RealtimeClient {
  let socket: WebSocket | null = null;
  let connected = false;
  let reconnectTimer: number | undefined;

  const sendFrame = (command: string, headers: Record<string, string> = {}, body = '') => {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      return;
    }
    const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`);
    socket.send(`${command}\n${headerLines.join('\n')}\n\n${body}${FRAME_END}`);
  };

  const subscribe = () => {
    sendFrame('SUBSCRIBE', { id: 'notifications', destination: '/user/queue/notifications' });
    sendFrame('SUBSCRIBE', { id: 'messages', destination: '/user/queue/messages' });
    sendFrame('SUBSCRIBE', { id: 'unread-count', destination: '/user/queue/unread-count' });
  };

  const handleMessage = (frame: string) => {
    if (frame.startsWith('CONNECTED')) {
      connected = true;
      subscribe();
      return;
    }
    const destination = frame.match(/destination:([^\n]+)/)?.[1] || '';
    const body = frame.slice(frame.indexOf('\n\n') + 2).replace(/\0$/, '');
    if (!body) {
      return;
    }
    const envelope = JSON.parse(body) as RealtimeEnvelope;
    if (destination.endsWith('/unread-count')) {
      handlers.onUnreadCount?.(envelope as UnreadCount);
      return;
    }
    handlers.onUnreadCount?.({
      notificationUnreadCount: envelope.unreadNotificationCount ?? 0,
      messageUnreadCount: envelope.unreadMessageCount ?? 0,
    });
    if (destination.endsWith('/notifications')) {
      handlers.onNotification?.(envelope);
    } else if (destination.endsWith('/messages')) {
      handlers.onMessage?.(envelope);
    }
  };

  const open = () => {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    socket = new WebSocket(`${protocol}://${window.location.host}/ws?token=${encodeURIComponent(token)}`);
    socket.onopen = () => sendFrame('CONNECT', { 'accept-version': '1.2', host: window.location.host });
    socket.onmessage = (event) => {
      String(event.data)
        .split(FRAME_END)
        .filter(Boolean)
        .forEach((frame) => handleMessage(`${frame}${FRAME_END}`));
    };
    socket.onclose = () => {
      if (connected) {
        reconnectTimer = window.setTimeout(open, 3000);
      }
    };
  };

  open();

  return {
    disconnect: () => {
      connected = false;
      if (reconnectTimer) {
        window.clearTimeout(reconnectTimer);
      }
      if (socket?.readyState === WebSocket.OPEN) {
        sendFrame('DISCONNECT');
      }
      socket?.close();
    },
  };
}
