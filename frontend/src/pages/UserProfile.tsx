import { useParams } from 'react-router-dom';
import { Card, Avatar, Typography, Descriptions } from 'antd';
import { UserOutlined } from '@ant-design/icons';

const { Title } = Typography;

export default function UserProfile() {
  const { id } = useParams();

  return (
    <Card>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 24 }}>
        <Avatar size={64} icon={<UserOutlined />} />
        <Title level={3} style={{ marginLeft: 16, marginBottom: 0 }}>用户中心</Title>
      </div>
      <Descriptions title="用户信息" bordered column={1}>
        <Descriptions.Item label="用户ID">{id}</Descriptions.Item>
        <Descriptions.Item label="用户名">加载中...</Descriptions.Item>
        <Descriptions.Item label="邮箱">加载中...</Descriptions.Item>
      </Descriptions>
    </Card>
  );
}
