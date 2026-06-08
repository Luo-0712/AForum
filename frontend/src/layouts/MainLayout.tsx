import { useEffect } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Badge, Button, Dropdown, Input, Layout, Menu } from 'antd';
import { BellOutlined, LogoutOutlined, MailOutlined, PlusOutlined, SettingOutlined, UserOutlined } from '@ant-design/icons';
import { getUnreadCount } from '../api/notifications';
import { connectRealtime } from '../api/realtime';
import NotificationBell from '../components/NotificationBell';
import { useAuthStore } from '../store/auth';
import { useMessageStore } from '../store/message';
import { queryClient } from '../queryClient';

const { Header, Content, Footer } = Layout;
const { Search } = Input;

export default function MainLayout() {
  const { user, token, logout } = useAuthStore();
  const messageUnreadCount = useMessageStore((state) => state.messageUnreadCount);
  const setUnreadCounts = useMessageStore((state) => state.setUnreadCounts);
  const resetUnreadCounts = useMessageStore((state) => state.resetUnreadCounts);
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    resetUnreadCounts();
    logout();
    navigate('/login');
  };

  useEffect(() => {
    if (!token) {
      resetUnreadCounts();
      return undefined;
    }

    getUnreadCount()
      .then((response) => setUnreadCounts(response.data))
      .catch(() => undefined);

    const client = connectRealtime(token, {
      onUnreadCount: setUnreadCounts,
      onNotification: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
      onMessage: () => {
        queryClient.invalidateQueries({ queryKey: ['message-conversations'] });
        queryClient.invalidateQueries({ queryKey: ['conversation-messages'] });
      },
    });

    return () => client.disconnect();
  }, [resetUnreadCounts, setUnreadCounts, token]);

  const selectedKey = location.pathname.startsWith('/posts') ? 'posts' : 'home';
  const displayName = user?.nickname || user?.username || '我的账号';

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
      onClick: () => navigate('/user'),
    },
    {
      key: 'notifications',
      icon: <BellOutlined />,
      label: '通知中心',
      onClick: () => navigate('/notifications'),
    },
    {
      key: 'messages',
      icon: <MailOutlined />,
      label: '私信',
      onClick: () => navigate('/messages'),
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: '设置',
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ];

  return (
    <Layout className="app-layout">
      <Header className="forum-header">
        <div className="forum-header__inner">
          <button className="brand-mark ghost-button" type="button" onClick={() => navigate('/')}>
            AForum
          </button>

          <Menu
            className="forum-menu"
            mode="horizontal"
            selectedKeys={[selectedKey]}
            items={[
              { key: 'home', label: '首页', onClick: () => navigate('/') },
              { key: 'posts', label: '帖子', onClick: () => navigate('/posts') },
            ]}
          />

          <div className="forum-actions">
            <Search
              allowClear
              placeholder="搜索帖子"
              onSearch={(value) => {
                const keyword = value.trim();
                navigate(keyword ? `/posts?keyword=${encodeURIComponent(keyword)}` : '/posts');
              }}
            />
            {token && (
              <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/posts/new')}>
                发帖
              </Button>
            )}
            {token ? (
              <>
                <NotificationBell />
                <Badge count={messageUnreadCount} size="small" overflowCount={99}>
                  <Button aria-label="私信" icon={<MailOutlined />} onClick={() => navigate('/messages')} />
                </Badge>
                <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
                  <Button icon={<UserOutlined />}>{displayName}</Button>
                </Dropdown>
              </>
            ) : (
              <>
                <Button type="text" onClick={() => navigate('/login')}>
                  登录
                </Button>
                <Button type="primary" onClick={() => navigate('/register')}>
                  注册
                </Button>
              </>
            )}
          </div>
        </div>
      </Header>

      <Content className="forum-content">
        <Outlet />
      </Content>

      <Footer className="forum-footer">AForum &copy; {new Date().getFullYear()} - 开发者讨论社区</Footer>
    </Layout>
  );
}
