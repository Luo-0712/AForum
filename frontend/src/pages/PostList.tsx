import { Card, List, Tag, Typography } from 'antd';
import { EyeOutlined, LikeOutlined, MessageOutlined } from '@ant-design/icons';

const { Title } = Typography;

export default function PostList() {
  return (
    <div>
      <Title level={3}>帖子列表</Title>
      <List
        dataSource={[]}
        renderItem={(item: any) => (
          <Card style={{ marginBottom: 16 }}>
            <Card.Meta
              title={<a>{item.title}</a>}
              description={item.summary}
            />
            <div style={{ marginTop: 16 }}>
              <Tag icon={<EyeOutlined />}>{item.viewCount || 0}</Tag>
              <Tag icon={<LikeOutlined />}>{item.likeCount || 0}</Tag>
              <Tag icon={<MessageOutlined />}>{item.commentCount || 0}</Tag>
            </div>
          </Card>
        )}
        locale={{ emptyText: '暂无帖子' }}
      />
    </div>
  );
}
