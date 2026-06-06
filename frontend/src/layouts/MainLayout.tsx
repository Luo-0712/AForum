import { Outlet } from 'react-router-dom';
import { Layout, Menu, Input, Button, Space, Dropdown } from 'antd';
import { UserOutlined, LogoutOutlined, SettingOutlined, PlusOutlined } from '@ant-design/icons';
import { useAuthStore } from '../store/auth';
import { useNavigate } from 'react-router-dom';

const { Header, Content, Footer } = Layout;
const { Search } = Input;

export default function MainLayout() {
  const { user, token, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
      onClick: () => navigate(`/user/${user?.id}`),
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
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <h1 style={{ color: 'white', margin: 0, marginRight: 50, cursor: 'pointer' }} onClick={() => navigate('/')}>
            AForum
          </h1>
          <Menu
            theme="dark"
            mode="horizontal"
            items={[
              { key: 'home', label: '首页', onClick: () => navigate('/') },
              { key: 'posts', label: '帖子', onClick: () => navigate('/posts') },
            ]}
            style={{ flex: 1, minWidth: 0 }}
          />
        </div>
        <Space>
          <Search placeholder="搜索" style={{ width: 200 }} />
          {token && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/posts/new')}>
              发帖
            </Button>
          )}
          {token ? (
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <Button type="text" style={{ color: 'white' }}>
                {user?.nickname || user?.username}
              </Button>
            </Dropdown>
          ) : (
            <Space>
              <Button type="text" style={{ color: 'white' }} onClick={() => navigate('/login')}>
                登录
              </Button>
              <Button type="primary" onClick={() => navigate('/register')}>
                注册
              </Button>
            </Space>
          )}
        </Space>
      </Header>
      <Content style={{ padding: '24px 50px' }}>
        <Outlet />
      </Content>
      <Footer style={{ textAlign: 'center' }}>
        AForum &copy; {new Date().getFullYear()} - 论坛系统
      </Footer>
    </Layout>
  );
}
