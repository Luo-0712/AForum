import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Button, Card, List, Space, Tag, Typography } from 'antd';
import { ArrowRightOutlined, MessageOutlined } from '@ant-design/icons';
import { listPosts } from '../api/posts';
import heroImage from '../assets/hero.png';

const { Title, Paragraph, Text } = Typography;

export default function Home() {
  const { data, isLoading } = useQuery({
    queryKey: ['home-posts'],
    queryFn: () => listPosts({ pageNum: 1, pageSize: 5 }),
  });

  return (
    <div className="forum-page">
      <div className="home-hero">
        <div>
          <Text className="hero-kicker">AForum</Text>
          <Title level={1}>面向开发者的 AI 论坛</Title>
          <Paragraph>
            先把高质量讨论沉淀下来，再让 AI 做摘要、分流、审核和知识检索。
          </Paragraph>
          <Space>
            <Link to="/posts">
              <Button type="primary" icon={<ArrowRightOutlined />}>浏览帖子</Button>
            </Link>
            <Link to="/posts/new">
              <Button icon={<MessageOutlined />}>发起讨论</Button>
            </Link>
          </Space>
        </div>
        <img src={heroImage} alt="AForum" />
      </div>

      <Card className="section-card" title="最新讨论">
        <List
          loading={isLoading}
          dataSource={data?.data.records || []}
          renderItem={(item) => (
            <List.Item>
              <List.Item.Meta
                title={<Link to={`/posts/${item.id}`}>{item.title}</Link>}
                description={item.summary || '暂无摘要'}
              />
              <Space>
                {item.boardName && <Tag color="processing">{item.boardName}</Tag>}
                <Text type="secondary">{item.commentCount} 条评论</Text>
              </Space>
            </List.Item>
          )}
          locale={{ emptyText: '暂无帖子' }}
        />
      </Card>
    </div>
  );
}
