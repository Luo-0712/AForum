import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../store/auth';

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const token = useAuthStore((state) => state.token);
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

export function AdminRoute({ children }: { children: ReactNode }) {
  const token = useAuthStore((state) => state.token);
  const user = useAuthStore((state) => state.user);
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  if (!user || !canAccessAdmin(user.roles, user.permissions)) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}

function canAccessAdmin(roles: string[] = [], permissions: string[] = []) {
  return (
    roles.includes('SUPER_ADMIN') ||
    roles.includes('ADMIN') ||
    permissions.some((permission) =>
      [
        'admin:dashboard',
        'user:list',
        'user:edit',
        'post:list',
        'post:review',
        'role:manage',
        'permission:manage',
      ].includes(permission),
    )
  );
}
