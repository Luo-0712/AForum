import { Link } from 'react-router-dom';
import type { ReactNode } from 'react';
import { EyeOutlined, LikeOutlined, MessageOutlined, StarOutlined } from '@ant-design/icons';
import type { PostSummary } from '../api/posts';
import ForumStatPill from './ForumStatPill';

interface PostPreviewCardProps {
  post: PostSummary;
  compact?: boolean;
  likePending?: boolean;
  favoritePending?: boolean;
  onLikeToggle?: (post: PostSummary) => void;
  onFavoriteToggle?: (post: PostSummary) => void;
  titleNode?: ReactNode;
  summaryNode?: ReactNode;
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
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export default function PostPreviewCard({
  post,
  compact = false,
  likePending = false,
  favoritePending = false,
  onLikeToggle,
  onFavoriteToggle,
  titleNode,
  summaryNode,
}: PostPreviewCardProps) {
  const href = `/posts/${post.id}`;
  const hasCover = Boolean(post.coverUrl);

  return (
    <article className={`post-preview-card${compact ? ' post-preview-card--compact' : ''}${hasCover ? ' has-media' : ''}`}>
      <div className="post-preview-card__main">
        <div className="post-preview-card__meta">
          {post.boardName && <span className="forum-chip forum-chip--primary">{post.boardName}</span>}
          {post.categoryName && <span className="forum-chip">{post.categoryName}</span>}
          <span>{post.authorName || '匿名用户'}</span>
          <span>{formatDate(post.lastCommentAt || post.createdAt)}</span>
        </div>

        <Link className="post-preview-card__title" to={href}>
          {titleNode || post.title}
        </Link>

        <p className="post-preview-card__summary">
          {summaryNode || post.summary || '暂无摘要，打开帖子查看完整讨论内容。'}
        </p>

        <div className="post-preview-card__footer">
          <ForumStatPill icon={<EyeOutlined />} value={post.viewCount || 0} label="浏览" />
          <ForumStatPill
            icon={<LikeOutlined />}
            value={post.likeCount || 0}
            label="点赞"
            active={post.liked}
            loading={likePending}
            onClick={onLikeToggle ? () => onLikeToggle(post) : undefined}
          />
          <ForumStatPill icon={<MessageOutlined />} value={post.commentCount || 0} label="评论" />
          <ForumStatPill
            icon={<StarOutlined />}
            value={post.favoriteCount || 0}
            label="收藏"
            active={post.favorited}
            loading={favoritePending}
            onClick={onFavoriteToggle ? () => onFavoriteToggle(post) : undefined}
          />
        </div>
      </div>

      {post.coverUrl && (
        <Link className="post-preview-media" to={href} aria-label={`查看帖子：${post.title}`}>
          <img
            src={post.coverUrl}
            alt=""
            loading="lazy"
            onError={(event) => {
              event.currentTarget.style.display = 'none';
              event.currentTarget.parentElement?.classList.add('is-empty');
            }}
          />
        </Link>
      )}
    </article>
  );
}
