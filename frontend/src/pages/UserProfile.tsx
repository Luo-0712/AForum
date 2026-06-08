import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Avatar, Button, Empty, Form, Input, Modal, Spin, Tabs, message } from 'antd';
import { EditOutlined, MailOutlined, StarOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons';
import {
  favoritePost,
  likePost,
  listPosts,
  unfavoritePost,
  unlikePost,
  type Numeric,
  type PostSummary,
} from '../api/posts';
import {
  followUser,
  getCurrentUser,
  getUser,
  listFollowers,
  listFollowing,
  listUserFavorites,
  unfollowUser,
  updateCurrentUserProfile,
  type UpdateProfilePayload,
  type UserProfileInfo,
} from '../api/users';
import PostPreviewCard from '../components/PostPreviewCard';
import { useAuthStore, type UserInfo } from '../store/auth';

type PostInteraction = {
  post: PostSummary;
  kind: 'like' | 'favorite';
};

type ProfileFormValues = Required<UpdateProfilePayload>;

interface UserListProps {
  users: UserProfileInfo[];
  isLoading: boolean;
  currentUserId?: Numeric;
  pendingUserId?: string;
  onFollowToggle: (user: UserProfileInfo) => void;
}

function displayName(user?: Pick<UserProfileInfo, 'nickname' | 'username'>) {
  return user?.nickname || user?.username || '匿名用户';
}

function cleanProfileValue(value?: string | null) {
  return value?.trim() || '';
}

function getOptionalNumber(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return undefined;
  }

  const nextValue = Number(value);
  return Number.isFinite(nextValue) ? nextValue : undefined;
}

function getCount(primary: unknown, fallback: unknown) {
  return getOptionalNumber(primary) ?? getOptionalNumber(fallback) ?? 0;
}

function formatDate(value?: string) {
  if (!value) {
    return undefined;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date);
}

function toAuthUser(profile: UserProfileInfo): UserInfo | null {
  const id = getOptionalNumber(profile.id);
  if (id === undefined) {
    return null;
  }

  return {
    id,
    username: profile.username,
    nickname: profile.nickname || profile.username,
    avatar: profile.avatar || '',
    userType: profile.userType,
    postCount: profile.postCount ?? undefined,
    favoriteCount: profile.favoriteCount ?? undefined,
    followingCount: profile.followingCount ?? undefined,
    followerCount: profile.followerCount ?? undefined,
    followedByCurrentUser: Boolean(profile.followedByCurrentUser),
    roles: profile.roles,
    permissions: profile.permissions,
  };
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
      {users.map((user) => {
        const isSelf = String(currentUserId || '') === String(user.id);
        return (
          <div className="profile-user-row" key={String(user.id)}>
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
      })}
    </div>
  );
}

export default function UserProfile() {
  const { id: routeUserId } = useParams();
  const navigate = useNavigate();
  const [profileForm] = Form.useForm<ProfileFormValues>();
  const [isProfileEditorOpen, setIsProfileEditorOpen] = useState(false);
  const token = useAuthStore((state) => state.token);
  const currentUser = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);
  const queryClient = useQueryClient();

  const currentUserQuery = useQuery({
    queryKey: ['user', 'me'],
    queryFn: getCurrentUser,
    enabled: Boolean(token),
  });

  const currentUserProfile = currentUserQuery.data?.data;
  const currentUserId = currentUserProfile?.id ?? currentUser?.id;
  const isResolvingCurrentUser = Boolean(routeUserId && token && currentUserId === undefined && currentUserQuery.isLoading);
  const isCurrentUserView = !routeUserId || (currentUserId !== undefined && String(routeUserId) === String(currentUserId));

  const otherUserQuery = useQuery({
    queryKey: ['user', routeUserId],
    queryFn: () => getUser(routeUserId || ''),
    enabled: Boolean(routeUserId && !isCurrentUserView && !isResolvingCurrentUser),
  });

  const profile = isCurrentUserView ? currentUserProfile : otherUserQuery.data?.data;
  const profileId = profile?.id ?? (isCurrentUserView ? currentUserId : routeUserId);
  const profileIsLoading = isResolvingCurrentUser || (isCurrentUserView ? currentUserQuery.isLoading : otherUserQuery.isLoading);
  const profileHasError = isCurrentUserView ? currentUserQuery.isError : otherUserQuery.isError;

  useEffect(() => {
    if (!currentUserProfile) {
      return;
    }

    const nextUser = toAuthUser(currentUserProfile);
    if (nextUser) {
      setUser(nextUser);
    }
  }, [currentUserProfile, setUser]);

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
      queryClient.invalidateQueries({ queryKey: ['user'] });
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

  const updateProfileMutation = useMutation({
    mutationFn: (values: ProfileFormValues) =>
      updateCurrentUserProfile({
        nickname: cleanProfileValue(values.nickname),
        bio: cleanProfileValue(values.bio),
        avatar: cleanProfileValue(values.avatar),
      }),
    onSuccess: async (response) => {
      if (response.code !== 200) {
        message.error(response.message || '资料更新失败');
        return;
      }

      message.success('资料已更新');
      setIsProfileEditorOpen(false);
      await queryClient.invalidateQueries({ queryKey: ['user'] });
    },
    onError: () => message.error('资料更新失败，请稍后再试'),
  });

  const postsPage = postsQuery.data?.data;
  const favoritesPage = favoritesQuery.data?.data;
  const followingPage = followingQuery.data?.data;
  const followersPage = followersQuery.data?.data;
  const posts = postsPage?.records || [];
  const favorites = favoritesPage?.records || [];
  const following = followingPage?.records || [];
  const followers = followersPage?.records || [];
  const pendingPost = postInteractionMutation.variables;
  const pendingPostKey = postInteractionMutation.isPending && pendingPost
    ? `${pendingPost.kind}:${pendingPost.post.id}`
    : '';
  const pendingUser = followMutation.variables;
  const pendingUserId = followMutation.isPending && pendingUser ? String(pendingUser.id) : '';

  const handlePostInteraction = (post: PostSummary, kind: PostInteraction['kind']) => {
    postInteractionMutation.mutate({ post, kind });
  };

  const openProfileEditor = (user: UserProfileInfo) => {
    profileForm.setFieldsValue({
      nickname: user.nickname || '',
      bio: user.bio || '',
      avatar: user.avatar || '',
    });
    setIsProfileEditorOpen(true);
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
        {items.map((post) => (
          <PostPreviewCard
            post={post}
            compact
            key={String(post.id)}
            likePending={pendingPostKey === `like:${post.id}`}
            favoritePending={pendingPostKey === `favorite:${post.id}`}
            onLikeToggle={(item) => handlePostInteraction(item, 'like')}
            onFavoriteToggle={(item) => handlePostInteraction(item, 'favorite')}
          />
        ))}
      </div>
    );
  };

  if (profileIsLoading) {
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
          <Empty description={profileHasError ? '用户信息加载失败' : '用户不存在'} />
        </div>
      </div>
    );
  }

  const stats = {
    postCount: getCount(profile.postCount, postsPage?.total),
    favoriteCount: getCount(profile.favoriteCount, favoritesPage?.total),
    followingCount: getCount(profile.followingCount, followingPage?.total),
    followerCount: getCount(profile.followerCount, followersPage?.total),
  };
  const infoItems = [
    { label: '用户名', value: profile.username },
    { label: '账号类型', value: profile.userType >= 2 ? '管理员' : '普通用户' },
    ...(isCurrentUserView && profile.email ? [{ label: '邮箱', value: profile.email }] : []),
    { label: '加入时间', value: formatDate(profile.createdAt) || '暂未记录' },
    ...(isCurrentUserView ? [{ label: '最近登录', value: formatDate(profile.lastLoginAt) || '暂未记录' }] : []),
    { label: '帖子', value: stats.postCount },
    { label: '收藏', value: stats.favoriteCount },
    { label: '关注', value: stats.followingCount },
    { label: '粉丝', value: stats.followerCount },
  ];

  return (
    <div className="forum-page">
      <section className="profile-card">
        <div className="profile-hero">
          <Avatar size={72} src={profile.avatar || undefined} icon={<UserOutlined />} />
          <div className="profile-hero__copy">
            <span className="eyebrow">{isCurrentUserView ? 'User Center' : 'Profile'}</span>
            <h1>{displayName(profile)}</h1>
            <p>{profile.bio || (isCurrentUserView ? '完善简介，让大家更快认识你。' : `用户 ID：${profile.id}`)}</p>
          </div>
          {isCurrentUserView ? (
            <div className="profile-actions">
              <span className="profile-current-chip">当前账号</span>
              <Button type="primary" icon={<EditOutlined />} onClick={() => openProfileEditor(profile)}>
                编辑资料
              </Button>
            </div>
          ) : (
            <div className="profile-actions">
              <Button icon={<MailOutlined />} onClick={() => navigate(`/messages/${profile.id}`)}>
                发私信
              </Button>
              <Button
                type={profile.followedByCurrentUser ? 'default' : 'primary'}
                loading={pendingUserId === String(profile.id)}
                onClick={() => followMutation.mutate(profile)}
              >
                {profile.followedByCurrentUser ? '已关注' : '关注'}
              </Button>
            </div>
          )}
        </div>

        <div className="profile-body">
          <div className="profile-info-grid">
            {infoItems.map((item) => (
              <div key={item.label}>
                <span>{item.label}</span>
                <strong>{item.value}</strong>
              </div>
            ))}
          </div>

          <Tabs
            className="profile-tabs"
            items={[
              {
                key: 'posts',
                label: isCurrentUserView ? '我的帖子' : '帖子',
                children: renderPostList(
                  posts,
                  postsQuery.isLoading,
                  isCurrentUserView ? '你还没有发布帖子' : '暂无帖子',
                ),
              },
              {
                key: 'favorites',
                label: (
                  <span>
                    <StarOutlined /> {isCurrentUserView ? '我的收藏' : '收藏'}
                  </span>
                ),
                children: renderPostList(
                  favorites,
                  favoritesQuery.isLoading,
                  isCurrentUserView ? '你还没有收藏帖子' : '暂无收藏',
                ),
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
                    currentUserId={currentUserId}
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
                    currentUserId={currentUserId}
                    pendingUserId={pendingUserId}
                    onFollowToggle={(user) => followMutation.mutate(user)}
                  />
                ),
              },
            ]}
          />
        </div>
      </section>

      <Modal
        title="编辑个人资料"
        open={isProfileEditorOpen}
        okText="保存"
        cancelText="取消"
        className="profile-edit-modal"
        confirmLoading={updateProfileMutation.isPending}
        onCancel={() => setIsProfileEditorOpen(false)}
        onOk={() => profileForm.submit()}
      >
        <Form
          form={profileForm}
          layout="vertical"
          className="profile-edit-form"
          onFinish={(values) => updateProfileMutation.mutate(values)}
        >
          <Form.Item name="nickname" label="昵称" rules={[{ max: 50, message: '昵称最长50个字符' }]}>
            <Input placeholder="留空时显示用户名" maxLength={50} showCount />
          </Form.Item>

          <Form.Item name="avatar" label="头像 URL" rules={[{ max: 255, message: '头像URL最长255个字符' }]}>
            <Input placeholder="https://example.com/avatar.png" maxLength={255} showCount />
          </Form.Item>

          <Form.Item name="bio" label="个人简介" rules={[{ max: 500, message: '个人简介最长500个字符' }]}>
            <Input.TextArea rows={5} placeholder="写一点让大家认识你的简介" maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
