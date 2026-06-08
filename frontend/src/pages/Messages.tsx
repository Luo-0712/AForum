import { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Avatar, Badge, Button, Empty, Form, Input, List, Spin, Typography, message } from 'antd';
import { SendOutlined, UserOutlined } from '@ant-design/icons';
import {
  listConversationMessages,
  listConversations,
  markConversationRead,
  sendPrivateMessage,
} from '../api/messages';
import { useAuthStore } from '../store/auth';

const { TextArea } = Input;
const { Text } = Typography;

interface MessageForm {
  content: string;
}

function formatDate(value?: string) {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export default function Messages() {
  const { peerUserId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const currentUser = useAuthStore((state) => state.user);
  const [form] = Form.useForm<MessageForm>();

  const conversationsQuery = useQuery({
    queryKey: ['message-conversations'],
    queryFn: () => listConversations({ pageNum: 1, pageSize: 30 }),
  });

  const messagesQuery = useQuery({
    queryKey: ['conversation-messages', peerUserId],
    queryFn: () => listConversationMessages(peerUserId || '', { pageNum: 1, pageSize: 50 }),
    enabled: Boolean(peerUserId),
  });

  const markReadMutation = useMutation({
    mutationFn: markConversationRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['message-conversations'] });
      queryClient.invalidateQueries({ queryKey: ['unread-count'] });
    },
  });
  const markRead = markReadMutation.mutate;

  useEffect(() => {
    if (peerUserId && messagesQuery.data) {
      markRead(peerUserId);
    }
  }, [markRead, messagesQuery.data, peerUserId]);

  const sendMutation = useMutation({
    mutationFn: (values: MessageForm) =>
      sendPrivateMessage({
        receiverId: peerUserId || '',
        content: values.content,
      }),
    onSuccess: () => {
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['conversation-messages', peerUserId] });
      queryClient.invalidateQueries({ queryKey: ['message-conversations'] });
    },
    onError: () => message.error('私信发送失败，请稍后再试'),
  });

  const conversations = conversationsQuery.data?.data.records || [];
  const messages = messagesQuery.data?.data.records || [];
  const activeConversation = conversations.find((item) => String(item.peerUserId) === String(peerUserId));
  const activePeerName = activeConversation?.peerName || (peerUserId ? `用户 ${peerUserId}` : '选择会话');

  return (
    <div className="forum-page">
      <div className="page-heading">
        <div>
          <span className="eyebrow">Messages</span>
          <h1>私信</h1>
          <p>和社区成员继续一对一交流。</p>
        </div>
      </div>

      <section className="message-shell section-card">
        <aside className="conversation-pane">
          {conversationsQuery.isLoading ? (
            <div className="center-panel">
              <Spin />
            </div>
          ) : conversations.length ? (
            <List
              dataSource={conversations}
              renderItem={(conversation) => (
                <List.Item
                  className={
                    String(conversation.peerUserId) === String(peerUserId)
                      ? 'conversation-item conversation-item--active'
                      : 'conversation-item'
                  }
                  onClick={() => navigate(`/messages/${conversation.peerUserId}`)}
                >
                  <List.Item.Meta
                    avatar={<Avatar src={conversation.peerAvatar} icon={<UserOutlined />} />}
                    title={
                      <span className="conversation-title">
                        <span>{conversation.peerName}</span>
                        {Number(conversation.unreadCount) > 0 && (
                          <Badge count={Number(conversation.unreadCount)} size="small" />
                        )}
                      </span>
                    }
                    description={
                      <span className="conversation-preview">
                        <Text ellipsis>{conversation.lastMessageContent}</Text>
                        <time>{formatDate(conversation.lastMessageAt)}</time>
                      </span>
                    }
                  />
                </List.Item>
              )}
            />
          ) : (
            <Empty className="message-empty" description="暂无私信会话" />
          )}
        </aside>

        <main className="thread-pane">
          {peerUserId ? (
            <>
              <header className="thread-header">
                <Avatar src={activeConversation?.peerAvatar} icon={<UserOutlined />} />
                <div>
                  <strong>{activePeerName}</strong>
                  <Link to={`/user/${peerUserId}`}>查看主页</Link>
                </div>
              </header>

              <div className="thread-list">
                {messagesQuery.isLoading ? (
                  <div className="center-panel">
                    <Spin />
                  </div>
                ) : messages.length ? (
                  messages.map((item) => {
                    const isMine = String(item.senderId) === String(currentUser?.id);
                    return (
                      <div className={isMine ? 'thread-message thread-message--mine' : 'thread-message'} key={String(item.id)}>
                        <Avatar size={30} src={isMine ? currentUser?.avatar : item.senderAvatar} icon={<UserOutlined />} />
                        <div>
                          <p>{item.content}</p>
                          <time>{formatDate(item.createdAt)}</time>
                        </div>
                      </div>
                    );
                  })
                ) : (
                  <Empty description="还没有消息" />
                )}
              </div>

              <Form form={form} className="message-composer" layout="vertical" onFinish={(values) => sendMutation.mutate(values)}>
                <Form.Item name="content" rules={[{ required: true, message: '请输入私信内容' }]}>
                  <TextArea rows={3} maxLength={2000} showCount placeholder="写一条私信" />
                </Form.Item>
                <Button type="primary" icon={<SendOutlined />} htmlType="submit" loading={sendMutation.isPending}>
                  发送
                </Button>
              </Form>
            </>
          ) : (
            <Empty className="message-empty" description="选择一个会话开始查看" />
          )}
        </main>
      </section>
    </div>
  );
}
