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
  liked: boolean;
  favorited: boolean;
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
  liked: boolean;
  status: number;
  createdAt: string;
}

export interface InteractionState {
  active: boolean;
  count: number;
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

export function listPosts(params: {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  userId?: Numeric;
  categoryId?: Numeric;
  boardId?: Numeric;
  sort?: string;
}) {
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

export function likePost(postId: number | string) {
  return api.post<unknown, ApiResponse<InteractionState>>(`/posts/${postId}/like`);
}

export function unlikePost(postId: number | string) {
  return api.delete<unknown, ApiResponse<InteractionState>>(`/posts/${postId}/like`);
}

export function favoritePost(postId: number | string) {
  return api.post<unknown, ApiResponse<InteractionState>>(`/posts/${postId}/favorite`);
}

export function unfavoritePost(postId: number | string) {
  return api.delete<unknown, ApiResponse<InteractionState>>(`/posts/${postId}/favorite`);
}

export function likeComment(commentId: number | string) {
  return api.post<unknown, ApiResponse<InteractionState>>(`/comments/${commentId}/like`);
}

export function unlikeComment(commentId: number | string) {
  return api.delete<unknown, ApiResponse<InteractionState>>(`/comments/${commentId}/like`);
}
