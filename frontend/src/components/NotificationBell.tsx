import { Badge, Button } from 'antd';
import { BellOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useMessageStore } from '../store/message';

export default function NotificationBell() {
  const navigate = useNavigate();
  const count = useMessageStore((state) => state.notificationUnreadCount);

  return (
    <Badge count={count} size="small" overflowCount={99}>
      <Button aria-label="通知中心" icon={<BellOutlined />} onClick={() => navigate('/notifications')} />
    </Badge>
  );
}
