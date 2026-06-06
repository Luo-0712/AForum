import { useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Divider, Empty, Form, Input, List, Space, Spin, Tag, Typography, message } from 'antd';
import { EyeOutlined, LikeOutlined, MessageOutlined, SendOutlined } from '@ant-design/icons';
import { createComment, getPost, listComments } from '../api/posts';
import { useAuthStore } from '../store/auth';

const { Title, Paragraph, Text } = Typography;
const { TextArea } = Input;

export default function PostDetail() {
  const { id } = useParams();
  const token = useAuthStore((state) => state.token);
  const queryClient = useQueryClient();
  const [form] = Form.useForm();
  const postId = id || '';

  const postQuery = useQuery({
    queryKey: ['post', postId],
    queryFn: () => getPost(postId),
    enabled: !!postId,
  });

  const commentsQuery = useQuery({
    queryKey: ['comments', postId],
    queryFn: () => listComments(postId),
    enabled: !!postId,
  });

  const commentMutation = useMutation({
    mutationFn: (content: string) => createComment(postId, { content }),
    onSuccess: (res) => {
      form.resetFields();
      if (res.data.status === 1) {
        message.warning('评论已提交，但未通过基础审核');
      } else {
        message.success('评论已发布');
      }
      queryClient.invalidateQueries({ queryKey: ['comments', postId] });
      queryClient.invalidateQueries({ queryKey: ['post', postId] });
    },
    onError: () => message.error('评论失败'),
  });

  if (postQuery.isLoading) {
    return <div className="center-panel"><Spin /></div>;
  }

  const post = postQuery.data?.data;

  if (!post) {
    return (
      <Card>
        <Empty description="帖子不存在或尚未发布" />
      </Card>
    );
  }

  return (
    <div className="forum-page detail-page">
      <Card className="section-card">
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Space wrap>
            {post.boardName && <Tag color="processing">{post.boardName}</Tag>}
            {post.categoryName && <Tag>{post.categoryName}</Tag>}
            {post.suggestedTags?.map((tag) => <Tag color="gold" key={tag}>{tag}</Tag>)}
          </Space>
          <Title level={2}>{post.title}</Title>
          <Text type="secondary">作者：{post.authorName}</Text>
          {post.auditReason && <Alert type="info" showIcon message={post.auditReason} />}
          <Paragraph className="post-content">{post.content}</Paragraph>
          <Space size={8} wrap>
            <Tag icon={<EyeOutlined />}>{post.viewCount || 0}</Tag>
            <Tag icon={<LikeOutlined />}>{post.likeCount || 0}</Tag>
            <Tag icon={<MessageOutlined />}>{post.commentCount || 0}</Tag>
          </Space>
        </Space>
      </Card>

      <Card className="section-card" title="评论">
        {token && (
          <>
            <Form form={form} onFinish={(values) => commentMutation.mutate(values.content)}>
              <Form.Item name="content" rules={[{ required: true, message: '请输入评论内容' }]}>
                <TextArea rows={4} placeholder="写下你的回应" maxLength={5000} showCount />
              </Form.Item>
              <Form.Item>
                <Button type="primary" icon={<SendOutlined />} htmlType="submit" loading={commentMutation.isPending}>
                  发布评论
                </Button>
              </Form.Item>
            </Form>
            <Divider />
          </>
        )}
        <List
          loading={commentsQuery.isLoading}
          dataSource={commentsQuery.data?.data || []}
          renderItem={(item) => (
            <List.Item>
              <List.Item.Meta
                title={item.authorName}
                description={<Paragraph className="comment-content">{item.content}</Paragraph>}
              />
            </List.Item>
          )}
          locale={{ emptyText: '暂无评论' }}
        />
      </Card>
    </div>
  );
}
