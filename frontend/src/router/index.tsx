import { createBrowserRouter, Navigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import AdminLayout from '../layouts/AdminLayout';
import Home from '../pages/Home';
import Login from '../pages/Login';
import Register from '../pages/Register';
import PostList from '../pages/PostList';
import PostDetail from '../pages/PostDetail';
import PostEditor from '../pages/PostEditor';
import UserProfile from '../pages/UserProfile';
import AdminDashboard from '../pages/admin/Dashboard';
import { useAuthStore } from '../store/auth';

// 路由守卫
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((state) => state.token);
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

function AdminRoute({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((state) => state.token);
  const user = useAuthStore((state) => state.user);
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  if (!user || user.userType < 2) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      {
        index: true,
        element: <Home />,
      },
      {
        path: 'posts',
        element: <PostList />,
      },
      {
        path: 'posts/new',
        element: (
          <ProtectedRoute>
            <PostEditor />
          </ProtectedRoute>
        ),
      },
      {
        path: 'posts/:id',
        element: <PostDetail />,
      },
      {
        path: 'user/:id',
        element: (
          <ProtectedRoute>
            <UserProfile />
          </ProtectedRoute>
        ),
      },
    ],
  },
  {
    path: '/login',
    element: <Login />,
  },
  {
    path: '/register',
    element: <Register />,
  },
  {
    path: '/admin',
    element: (
      <AdminRoute>
        <AdminLayout />
      </AdminRoute>
    ),
    children: [
      {
        index: true,
        element: <AdminDashboard />,
      },
    ],
  },
]);
