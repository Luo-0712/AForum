import { useQuery } from '@tanstack/react-query';
import { Col, Row, Spin, Statistic, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  AppstoreOutlined,
  FileDoneOutlined,
  FileTextOutlined,
  MessageOutlined,
  TagsOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { getAdminOverview } from '../../api/adminManagement';
import { getAdminTrends, type AdminTrendPoint } from '../../api/adminManagement';

function toNumber(value: unknown) {
  return Number(value ?? 0);
}

export default function AdminDashboard() {
  const overviewQuery = useQuery({
    queryKey: ['admin-management', 'overview'],
    queryFn: getAdminOverview,
  });
  const trendsQuery = useQuery({
    queryKey: ['admin-management', 'trends', 7],
    queryFn: () => getAdminTrends(7),
  });
  const data = overviewQuery.data?.data;
  const trends = trendsQuery.data?.data ?? [];
  const trendColumns: ColumnsType<AdminTrendPoint> = [
    {
      title: 'Date',
      dataIndex: 'date',
      key: 'date',
    },
    {
      title: 'Users',
      dataIndex: 'userCount',
      key: 'userCount',
      render: toNumber,
    },
    {
      title: 'Posts',
      dataIndex: 'postCount',
      key: 'postCount',
      render: toNumber,
    },
    {
      title: 'Comments',
      dataIndex: 'commentCount',
      key: 'commentCount',
      render: toNumber,
    },
  ];

  return (
    <div>
      <div className="page-heading">
        <div>
          <span className="eyebrow">Overview</span>
          <h1>Admin Dashboard</h1>
          <p>Live platform counters for moderation, users, and content structure.</p>
        </div>
      </div>

      {overviewQuery.isLoading ? (
        <section className="section-card center-panel admin-loading-panel">
          <Spin />
        </section>
      ) : (
        <>
          <Row className="dashboard-grid" gutter={[16, 16]}>
            <Col xs={24} md={8}>
              <div className="dashboard-card section-card">
                <Statistic title="Users" value={toNumber(data?.userCount)} prefix={<UserOutlined />} />
              </div>
            </Col>
            <Col xs={24} md={8}>
              <div className="dashboard-card section-card">
                <Statistic title="Posts" value={toNumber(data?.postCount)} prefix={<FileTextOutlined />} />
              </div>
            </Col>
            <Col xs={24} md={8}>
              <div className="dashboard-card section-card">
                <Statistic title="Comments" value={toNumber(data?.commentCount)} prefix={<MessageOutlined />} />
              </div>
            </Col>
          </Row>

          <Row className="dashboard-grid" gutter={[16, 16]}>
            <Col xs={24} md={6}>
              <div className="dashboard-card section-card">
                <Statistic title="Pending Review" value={toNumber(data?.pendingPostCount)} prefix={<FileDoneOutlined />} />
              </div>
            </Col>
            <Col xs={24} md={6}>
              <div className="dashboard-card section-card">
                <Statistic title="Published" value={toNumber(data?.publishedPostCount)} prefix={<FileTextOutlined />} />
              </div>
            </Col>
            <Col xs={24} md={6}>
              <div className="dashboard-card section-card">
                <Statistic title="Boards" value={toNumber(data?.boardCount)} prefix={<AppstoreOutlined />} />
              </div>
            </Col>
            <Col xs={24} md={6}>
              <div className="dashboard-card section-card">
                <Statistic title="Categories" value={toNumber(data?.categoryCount)} prefix={<TagsOutlined />} />
              </div>
            </Col>
          </Row>

          <section className="section-card admin-table-card admin-trend-panel">
            <div className="rbac-panel__header">
              <div>
                <h2>7-Day Trend</h2>
                <p>New users, posts, and comments by day.</p>
              </div>
            </div>
            <Table<AdminTrendPoint>
              rowKey={(record) => record.date}
              loading={trendsQuery.isLoading}
              columns={trendColumns}
              dataSource={trends}
              pagination={false}
              size="middle"
            />
          </section>
        </>
      )}
    </div>
  );
}
