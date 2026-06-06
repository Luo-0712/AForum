import { Children } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Empty, Input, Pagination, Spin, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { favoritePost, likePost, listPosts, unfavoritePost, unlikePost, type PostSummary } from '../api/posts';
import PostPreviewCard from '../components/PostPreviewCard';
import { useAuthStore } from '../store/auth';

const { Search } = Input;

type PostInteraction = {
  post: PostSummary;
  kind: 'like' | 'favorite';
};

export default function PostList() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = useAuthStore((state) => state.token);
  const queryClient = useQueryClient();
  const keyword = searchParams.get('keyword') || '';
  const pageParam = Number(searchParams.get('page') || 1);
  const page = Number.isFinite(pageParam) && pageParam > 0 ? pageParam : 1;

  const { data, isLoading } = useQuery({
    queryKey: ['posts', page, keyword],
    queryFn: () => listPosts({ pageNum: page, pageSize: 10, keyword }),
  });

  const interactionMutation = useMutation({
    mutationFn: ({ post, kind }: PostInteraction) => {
      if (kind === 'like') {
        return post.liked ? unlikePost(post.id) : likePost(post.id);
      }
      return post.favorited ? unfavoritePost(post.id) : favoritePost(post.id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['posts'] });
    },
    onError: () => message.error('互动失败，请稍后再试'),
  });

  const pageData = data?.data;
  const total = Number(pageData?.total || 0);
  const current = Number(pageData?.current || page);
  const size = Number(pageData?.size || 10);
  const posts = pageData?.records || [];
  const pendingPost = interactionMutation.variables;
  const pendingKey = interactionMutation.isPending && pendingPost
    ? `${pendingPost.kind}:${pendingPost.post.id}`
    : '';

  const handleSearch = (value: string) => {
    const nextKeyword = value.trim();
    setSearchParams(nextKeyword ? { keyword: nextKeyword } : {});
  };

  const handlePageChange = (nextPage: number) => {
    const nextParams: Record<string, string> = {};
    if (keyword) {
      nextParams.keyword = keyword;
    }
    if (nextPage > 1) {
      nextParams.page = String(nextPage);
    }
    setSearchParams(nextParams);
  };

  const handleInteraction = (post: PostSummary, kind: PostInteraction['kind']) => {
    if (!token) {
      message.warning('登录后参与互动');
      navigate('/login');
      return;
    }
    interactionMutation.mutate({ post, kind });
  };

  return (
    <div className="forum-page">
      <div className="page-heading">
        <div>
          <span className="eyebrow">Posts</span>
          <h1>帖子</h1>
          <p>最新发布的社区讨论。</p>
        </div>
        <div className="page-toolbar">
          <Search
            allowClear
            enterButton={<SearchOutlined />}
            placeholder="搜索标题或正文"
            defaultValue={keyword}
            onSearch={handleSearch}
          />
        </div>
      </div>

      {isLoading ? (
        <div className="center-panel">
          <Spin />
        </div>
      ) : posts.length > 0 ? (
        <>
          <div className="post-list">
            {Children.toArray(
              posts.map((post) => (
                <PostPreviewCard
                  post={post}
                  likePending={pendingKey === `like:${post.id}`}
                  favoritePending={pendingKey === `favorite:${post.id}`}
                  onLikeToggle={(item) => handleInteraction(item, 'like')}
                  onFavoriteToggle={(item) => handleInteraction(item, 'favorite')}
                />
              )),
            )}
          </div>
          {total > 0 && (
            <Pagination
              align="end"
              current={current}
              pageSize={size}
              total={total}
              onChange={handlePageChange}
              showSizeChanger={false}
              className="post-pagination"
            />
          )}
        </>
      ) : (
        <div className="section-card center-panel">
          <Empty description="暂无帖子" />
        </div>
      )}
    </div>
  );
}
