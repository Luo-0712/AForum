import api from './axios';
import type { ApiResponse, Numeric, PageResult } from './posts';

export interface AdminDashboard {
  userCount: Numeric;
  activeUserCount: Numeric;
  disabledUserCount: Numeric;
  postCount: Numeric;
  publishedPostCount: Numeric;
  pendingPostCount: Numeric;
  rejectedPostCount: Numeric;
  commentCount: Numeric;
  boardCount: Numeric;
  categoryCount: Numeric;
}

export interface AdminTrendPoint {
  date: string;
  userCount: Numeric;
  postCount: Numeric;
  commentCount: Numeric;
}

export interface AdminBoard {
  id: Numeric;
  name: string;
  code: string;
  description?: string;
  icon?: string;
  sort: number;
  status: number;
  postCount: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminCategory {
  id: Numeric;
  name: string;
  code: string;
  description?: string;
  icon?: string;
  parentId?: Numeric;
  sort: number;
  status: number;
  postCount: Numeric;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminUser {
  id: Numeric;
  username: string;
  email?: string;
  phone?: string;
  nickname?: string;
  avatar?: string;
  userType: number;
  status: number;
  roles: string[];
  postCount: Numeric;
  lastLoginAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminPost {
  id: Numeric;
  title: string;
  summary?: string;
  content?: string;
  userId: Numeric;
  authorName?: string;
  categoryId?: Numeric;
  categoryName?: string;
  boardId?: Numeric;
  boardName?: string;
  status: number;
  auditReason?: string;
  top: boolean;
  essence: boolean;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  favoriteCount: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminBoardPayload {
  name: string;
  code: string;
  description?: string;
  icon?: string;
  sort?: number;
  status?: number;
}

export interface AdminCategoryPayload extends AdminBoardPayload {
  parentId?: Numeric;
}

export interface AdminUserQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
  userType?: number;
}

export interface AdminPostQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  userId?: Numeric;
  categoryId?: Numeric;
  boardId?: Numeric;
  status?: number;
}

export function getAdminOverview() {
  return api.get<unknown, ApiResponse<AdminDashboard>>('/admin/stats/overview');
}

export function getAdminTrends(days = 7) {
  return api.get<unknown, ApiResponse<AdminTrendPoint[]>>('/admin/stats/trends', { params: { days } });
}

export function listAdminBoards() {
  return api.get<unknown, ApiResponse<AdminBoard[]>>('/admin/boards');
}

export function createAdminBoard(payload: AdminBoardPayload) {
  return api.post<unknown, ApiResponse<AdminBoard>>('/admin/boards', payload);
}

export function updateAdminBoard(id: Numeric, payload: AdminBoardPayload) {
  return api.put<unknown, ApiResponse<AdminBoard>>(`/admin/boards/${id}`, payload);
}

export function deleteAdminBoard(id: Numeric) {
  return api.delete<unknown, ApiResponse<void>>(`/admin/boards/${id}`);
}

export function listAdminCategories() {
  return api.get<unknown, ApiResponse<AdminCategory[]>>('/admin/categories');
}

export function createAdminCategory(payload: AdminCategoryPayload) {
  return api.post<unknown, ApiResponse<AdminCategory>>('/admin/categories', payload);
}

export function updateAdminCategory(id: Numeric, payload: AdminCategoryPayload) {
  return api.put<unknown, ApiResponse<AdminCategory>>(`/admin/categories/${id}`, payload);
}

export function deleteAdminCategory(id: Numeric) {
  return api.delete<unknown, ApiResponse<void>>(`/admin/categories/${id}`);
}

export function listAdminUsers(params: AdminUserQuery) {
  return api.get<unknown, ApiResponse<PageResult<AdminUser>>>('/admin/users', { params });
}

export function updateAdminUserStatus(id: Numeric, status: number) {
  return api.put<unknown, ApiResponse<AdminUser>>(`/admin/users/${id}/status`, { status });
}

export function listAdminPosts(params: AdminPostQuery) {
  return api.get<unknown, ApiResponse<PageResult<AdminPost>>>('/admin/posts', { params });
}

export function listPendingAdminPosts(params: AdminPostQuery) {
  return api.get<unknown, ApiResponse<PageResult<AdminPost>>>('/admin/audit/pending', { params });
}

export function approveAdminPost(id: Numeric) {
  return api.put<unknown, ApiResponse<AdminPost>>(`/admin/audit/posts/${id}/approve`);
}

export function rejectAdminPost(id: Numeric, reason?: string) {
  return api.put<unknown, ApiResponse<AdminPost>>(`/admin/audit/posts/${id}/reject`, { reason });
}

export function updateAdminPostFlags(id: Numeric, top?: boolean, essence?: boolean) {
  return api.put<unknown, ApiResponse<AdminPost>>(`/admin/posts/${id}/flags`, { top, essence });
}

export function deleteAdminPost(id: Numeric) {
  return api.delete<unknown, ApiResponse<void>>(`/admin/posts/${id}`);
}
