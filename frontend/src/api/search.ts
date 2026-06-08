import api from './axios';
import type { ApiResponse, Numeric, PageResult, PostSummary } from './posts';

export interface SearchHighlight {
  title?: string[];
  summary?: string[];
  content?: string[];
}

export interface SearchPostSummary extends PostSummary {
  highlights?: SearchHighlight;
  top?: boolean;
  essence?: boolean;
  updatedAt?: string;
}

export interface SearchPostParams {
  q: string;
  pageNum?: number;
  pageSize?: number;
  categoryId?: Numeric;
  boardId?: Numeric;
  authorId?: Numeric;
  sort?: 'relevance' | 'latest' | 'hot';
  highlight?: boolean;
}

export interface SearchOption {
  id: Numeric;
  name: string;
}

export interface SearchOptions {
  categories: SearchOption[];
  boards: SearchOption[];
}

export function searchPosts(params: SearchPostParams) {
  return api.get<unknown, ApiResponse<PageResult<SearchPostSummary>>>('/search', { params });
}

export function getSearchOptions() {
  return api.get<unknown, ApiResponse<SearchOptions>>('/search/options');
}
