import { Children } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Empty, Input, Pagination, Segmented, Select, Spin, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { favoritePost, likePost, listPosts, unfavoritePost, unlikePost, type PostSummary } from '../api/posts';
import { getSearchOptions, searchPosts, type SearchPostSummary } from '../api/search';
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
  const sortParam = searchParams.get('sort') || 'relevance';
  const sort = ['relevance', 'latest', 'hot'].includes(sortParam) ? sortParam as 'relevance' | 'latest' | 'hot' : 'relevance';
  const categoryId = searchParams.get('categoryId') || undefined;
  const boardId = searchParams.get('boardId') || undefined;
  const pageParam = Number(searchParams.get('page') || 1);
  const page = Number.isFinite(pageParam) && pageParam > 0 ? pageParam : 1;
  const isSearching = Boolean(keyword);

  const { data, isLoading } = useQuery({
    queryKey: ['posts', page, keyword, sort, categoryId, boardId],
    queryFn: () => isSearching
      ? searchPosts({ q: keyword, pageNum: page, pageSize: 10, sort, categoryId, boardId, highlight: true })
      : listPosts({ pageNum: page, pageSize: 10, categoryId, boardId }),
  });

  const { data: searchOptions } = useQuery({
    queryKey: ['search-options'],
    queryFn: getSearchOptions,
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
    setSearchParams(buildParams({ keyword: nextKeyword || undefined, sort: nextKeyword ? sort : undefined, categoryId, boardId }));
  };

  const handlePageChange = (nextPage: number) => {
    setSearchParams(buildParams({ keyword, sort, categoryId, boardId, page: nextPage }));
  };

  const handleSortChange = (value: string | number) => {
    setSearchParams(buildParams({ keyword, sort: String(value), categoryId, boardId }));
  };

  const handleFilterChange = (key: 'categoryId' | 'boardId', value?: string) => {
    setSearchParams(buildParams({
      keyword,
      sort,
      categoryId: key === 'categoryId' ? value : categoryId,
      boardId: key === 'boardId' ? value : boardId,
    }));
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
          <p>{isSearching ? `搜索「${keyword}」共找到 ${total} 条讨论。` : '最新发布的社区讨论。'}</p>
        </div>
        <div className="page-toolbar">
          <Search
            allowClear
            enterButton={<SearchOutlined />}
            placeholder="搜索标题或正文"
            defaultValue={keyword}
            onSearch={handleSearch}
          />
          {isSearching && (
            <Segmented
              value={sort}
              onChange={handleSortChange}
              options={[
                { label: '综合', value: 'relevance' },
                { label: '最新', value: 'latest' },
                { label: '热门', value: 'hot' },
              ]}
            />
          )}
          <Select
            allowClear
            className="search-filter-select"
            placeholder="分类"
            value={categoryId}
            onChange={(value) => handleFilterChange('categoryId', value)}
            options={(searchOptions?.data.categories || []).map((item) => ({
              label: item.name,
              value: String(item.id),
            }))}
          />
          <Select
            allowClear
            className="search-filter-select"
            placeholder="板块"
            value={boardId}
            onChange={(value) => handleFilterChange('boardId', value)}
            options={(searchOptions?.data.boards || []).map((item) => ({
              label: item.name,
              value: String(item.id),
            }))}
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
                  titleNode={renderHighlight(post as SearchPostSummary, 'title')}
                  summaryNode={renderHighlight(post as SearchPostSummary, 'summary')
                    || renderHighlight(post as SearchPostSummary, 'content')}
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
          <Empty description={isSearching ? '没有找到匹配的帖子' : '暂无帖子'} />
        </div>
      )}
    </div>
  );
}

function buildParams({
  keyword,
  sort,
  categoryId,
  boardId,
  page,
}: {
  keyword?: string;
  sort?: string;
  categoryId?: string;
  boardId?: string;
  page?: number;
}) {
  const nextParams: Record<string, string> = {};
  if (keyword) {
    nextParams.keyword = keyword;
  }
  if (keyword && sort && sort !== 'relevance') {
    nextParams.sort = sort;
  }
  if (categoryId) {
    nextParams.categoryId = categoryId;
  }
  if (boardId) {
    nextParams.boardId = boardId;
  }
  if (page && page > 1) {
    nextParams.page = String(page);
  }
  return nextParams;
}

function renderHighlight(post: SearchPostSummary, field: keyof NonNullable<SearchPostSummary['highlights']>) {
  const value = post.highlights?.[field]?.[0];
  if (!value) {
    return null;
  }
  return parseMarkFragments(value);
}

function parseMarkFragments(value: string) {
  const parts = value.split(/(<mark>|<\/mark>)/g);
  let marked = false;
  return parts
    .filter((part) => part !== '')
    .map((part, index) => {
      if (part === '<mark>') {
        marked = true;
        return null;
      }
      if (part === '</mark>') {
        marked = false;
        return null;
      }
      return marked ? <mark key={`${part}-${index}`}>{part}</mark> : <span key={`${part}-${index}`}>{part}</span>;
    });
}
