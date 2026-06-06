import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  avatar: string;
  userType: number;
  postCount?: number;
  favoriteCount?: number;
  followingCount?: number;
  followerCount?: number;
  followedByCurrentUser?: boolean;
  roles?: string[];
  permissions?: string[];
}

interface AuthState {
  token: string | null;
  user: UserInfo | null;
  setToken: (token: string) => void;
  setUser: (user: UserInfo) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      setToken: (token) => set({ token }),
      setUser: (user) => set({ user }),
      logout: () => set({ token: null, user: null }),
    }),
    {
      name: 'auth-storage',
    }
  )
);
