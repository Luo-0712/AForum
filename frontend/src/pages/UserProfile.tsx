import { Children } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Avatar, Button, Empty, Spin, Tabs, message } from 'antd';
import { StarOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons';
import {
  favoritePost,
  likePost,
  listPosts,
  unfavoritePost,
  unlikePost,
  type PostSummary,
} from '../api/posts';
import {
  followUser,
  getUser,
  listFollowers,
  listFollowing,
  listUserFavorites,
  unfollowUser,
  type UserProfileInfo,
} from '../api/users';
import PostPreviewCard from '../components/PostPreviewCard';
import { useAuthStore } from '../store/auth';

type PostInteraction = {
  post: PostSummary;
  kind: 'like' | 'favorite';
};

interface UserListProps {
  users: UserProfileInfo[];
  isLoading: boolean;
  currentUserId?: number | string;
  pendingUserId?: string;
  onFollowToggle: (user: UserProfileInfo) => void;
}

function displayName(user?: Pick<UserProfileInfo, 'nickname' | 'username'>) {
  return user?.nickname || user?.username || '匿名用户';
}

function UserRelationList({ users, isLoading, currentUserId, pendingUserId, onFollowToggle }: UserListProps) {
  if (isLoading) {
    return (
      <div className="center-panel profile-tab-loader">
        <Spin />
      </div>
    );
  }

  if (!users.length) {
    return <Empty description="暂无数据" />;
  }

  return (
    <div className="profile-user-list">
      {Children.toArray(
        users.map((user) => {
          const isSelf = String(currentUserId || '') === String(user.id);
          return (
            <div className="profile-user-row">
              <Link className="profile-user-row__identity" to={`/user/${user.id}`}>
                <Avatar size={42} src={user.avatar || undefined} icon={<UserOutlined />} />
                <div>
                  <strong>{displayName(user)}</strong>
                  <span>{user.bio || `用户 ID：${user.id}`}</span>
                </div>
              </Link>
              <div className="profile-user-row__stats">
                <span>{user.postCount || 0} 帖子</span>
                <span>{user.followerCount || 0} 粉丝</span>
              </div>
              {!isSelf && (
                <Button
                  size="small"
                  type={user.followedByCurrentUser ? 'default' : 'primary'}
                  loading={pendingUserId === String(user.id)}
                  onClick={() => onFollowToggle(user)}
                >
                  {user.followedByCurrentUser ? '已关注' : '关注'}
                </Button>
              )}
            </div>
          );
        }),
      )}
    </div>
  );
}

export default function UserProfile() {
  const { id } = useParams();
  const currentUser = useAuthStore((state) => state.user);
  const queryClient = useQueryClient();
  const profileId = id || currentUser?.id;
  const isCurrentUser = Boolean(currentUser && String(currentUser.id) === String(profileId));

  const profileQuery = useQuery({
    queryKey: ['user', profileId],
    queryFn: () => getUser(profileId || ''),
    enabled: Boolean(profileId),
  });

  const postsQuery = useQuery({
    queryKey: ['profile-posts', profileId],
    queryFn: () => listPosts({ pageNum: 1, pageSize: 20, userId: profileId }),
    enabled: Boolean(profileId),
  });

  const favoritesQuery = useQuery({
    queryKey: ['profile-favorites', profileId],
    queryFn: () => listUserFavorites(profileId || '', { pageNum: 1, pageSize: 20 }),
    enabled: Boolean(profileId),
  });

  const followingQuery = useQuery({
    queryKey: ['profile-following', profileId],
    queryFn: () => listFollowing(profileId || '', { pageNum: 1, pageSize: 30 }),
    enabled: Boolean(profileId),
  });

  const followersQuery = useQuery({
    queryKey: ['profile-followers', profileId],
    queryFn: () => listFollowers(profileId || '', { pageNum: 1, pageSize: 30 }),
    enabled: Boolean(profileId),
  });

  const postInteractionMutation = useMutation({
    mutationFn: ({ post, kind }: PostInteraction) => {
      if (kind === 'like') {
        return post.liked ? unlikePost(post.id) : likePost(post.id);
      }
      return post.favorited ? unfavoritePost(post.id) : favoritePost(post.id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile-posts', profileId] });
      queryClient.invalidateQueries({ queryKey: ['profile-favorites', profileId] });
      queryClient.invalidateQueries({ queryKey: ['posts'] });
      queryClient.invalidateQueries({ queryKey: ['user', profileId] });
    },
    onError: () => message.error('互动失败，请稍后再试'),
  });

  const followMutation = useMutation({
    mutationFn: (user: UserProfileInfo) => (user.followedByCurrentUser ? unfollowUser(user.id) : followUser(user.id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user'] });
      queryClient.invalidateQueries({ queryKey: ['profile-following'] });
      queryClient.invalidateQueries({ queryKey: ['profile-followers'] });
    },
    onError: () => message.error('关注失败，请稍后再试'),
  });

  const profile = profileQuery.data?.data;
  const posts = postsQuery.data?.data.records || [];
  const favorites = favoritesQuery.data?.data.records || [];
  const following = followingQuery.data?.data.records || [];
  const followers = followersQuery.data?.data.records || [];
  const pendingPost = postInteractionMutation.variables;
  const pendingPostKey = postInteractionMutation.isPending && pendingPost
    ? `${pendingPost.kind}:${pendingPost.post.id}`
    : '';
  const pendingUser = followMutation.variables;
  const pendingUserId = followMutation.isPending && pendingUser ? String(pendingUser.id) : '';

  const handlePostInteraction = (post: PostSummary, kind: PostInteraction['kind']) => {
    postInteractionMutation.mutate({ post, kind });
  };

  const renderPostList = (items: PostSummary[], loading: boolean, emptyText: string) => {
    if (loading) {
      return (
        <div className="center-panel profile-tab-loader">
          <Spin />
        </div>
      );
    }
    if (!items.length) {
      return <Empty description={emptyText} />;
    }
    return (
      <div className="post-list profile-post-list">
        {Children.toArray(
          items.map((post) => (
            <PostPreviewCard
              post={post}
              compact
              likePending={pendingPostKey === `like:${post.id}`}
              favoritePending={pendingPostKey === `favorite:${post.id}`}
              onLikeToggle={(item) => handlePostInteraction(item, 'like')}
              onFavoriteToggle={(item) => handlePostInteraction(item, 'favorite')}
            />
          )),
        )}
      </div>
    );
  };

  if (profileQuery.isLoading) {
    return (
      <div className="center-panel">
        <Spin />
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="forum-page">
        <div className="section-card center-panel">
          <Empty description="用户不存在" />
        </div>
      </div>
    );
  }

  return (
    <div className="forum-page">
      <section className="profile-card">
        <div className="profile-hero">
          <Avatar size={72} src={profile.avatar || undefined} icon={<UserOutlined />} />
          <div className="profile-hero__copy">
            <span className="eyebrow">Profile</span>
            <h1>{displayName(profile)}</h1>
            <p>{profile.bio || `用户 ID：${profile.id}`}</p>
          </div>
          {!isCurrentUser && (
            <Button
              type={profile.followedByCurrentUser ? 'default' : 'primary'}
              loading={pendingUserId === String(profile.id)}
              onClick={() => followMutation.mutate(profile)}
            >
              {profile.followedByCurrentUser ? '已关注' : '关注'}
            </Button>
          )}
        </div>

        <div className="profile-body">
          <div className="profile-info-grid">
            <div>
              <span>用户名</span>
              <strong>{profile.username}</strong>
            </div>
            <div>
              <span>账号类型</span>
              <strong>{profile.userType >= 2 ? '管理员' : '普通用户'}</strong>
            </div>
            <div>
              <span>帖子</span>
              <strong>{profile.postCount || 0}</strong>
            </div>
            <div>
              <span>收藏</span>
              <strong>{profile.favoriteCount || 0}</strong>
            </div>
            <div>
              <span>关注</span>
              <strong>{profile.followingCount || 0}</strong>
            </div>
            <div>
              <span>粉丝</span>
              <strong>{profile.followerCount || 0}</strong>
            </div>
          </div>

          <Tabs
            className="profile-tabs"
            items={[
              {
                key: 'posts',
                label: '帖子',
                children: renderPostList(posts, postsQuery.isLoading, '暂无帖子'),
              },
              {
                key: 'favorites',
                label: (
                  <span>
                    <StarOutlined /> 收藏
                  </span>
                ),
                children: renderPostList(favorites, favoritesQuery.isLoading, '暂无收藏'),
              },
              {
                key: 'following',
                label: (
                  <span>
                    <TeamOutlined /> 关注
                  </span>
                ),
                children: (
                  <UserRelationList
                    users={following}
                    isLoading={followingQuery.isLoading}
                    currentUserId={currentUser?.id}
                    pendingUserId={pendingUserId}
                    onFollowToggle={(user) => followMutation.mutate(user)}
                  />
                ),
              },
              {
                key: 'followers',
                label: '粉丝',
                children: (
                  <UserRelationList
                    users={followers}
                    isLoading={followersQuery.isLoading}
                    currentUserId={currentUser?.id}
                    pendingUserId={pendingUserId}
                    onFollowToggle={(user) => followMutation.mutate(user)}
                  />
                ),
              },
            ]}
          />
        </div>
      </section>
    </div>
  );
}
