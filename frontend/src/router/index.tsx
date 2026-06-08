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
import Notifications from '../pages/Notifications';
import Messages from '../pages/Messages';
import AdminDashboard from '../pages/admin/Dashboard';
import AccessControl from '../pages/admin/AccessControl';
import UserManagement from '../pages/admin/UserManagement';
import PostManagement from '../pages/admin/PostManagement';
import BoardCategoryManagement from '../pages/admin/BoardCategoryManagement';
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
        path: 'user',
        element: (
          <ProtectedRoute>
            <UserProfile />
          </ProtectedRoute>
        ),
      },
      {
        path: 'user/:id',
        element: (
          <ProtectedRoute>
            <UserProfile />
          </ProtectedRoute>
        ),
      },
      {
        path: 'notifications',
        element: (
          <ProtectedRoute>
            <Notifications />
          </ProtectedRoute>
        ),
      },
      {
        path: 'messages',
        element: (
          <ProtectedRoute>
            <Messages />
          </ProtectedRoute>
        ),
      },
      {
        path: 'messages/:peerUserId',
        element: (
          <ProtectedRoute>
            <Messages />
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
        path: 'users',
        element: <UserManagement />,
      },
      {
        path: 'posts',
        element: <PostManagement />,
      },
      {
        path: 'boards',
        element: <BoardCategoryManagement />,
      },
      {
        path: 'access',
        element: <AccessControl />,
      },
    ],
  },
]);
