import { Link, useNavigate } from 'react-router-dom';
import { Button, Form, Input, message } from 'antd';
import { LockOutlined, MailOutlined, UserOutlined } from '@ant-design/icons';
import api from '../api/axios';
import AuthShell from '../components/AuthShell';

interface RegisterForm {
  username: string;
  password: string;
  confirmPassword: string;
  email: string;
}

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export default function Register() {
  const navigate = useNavigate();

  const onFinish = async (values: RegisterForm) => {
    try {
      const res = await api.post<unknown, ApiResponse<unknown>>('/auth/register', values);
      if (res.code === 200) {
        message.success('注册成功');
        navigate('/login');
      } else {
        message.error(res.message || '注册失败');
      }
    } catch {
      message.error('注册失败');
    }
  };

  return (
    <AuthShell title="创建账号" subtitle="加入 AForum，沉淀你的问题与经验。">
      <Form name="register" layout="vertical" onFinish={onFinish}>
        <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
          <Input prefix={<UserOutlined />} placeholder="用户名" />
        </Form.Item>
        <Form.Item name="email" rules={[{ required: true, type: 'email', message: '请输入有效的邮箱' }]}>
          <Input prefix={<MailOutlined />} placeholder="邮箱" />
        </Form.Item>
        <Form.Item name="password" rules={[{ required: true, min: 8, message: '密码至少 8 位' }]}>
          <Input.Password prefix={<LockOutlined />} placeholder="密码" />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          dependencies={['password']}
          rules={[
            { required: true, message: '请确认密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('password') === value) {
                  return Promise.resolve();
                }
                return Promise.reject(new Error('两次密码不一致'));
              },
            }),
          ]}
        >
          <Input.Password prefix={<LockOutlined />} placeholder="确认密码" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block>
          注册
        </Button>
        <div className="auth-switch">
          已有账号？ <Link to="/login">立即登录</Link>
        </div>
      </Form>
    </AuthShell>
  );
}
