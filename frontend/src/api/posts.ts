import api from './axios';

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export type Numeric = number | string;

export interface PageResult<T> {
  records: T[];
  total: Numeric;
  size: Numeric;
  current: Numeric;
  pages: Numeric;
}

export interface PostSummary {
  id: Numeric;
  title: string;
  summary: string;
  coverUrl?: string;
  userId: Numeric;
  authorName: string;
  categoryName?: string;
  boardName?: string;
  status: number;
  auditReason?: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  favoriteCount: number;
  createdAt: string;
  lastCommentAt?: string;
}

export interface PostDetail extends PostSummary {
  content: string;
  suggestedTags?: string[];
  top: boolean;
  essence: boolean;
  updatedAt: string;
}

export interface CommentItem {
  id: Numeric;
  content: string;
  postId: Numeric;
  userId: Numeric;
  authorName: string;
  parentId: Numeric;
  replyUserId?: Numeric;
  likeCount: number;
  status: number;
  createdAt: string;
}

export interface PostPayload {
  title: string;
  content: string;
  categoryId?: Numeric;
  boardId?: Numeric;
}

export interface CommentPayload {
  content: string;
  parentId?: Numeric;
  replyUserId?: Numeric;
}

export function listPosts(params: { pageNum?: number; pageSize?: number; keyword?: string }) {
  return api.get<unknown, ApiResponse<PageResult<PostSummary>>>('/posts', { params });
}

export function getPost(id: number | string) {
  return api.get<unknown, ApiResponse<PostDetail>>(`/posts/${id}`);
}

export function createPost(payload: PostPayload) {
  return api.post<unknown, ApiResponse<PostDetail>>('/posts', payload);
}

export function updatePost(id: number | string, payload: PostPayload) {
  return api.put<unknown, ApiResponse<PostDetail>>(`/posts/${id}`, payload);
}

export function deletePost(id: number | string) {
  return api.delete<unknown, ApiResponse<void>>(`/posts/${id}`);
}

export function listComments(postId: number | string) {
  return api.get<unknown, ApiResponse<CommentItem[]>>(`/posts/${postId}/comments`);
}

export function createComment(postId: number | string, payload: CommentPayload) {
  return api.post<unknown, ApiResponse<CommentItem>>(`/posts/${postId}/comments`, payload);
}
