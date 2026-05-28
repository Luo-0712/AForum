import { useParams } from 'react-router-dom';
import { Card, Typography } from 'antd';

const { Title, Paragraph } = Typography;

export default function PostDetail() {
  const { id } = useParams();

  return (
    <Card>
      <Title level={2}>帖子详情</Title>
      <Paragraph>帖子ID: {id}</Paragraph>
      <Paragraph>内容加载中...</Paragraph>
    </Card>
  );
}
