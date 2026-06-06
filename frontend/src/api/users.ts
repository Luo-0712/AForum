import api from './axios';
import type { ApiResponse, Numeric, PageResult, PostSummary } from './posts';

export interface UserProfileInfo {
  id: Numeric;
  username: string;
  email?: string;
  nickname?: string;
  avatar?: string;
  bio?: string;
  userType: number;
  status: number;
  postCount: number;
  favoriteCount: number;
  followingCount: number;
  followerCount: number;
  followedByCurrentUser: boolean;
  createdAt?: string;
  lastLoginAt?: string;
}

export interface FollowState {
  following: boolean;
  followerCount: number;
  followingCount: number;
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
