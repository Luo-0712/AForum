import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Button, Empty, Spin } from 'antd';
import { ArrowRightOutlined, MessageOutlined } from '@ant-design/icons';
import { listPosts } from '../api/posts';
import PostPreviewCard from '../components/PostPreviewCard';

export default function Home() {
  const { data, isLoading } = useQuery({
    queryKey: ['home-posts'],
    queryFn: () => listPosts({ pageNum: 1, pageSize: 5 }),
  });

  const posts = data?.data.records || [];
  const totalPosts = Number(data?.data.total || posts.length);
  const latestComments = posts.reduce((sum, post) => sum + Number(post.commentCount || 0), 0);

  return (
    <div className="forum-page">
      <section className="home-hero">
        <div className="home-hero__copy">
          <span className="eyebrow">AForum</span>
          <h1>面向开发者的 AI 讨论社区</h1>
          <p>把问题、经验和灵感沉淀成可检索的讨论流，让高质量交流更容易被看见。</p>

          <div className="home-hero__actions">
            <Link to="/posts">
              <Button type="primary" size="large" icon={<ArrowRightOutlined />}>
                浏览帖子
              </Button>
            </Link>
            <Link to="/posts/new">
              <Button size="large" icon={<MessageOutlined />}>
                发起讨论
              </Button>
            </Link>
          </div>

          <div className="home-hero__stats">
            <div className="hero-stat">
              <strong>{totalPosts}</strong>
              <span>公开讨论</span>
            </div>
            <div className="hero-stat">
              <strong>{latestComments}</strong>
              <span>近期回复</span>
            </div>
            <div className="hero-stat">
              <strong>AI</strong>
              <span>内容审核</span>
            </div>
          </div>
        </div>

        <div className="hero-visual" aria-hidden="true">
          <div className="hero-visual__grid" />
          <div className="hero-visual__tile" />
          <div className="hero-visual__tile" />
          <div className="hero-visual__tile" />
          <div className="hero-visual__slot">Visual Slot</div>
        </div>
      </section>

      <section className="section-card latest-section">
        <div className="section-heading">
          <div>
            <span className="eyebrow">Latest</span>
            <h2>最新讨论</h2>
            <p>社区里刚刚浮上来的问题与观点。</p>
          </div>
          <Link to="/posts">
            <Button type="text">查看全部</Button>
          </Link>
        </div>

        {isLoading ? (
          <div className="center-panel">
            <Spin />
          </div>
        ) : posts.length > 0 ? (
          <div className="post-list">
            {posts.map((post) => (
              <PostPreviewCard key={post.id} post={post} compact />
            ))}
          </div>
        ) : (
          <Empty description="暂无帖子" />
        )}
      </section>
    </div>
  );
}
