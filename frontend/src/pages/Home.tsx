import { Card, List, Typography } from 'antd';

const { Title } = Typography;

export default function Home() {
  return (
    <div>
      <Title level={2}>欢迎来到 AForum</Title>
      <Card title="热门帖子" style={{ marginTop: 24 }}>
        <List
          dataSource={[]}
          renderItem={() => (
            <List.Item>
              <List.Item.Meta title="帖子标题" description="帖子摘要" />
            </List.Item>
          )}
          locale={{ emptyText: '暂无帖子' }}
        />
      </Card>
    </div>
  );
}
