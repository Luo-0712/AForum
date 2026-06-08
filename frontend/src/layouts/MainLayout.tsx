import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Button, Dropdown, Input, Layout, Menu } from 'antd';
import { LogoutOutlined, PlusOutlined, SettingOutlined, UserOutlined } from '@ant-design/icons';
import { useAuthStore } from '../store/auth';

const { Header, Content, Footer } = Layout;
const { Search } = Input;

export default function MainLayout() {
  const { user, token, logout } = useAuthStore();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

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
              <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
                <Button icon={<UserOutlined />}>{displayName}</Button>
              </Dropdown>
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
