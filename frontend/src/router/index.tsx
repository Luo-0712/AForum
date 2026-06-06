import { createBrowserRouter } from 'react-router-dom';
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
import AccessControl from '../pages/admin/AccessControl';
import { AdminRoute, ProtectedRoute } from './guards';

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
      {
        path: 'access',
        element: <AccessControl />,
      },
    ],
  },
]);
