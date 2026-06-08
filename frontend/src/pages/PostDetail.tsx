import { Children } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Avatar, Button, Empty, Form, Input, Spin, Typography, message } from 'antd';
import { EyeOutlined, LikeOutlined, MailOutlined, MessageOutlined, SendOutlined, StarOutlined, UserOutlined } from '@ant-design/icons';
import {
  createComment,
  favoritePost,
  getPost,
  likeComment,
  likePost,
  listComments,
  unfavoritePost,
  unlikeComment,
  unlikePost,
  type CommentItem,
} from '../api/posts';
import { followUser, getUser, unfollowUser } from '../api/users';
import ForumStatPill from '../components/ForumStatPill';
import { useAuthStore } from '../store/auth';

const { Paragraph, Title } = Typography;
const { TextArea } = Input;

interface CommentForm {
  content: string;
}

type PostAction = 'like' | 'favorite';

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
  const currentUser = useAuthStore((state) => state.user);
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

  const post = postQuery.data?.data;

  const authorQuery = useQuery({
    queryKey: ['user', post?.userId],
    queryFn: () => getUser(post?.userId || ''),
    enabled: Boolean(token && post?.userId),
  });

  const requireLogin = () => {
    if (token) {
      return true;
    }
    message.warning('登录后参与互动');
    navigate('/login');
    return false;
  };

  const postInteractionMutation = useMutation({
    mutationFn: (kind: PostAction) => {
      const currentPost = postQuery.data?.data;
      if (!currentPost) {
        throw new Error('Post is not loaded');
      }
      if (kind === 'like') {
        return currentPost.liked ? unlikePost(currentPost.id) : likePost(currentPost.id);
      }
      return currentPost.favorited ? unfavoritePost(currentPost.id) : favoritePost(currentPost.id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['post', postId] });
      queryClient.invalidateQueries({ queryKey: ['posts'] });
    },
    onError: () => message.error('互动失败，请稍后再试'),
  });

  const commentLikeMutation = useMutation({
    mutationFn: (comment: CommentItem) => (comment.liked ? unlikeComment(comment.id) : likeComment(comment.id)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['comments', postId] }),
    onError: () => message.error('评论点赞失败'),
  });

  const followMutation = useMutation({
    mutationFn: () => {
      const author = authorQuery.data?.data;
      const authorId = author?.id || post?.userId;
      if (!authorId) {
        throw new Error('Author is not loaded');
      }
      return author?.followedByCurrentUser ? unfollowUser(authorId) : followUser(authorId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user', post?.userId] });
      queryClient.invalidateQueries({ queryKey: ['profile-following'] });
      queryClient.invalidateQueries({ queryKey: ['profile-followers'] });
    },
    onError: () => message.error('关注失败，请稍后再试'),
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

  const handlePostInteraction = (kind: PostAction) => {
    if (!requireLogin()) {
      return;
    }
    postInteractionMutation.mutate(kind);
  };

  const handleCommentLike = (comment: CommentItem) => {
    if (!requireLogin()) {
      return;
    }
    commentLikeMutation.mutate(comment);
  };

  const handleFollow = () => {
    if (!requireLogin()) {
      return;
    }
    followMutation.mutate();
  };

  if (postQuery.isLoading) {
    return (
      <div className="center-panel">
        <Spin />
      </div>
    );
  }

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
  const author = authorQuery.data?.data;
  const canFollowAuthor = Boolean(token && currentUser && String(currentUser.id) !== String(post.userId));

  return (
    <div className="forum-page detail-page">
      <article className="section-card post-detail-card">
        <div className="post-detail__meta">
          {post.boardName && <span className="forum-chip forum-chip--primary">{post.boardName}</span>}
          {post.categoryName && <span className="forum-chip">{post.categoryName}</span>}
          {Children.toArray(
            (post.suggestedTags || []).map((tag) => (
              <span className="forum-chip forum-chip--warning">
                {tag}
              </span>
            )),
          )}
          <span>{formatDate(post.createdAt)}</span>
        </div>

        <Title className="post-detail__title" level={1}>
          {post.title}
        </Title>

        <div className="post-detail__author">
          <Link className="post-detail__author-main" to={`/user/${post.userId}`}>
            <Avatar size={34} src={author?.avatar || undefined} icon={<UserOutlined />} />
            <span>作者：{post.authorName || '匿名用户'}</span>
          </Link>
          {canFollowAuthor && (
            <div className="post-detail__author-actions">
              <Button size="small" icon={<MailOutlined />} onClick={() => navigate(`/messages/${post.userId}`)}>
                私信
              </Button>
              <Button
                size="small"
                type={author?.followedByCurrentUser ? 'default' : 'primary'}
                loading={followMutation.isPending || authorQuery.isLoading}
                onClick={handleFollow}
              >
                {author?.followedByCurrentUser ? '已关注' : '关注作者'}
              </Button>
            </div>
          )}
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
          <ForumStatPill
            icon={<LikeOutlined />}
            value={post.likeCount || 0}
            label="点赞"
            active={post.liked}
            loading={postInteractionMutation.isPending && postInteractionMutation.variables === 'like'}
            onClick={() => handlePostInteraction('like')}
          />
          <ForumStatPill icon={<MessageOutlined />} value={post.commentCount || 0} label="评论" />
          <ForumStatPill
            icon={<StarOutlined />}
            value={post.favoriteCount || 0}
            label="收藏"
            active={post.favorited}
            loading={postInteractionMutation.isPending && postInteractionMutation.variables === 'favorite'}
            onClick={() => handlePostInteraction('favorite')}
          />
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
            Children.toArray(
              comments.map((comment) => (
                <div className="comment-item">
                  <Avatar icon={<UserOutlined />} />
                  <div>
                    <div className="comment-item__header">
                      <span className="comment-item__author">{comment.authorName || '匿名用户'}</span>
                      <span>{formatDate(comment.createdAt)}</span>
                      <ForumStatPill
                        icon={<LikeOutlined />}
                        value={comment.likeCount || 0}
                        label="点赞"
                        active={comment.liked}
                        loading={
                          commentLikeMutation.isPending &&
                          String(commentLikeMutation.variables?.id) === String(comment.id)
                        }
                        onClick={() => handleCommentLike(comment)}
                      />
                    </div>
                    <Paragraph className="comment-content">{comment.content}</Paragraph>
                  </div>
                </div>
              )),
            )
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
