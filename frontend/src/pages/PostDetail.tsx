import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Avatar, Button, Empty, Form, Input, Spin, Typography, message } from 'antd';
import { EyeOutlined, LikeOutlined, MessageOutlined, SendOutlined, UserOutlined } from '@ant-design/icons';
import { createComment, getPost, listComments } from '../api/posts';
import ForumStatPill from '../components/ForumStatPill';
import { useAuthStore } from '../store/auth';

const { Paragraph, Title } = Typography;
const { TextArea } = Input;

interface CommentForm {
  content: string;
}

function formatDate(value?: string) {
  if (!value) {
    return '刚刚';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export default function PostDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const token = useAuthStore((state) => state.token);
  const queryClient = useQueryClient();
  const [form] = Form.useForm<CommentForm>();
  const postId = id || '';

  const postQuery = useQuery({
    queryKey: ['post', postId],
    queryFn: () => getPost(postId),
    enabled: Boolean(postId),
  });

  const commentsQuery = useQuery({
    queryKey: ['comments', postId],
    queryFn: () => listComments(postId),
    enabled: Boolean(postId),
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
    return (
      <div className="center-panel">
        <Spin />
      </div>
    );
  }

  const post = postQuery.data?.data;

  if (!post) {
    return (
      <div className="forum-page">
        <div className="section-card center-panel">
          <Empty description="帖子不存在或尚未发布" />
        </div>
      </div>
    );
  }

  const comments = commentsQuery.data?.data || [];

  return (
    <div className="forum-page detail-page">
      <article className="section-card post-detail-card">
        <div className="post-detail__meta">
          {post.boardName && <span className="forum-chip forum-chip--primary">{post.boardName}</span>}
          {post.categoryName && <span className="forum-chip">{post.categoryName}</span>}
          {post.suggestedTags?.map((tag) => (
            <span className="forum-chip forum-chip--warning" key={tag}>
              {tag}
            </span>
          ))}
          <span>{formatDate(post.createdAt)}</span>
        </div>

        <Title className="post-detail__title" level={1}>
          {post.title}
        </Title>

        <div className="post-detail__author">
          <Avatar size={34} icon={<UserOutlined />} />
          <span>作者：{post.authorName || '匿名用户'}</span>
        </div>

        {post.coverUrl && (
          <div className="post-cover">
            <img src={post.coverUrl} alt="" loading="lazy" />
          </div>
        )}

        {post.auditReason && <Alert className="detail-alert" type="info" showIcon message={post.auditReason} />}

        <Paragraph className="post-content">{post.content}</Paragraph>

        <div className="post-preview-card__footer">
          <ForumStatPill icon={<EyeOutlined />} value={post.viewCount || 0} label="浏览" />
          <ForumStatPill icon={<LikeOutlined />} value={post.likeCount || 0} label="点赞" />
          <ForumStatPill icon={<MessageOutlined />} value={post.commentCount || 0} label="评论" />
        </div>
      </article>

      <section className="section-card comments-card">
        <div className="section-heading">
          <div>
            <span className="eyebrow">Comments</span>
            <h2>评论</h2>
            <p>{comments.length} 条回复</p>
          </div>
        </div>

        {token ? (
          <Form
            className="comment-composer"
            form={form}
            layout="vertical"
            onFinish={(values) => commentMutation.mutate(values.content)}
          >
            <Form.Item name="content" rules={[{ required: true, message: '请输入评论内容' }]}>
              <TextArea rows={4} placeholder="写下你的回应" maxLength={5000} showCount />
            </Form.Item>
            <Button type="primary" icon={<SendOutlined />} htmlType="submit" loading={commentMutation.isPending}>
              发布评论
            </Button>
          </Form>
        ) : (
          <div className="comment-composer">
            <p className="comment-login-copy">登录后参与回复。</p>
            <Button type="primary" onClick={() => navigate('/login')}>
              登录
            </Button>
          </div>
        )}

        <div className="comment-list">
          {commentsQuery.isLoading ? (
            <div className="center-panel">
              <Spin />
            </div>
          ) : comments.length > 0 ? (
            comments.map((comment) => (
              <div className="comment-item" key={comment.id}>
                <Avatar icon={<UserOutlined />} />
                <div>
                  <div className="comment-item__header">
                    <span className="comment-item__author">{comment.authorName || '匿名用户'}</span>
                    <span>{formatDate(comment.createdAt)}</span>
                    <ForumStatPill icon={<LikeOutlined />} value={comment.likeCount || 0} label="点赞" />
                  </div>
                  <Paragraph className="comment-content">{comment.content}</Paragraph>
                </div>
              </div>
            ))
          ) : (
            <Empty description="暂无评论" />
          )}
        </div>

        <Link className="back-link" to="/posts">
          返回帖子列表
        </Link>
      </section>
    </div>
  );
}
