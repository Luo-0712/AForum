import { Link, useNavigate } from 'react-router-dom';
import { Button, Form, Input, message } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import api from '../api/axios';
import AuthShell from '../components/AuthShell';
import { useAuthStore } from '../store/auth';

interface LoginForm {
  username: string;
  password: string;
}

interface AuthUser {
  id: number;
  username: string;
  nickname: string;
  avatar: string;
  userType: number;
  roles?: string[];
  permissions?: string[];
}

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

interface LoginData {
  accessToken: string;
  user: AuthUser;
}

export default function Login() {
  const navigate = useNavigate();
  const { setToken, setUser } = useAuthStore();

  const onFinish = async (values: LoginForm) => {
    try {
      const res = await api.post<unknown, ApiResponse<LoginData>>('/auth/login', values);
      if (res.code === 200) {
        setToken(res.data.accessToken);
        setUser(res.data.user);
        message.success('登录成功');
        navigate('/');
      } else {
        message.error(res.message || '登录失败');
      }
    } catch (error: any) {
      message.error(error?.response?.data?.message || '登录失败');
    }
  };

  return (
    <AuthShell title="欢迎回来" subtitle="继续你的开发者讨论流。">
      <Form name="login" layout="vertical" onFinish={onFinish}>
        <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
          <Input prefix={<UserOutlined />} placeholder="用户名" />
        </Form.Item>
        <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
          <Input.Password prefix={<LockOutlined />} placeholder="密码" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block>
          登录
        </Button>
        <div className="auth-switch">
          还没有账号？ <Link to="/register">立即注册</Link>
        </div>
      </Form>
    </AuthShell>
  );
}
