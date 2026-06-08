import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Avatar, Button, Empty, List, Segmented, Space, Spin, Tag, message } from 'antd';
import { DeleteOutlined, UserOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import {
  deleteNotification,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationItem,
} from '../api/notifications';

const FILTERS = [
  { label: '全部', value: '' },
  { label: '未读', value: 'unread' },
  { label: '评论', value: 'comment' },
  { label: '互动', value: 'activity' },
  { label: '系统', value: 'system' },
];

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

function targetPath(item: NotificationItem) {
  if (item.type === 'message') {
    return item.senderId ? `/messages/${item.senderId}` : '/messages';
  }
  if (item.targetType === 'post' && item.targetId) {
    return `/posts/${item.targetId}`;
  }
  if (item.targetType === 'user' && item.targetId) {
    return `/user/${item.targetId}`;
  }
  return undefined;
}

export default function Notifications() {
  const [filter, setFilter] = useState('');
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['notifications', filter],
    queryFn: () =>
      listNotifications({
        pageNum: 1,
        pageSize: 30,
        readStatus: filter === 'unread' ? 0 : undefined,
        type: ['comment', 'reply', 'system'].includes(filter) ? filter : undefined,
      }),
  });

  const records = query.data?.data.records || [];
  const visibleRecords = filter === 'activity'
    ? records.filter((item) => ['like', 'favorite', 'follow'].includes(item.type))
    : records;

  const markReadMutation = useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unread-count'] });
    },
  });

  const markAllMutation = useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => {
      message.success('通知已全部标记为已读');
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unread-count'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteNotification,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  });

  const openNotification = async (item: NotificationItem) => {
    if (item.readStatus === 0) {
      await markReadMutation.mutateAsync(item.id);
    }
    const path = targetPath(item);
    if (path) {
      navigate(path);
    }
  };

  return (
    <div className="forum-page">
      <div className="page-heading">
        <div>
          <span className="eyebrow">Inbox</span>
          <h1>通知中心</h1>
          <p>评论、互动、审核和系统通知集中在这里。</p>
        </div>
        <Button onClick={() => markAllMutation.mutate()} loading={markAllMutation.isPending}>
          全部已读
        </Button>
      </div>

      <section className="section-card notification-panel">
        <Segmented className="message-filter" value={filter} options={FILTERS} onChange={(value) => setFilter(String(value))} />

        {query.isLoading ? (
          <div className="center-panel">
            <Spin />
          </div>
        ) : visibleRecords.length ? (
          <List
            dataSource={visibleRecords}
            renderItem={(item) => (
              <List.Item
                className={item.readStatus === 0 ? 'notification-item notification-item--unread' : 'notification-item'}
                actions={[
                  <Button
                    aria-label="删除通知"
                    icon={<DeleteOutlined />}
                    type="text"
                    onClick={(event) => {
                      event.stopPropagation();
                      deleteMutation.mutate(item.id);
                    }}
                  />,
                ]}
                onClick={() => openNotification(item)}
              >
                <List.Item.Meta
                  avatar={<Avatar src={item.senderAvatar} icon={<UserOutlined />} />}
                  title={
                    <div className="notification-title">
                      <span>{item.title}</span>
                      {item.readStatus === 0 && <Tag color="green">未读</Tag>}
                    </div>
                  }
                  description={
                    <div className="notification-copy">
                      <span>{item.content}</span>
                      <Space size={8}>
                        <Tag>{item.type}</Tag>
                        <time>{formatDate(item.createdAt)}</time>
                      </Space>
                    </div>
                  }
                />
              </List.Item>
            )}
          />
        ) : (
          <Empty description="暂无通知" />
        )}
      </section>
    </div>
  );
}
