import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Form, Input, message } from 'antd';
import { CheckCircleOutlined, SaveOutlined, UploadOutlined } from '@ant-design/icons';
import { createPost, type PostDetail } from '../api/posts';

const { TextArea } = Input;

interface PostForm {
  title: string;
  content: string;
}

export default function PostEditor() {
  const navigate = useNavigate();
  const [saving, setSaving] = useState(false);
  const [reviewedPost, setReviewedPost] = useState<PostDetail | null>(null);

  const onFinish = async (values: PostForm) => {
    setSaving(true);
    setReviewedPost(null);
    try {
      const res = await createPost(values);
      if (res.code !== 200) {
        message.error(res.message || '发布失败');
        return;
      }
      if (res.data.status === 1) {
        message.success('帖子已发布');
        navigate(`/posts/${res.data.id}`);
      } else {
        setReviewedPost(res.data);
        message.warning('帖子未通过基础审核');
      }
    } catch {
      message.error('发布失败');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="forum-page editor-page">
      <div className="page-heading">
        <div>
          <span className="eyebrow">Compose</span>
          <h1>发起讨论</h1>
          <p>发布后会经过基础审核，通过后进入帖子流。</p>
        </div>
      </div>

      <div className="editor-layout">
        <section className="editor-main">
          {reviewedPost && (
            <Alert
              className="review-alert"
              showIcon
              type="warning"
              message="基础审核未通过"
              description={reviewedPost.auditReason || '请修改内容后重新提交。'}
            />
          )}

          <Form layout="vertical" onFinish={onFinish}>
            <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
              <Input placeholder="一个清晰的问题或观点" maxLength={200} showCount />
            </Form.Item>

            <div className="editor-media-slot">
              <strong>封面与图片扩展位</strong>
              <span>未来接入图片字段后，这里会承载帖子封面和正文图片上传。</span>
              <Button icon={<UploadOutlined />} disabled>
                添加封面
              </Button>
            </div>

            <Form.Item name="content" label="正文" rules={[{ required: true, message: '请输入正文' }]}>
              <TextArea
                rows={13}
                placeholder="补充背景、尝试过的方法、期望讨论的方向"
                maxLength={20000}
                showCount
              />
            </Form.Item>

            <div className="editor-actions">
              <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saving}>
                发布
              </Button>
            </div>
          </Form>
        </section>

        <aside className="editor-aside">
          <span className="eyebrow">Publish</span>
          <h2>发布前</h2>
          <div className="publish-check">
            <CheckCircleOutlined />
            <span>标题聚焦一个具体问题或观点。</span>
          </div>
          <div className="publish-check">
            <CheckCircleOutlined />
            <span>正文保留必要上下文，方便他人接上讨论。</span>
          </div>
          <div className="publish-check">
            <CheckCircleOutlined />
            <span>图片能力已预留，当前不会提交额外字段。</span>
          </div>
        </aside>
      </div>
    </div>
  );
}
