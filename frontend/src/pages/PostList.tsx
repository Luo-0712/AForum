import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Card, Empty, Input, List, Pagination, Space, Spin, Tag, Typography } from 'antd';
import { EyeOutlined, LikeOutlined, MessageOutlined, SearchOutlined } from '@ant-design/icons';
import { listPosts } from '../api/posts';

const { Title, Text } = Typography;
const { Search } = Input;

export default function PostList() {
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');
  const { data, isLoading } = useQuery({
    queryKey: ['posts', page, keyword],
    queryFn: () => listPosts({ pageNum: page, pageSize: 10, keyword }),
  });

  const pageData = data?.data;
  const total = Number(pageData?.total || 0);
  const current = Number(pageData?.current || page);
  const size = Number(pageData?.size || 10);

  return (
    <div className="forum-page">
      <div className="page-heading">
        <div>
          <Title level={2}>帖子</Title>
          <Text type="secondary">最新发布的社区讨论</Text>
        </div>
        <Search
          allowClear
          enterButton={<SearchOutlined />}
          placeholder="搜索标题或正文"
          style={{ maxWidth: 320 }}
          onSearch={(value) => {
            setKeyword(value.trim());
            setPage(1);
          }}
        />
      </div>

      {isLoading ? (
        <div className="center-panel"><Spin /></div>
      ) : (
        <>
          <List
            dataSource={pageData?.records || []}
            renderItem={(item) => (
              <Card className="post-card">
                <Space direction="vertical" size={10} style={{ width: '100%' }}>
                  <Space size={8} wrap>
                    {item.boardName && <Tag color="processing">{item.boardName}</Tag>}
                    {item.categoryName && <Tag>{item.categoryName}</Tag>}
                    <Text type="secondary">{item.authorName}</Text>
                  </Space>
                  <Link className="post-title" to={`/posts/${item.id}`}>{item.title}</Link>
                  <Text type="secondary">{item.summary || '暂无摘要'}</Text>
                  <Space size={8} wrap>
                    <Tag icon={<EyeOutlined />}>{item.viewCount || 0}</Tag>
                    <Tag icon={<LikeOutlined />}>{item.likeCount || 0}</Tag>
                    <Tag icon={<MessageOutlined />}>{item.commentCount || 0}</Tag>
                  </Space>
                </Space>
              </Card>
            )}
            locale={{ emptyText: <Empty description="暂无帖子" /> }}
          />
          {total > 0 && (
            <Pagination
              align="end"
              current={current}
              pageSize={size}
              total={total}
              onChange={setPage}
              showSizeChanger={false}
            />
          )}
        </>
      )}
    </div>
  );
}
