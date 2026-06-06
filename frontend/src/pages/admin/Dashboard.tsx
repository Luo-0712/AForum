import { Col, Row, Statistic } from 'antd';
import { FileTextOutlined, MessageOutlined, UserOutlined } from '@ant-design/icons';

export default function AdminDashboard() {
  return (
    <div>
      <div className="page-heading">
        <div>
          <span className="eyebrow">Overview</span>
          <h1>仪表盘</h1>
          <p>平台核心数据概览。</p>
        </div>
      </div>

      <Row className="dashboard-grid" gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <div className="dashboard-card">
            <Statistic title="用户数" value={0} prefix={<UserOutlined />} />
          </div>
        </Col>
        <Col xs={24} md={8}>
          <div className="dashboard-card">
            <Statistic title="帖子数" value={0} prefix={<FileTextOutlined />} />
          </div>
        </Col>
        <Col xs={24} md={8}>
          <div className="dashboard-card">
            <Statistic title="评论数" value={0} prefix={<MessageOutlined />} />
          </div>
        </Col>
      </Row>
    </div>
  );
}
