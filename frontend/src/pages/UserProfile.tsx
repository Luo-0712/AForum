import { useParams } from 'react-router-dom';
import { Avatar, Empty, Tabs } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import { useAuthStore } from '../store/auth';

export default function UserProfile() {
  const { id } = useParams();
  const user = useAuthStore((state) => state.user);
  const isCurrentUser = Boolean(user && String(user.id) === String(id));
  const displayName = isCurrentUser ? user?.nickname || user?.username : '用户中心';
  const avatar = isCurrentUser ? user?.avatar : undefined;
  const userType = user?.userType ?? 0;

  return (
    <div className="forum-page">
      <section className="profile-card">
        <div className="profile-hero">
          <Avatar size={72} src={avatar || undefined} icon={<UserOutlined />} />
          <div>
            <span className="eyebrow">Profile</span>
            <h1>{displayName}</h1>
            <p>用户 ID：{id}</p>
          </div>
        </div>

        <div className="profile-body">
          <div className="profile-info-grid">
            <div>
              <span>用户名</span>
              <strong>{isCurrentUser ? user?.username : '加载中'}</strong>
            </div>
            <div>
              <span>昵称</span>
              <strong>{isCurrentUser ? user?.nickname || '未设置' : '加载中'}</strong>
            </div>
            <div>
              <span>账号类型</span>
              <strong>{isCurrentUser && userType >= 2 ? '管理员' : '普通用户'}</strong>
            </div>
          </div>

          <Tabs
            className="profile-tabs"
            items={[
              {
                key: 'posts',
                label: '我的帖子',
                children: <Empty description="帖子列表待接入" />,
              },
              {
                key: 'favorites',
                label: '收藏',
                children: <Empty description="收藏列表待接入" />,
              },
              {
                key: 'follows',
                label: '关注',
                children: <Empty description="关注列表待接入" />,
              },
            ]}
          />
        </div>
      </section>
    </div>
  );
}
