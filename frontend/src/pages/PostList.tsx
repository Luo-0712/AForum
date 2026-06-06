import { useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Empty, Input, Pagination, Spin } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { listPosts } from '../api/posts';
import PostPreviewCard from '../components/PostPreviewCard';

const { Search } = Input;

export default function PostList() {
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = searchParams.get('keyword') || '';
  const pageParam = Number(searchParams.get('page') || 1);
  const page = Number.isFinite(pageParam) && pageParam > 0 ? pageParam : 1;

  const { data, isLoading } = useQuery({
    queryKey: ['posts', page, keyword],
    queryFn: () => listPosts({ pageNum: page, pageSize: 10, keyword }),
  });

  const pageData = data?.data;
  const total = Number(pageData?.total || 0);
  const current = Number(pageData?.current || page);
  const size = Number(pageData?.size || 10);
  const posts = pageData?.records || [];

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
            key={keyword}
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
            {posts.map((post) => (
              <PostPreviewCard key={post.id} post={post} />
            ))}
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
