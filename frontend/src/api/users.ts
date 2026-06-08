import api from './axios';
import type { ApiResponse, Numeric, PageResult, PostSummary } from './posts';

export interface UserProfileInfo {
  id: Numeric;
  username: string;
  email?: string | null;
  nickname?: string | null;
  avatar?: string | null;
  bio?: string | null;
  userType: number;
  status: number;
  postCount?: number | null;
  favoriteCount?: number | null;
  followingCount?: number | null;
  followerCount?: number | null;
  followedByCurrentUser?: boolean | null;
  createdAt?: string;
  lastLoginAt?: string;
  roles?: string[];
  permissions?: string[];
}

export interface FollowState {
  following: boolean;
  followerCount: number;
  followingCount: number;
}

export interface UpdateProfilePayload {
  nickname?: string;
  bio?: string;
  avatar?: string;
}

export function getCurrentUser() {
  return api.get<unknown, ApiResponse<UserProfileInfo>>('/users/me');
}

export function updateCurrentUserProfile(payload: UpdateProfilePayload) {
  return api.put<unknown, ApiResponse<void>>('/users/me', payload);
}

export function getUser(id: Numeric) {
  return api.get<unknown, ApiResponse<UserProfileInfo>>(`/users/${id}`);
}

export function listUserFavorites(id: Numeric, params: { pageNum?: number; pageSize?: number } = {}) {
  return api.get<unknown, ApiResponse<PageResult<PostSummary>>>(`/users/${id}/favorites`, { params });
}

export function listFollowing(id: Numeric, params: { pageNum?: number; pageSize?: number } = {}) {
  return api.get<unknown, ApiResponse<PageResult<UserProfileInfo>>>(`/users/${id}/following`, { params });
}

export function listFollowers(id: Numeric, params: { pageNum?: number; pageSize?: number } = {}) {
  return api.get<unknown, ApiResponse<PageResult<UserProfileInfo>>>(`/users/${id}/followers`, { params });
}

export function followUser(id: Numeric) {
  return api.post<unknown, ApiResponse<FollowState>>(`/users/${id}/follow`);
}

export function unfollowUser(id: Numeric) {
  return api.delete<unknown, ApiResponse<FollowState>>(`/users/${id}/follow`);
}
