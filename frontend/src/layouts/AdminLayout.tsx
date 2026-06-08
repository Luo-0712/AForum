import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Layout, Menu } from 'antd';
import {
  AppstoreOutlined,
  DashboardOutlined,
  FileTextOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '../store/auth';

const { Header, Sider, Content } = Layout;

export default function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((state) => state.user);
  const roles = user?.roles ?? [];
  const permissions = user?.permissions ?? [];
  const superAdmin = roles.includes('SUPER_ADMIN');
  const can = (permission: string) => superAdmin || permissions.includes(permission);
  const canAny = (items: string[]) => superAdmin || items.some((permission) => permissions.includes(permission));

  const selectedKey = location.pathname.includes('/users')
    ? 'users'
    : location.pathname.includes('/posts')
      ? 'posts'
      : location.pathname.includes('/boards')
        ? 'boards'
        : location.pathname.includes('/access')
          ? 'access'
          : 'dashboard';

  const menuItems = [
    {
      key: 'dashboard',
      icon: <DashboardOutlined />,
      label: 'Dashboard',
      onClick: () => navigate('/admin'),
      visible: can('admin:dashboard') || roles.includes('ADMIN'),
    },
    {
      key: 'users',
      icon: <UserOutlined />,
      label: 'Users',
      onClick: () => navigate('/admin/users'),
      visible: canAny(['user:list', 'user:edit']),
    },
    {
      key: 'posts',
      icon: <FileTextOutlined />,
      label: 'Posts',
      onClick: () => navigate('/admin/posts'),
      visible: canAny(['post:list', 'post:review']),
    },
    {
      key: 'boards',
      icon: <AppstoreOutlined />,
      label: 'Boards',
      onClick: () => navigate('/admin/boards'),
      visible: canAny(['board:list', 'board:edit', 'category:list', 'category:edit']),
    },
    {
      key: 'access',
      icon: <SafetyCertificateOutlined />,
      label: 'Access',
      onClick: () => navigate('/admin/access'),
      visible: canAny(['role:manage', 'permission:manage']),
    },
  ]
    .filter((item) => item.visible)
    .map((item) => ({
      key: item.key,
      icon: item.icon,
      label: item.label,
      onClick: item.onClick,
    }));

  return (
    <Layout className="admin-shell">
      <Sider className="admin-sider" width={238}>
        <div className="admin-brand">
          <button className="brand-mark ghost-button" type="button" onClick={() => navigate('/')}>
            AForum
          </button>
        </div>
        <Menu mode="inline" selectedKeys={[selectedKey]} items={menuItems} />
      </Sider>
      <Layout>
        <Header className="admin-header">
          <div>
            <span className="eyebrow">Admin</span>
            <h1>Management</h1>
          </div>
        </Header>
        <Content className="admin-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
