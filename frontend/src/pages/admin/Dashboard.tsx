import { Card, Col, Row, Statistic, Typography } from 'antd';
import { UserOutlined, FileTextOutlined, MessageOutlined } from '@ant-design/icons';

const { Title } = Typography;

export default function AdminDashboard() {
  return (
    <div>
      <Title level={3}>仪表盘</Title>
      <Row gutter={16} style={{ marginTop: 24 }}>
        <Col span={8}>
          <Card>
            <Statistic title="用户数" value={0} prefix={<UserOutlined />} />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic title="帖子数" value={0} prefix={<FileTextOutlined />} />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic title="评论数" value={0} prefix={<MessageOutlined />} />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
