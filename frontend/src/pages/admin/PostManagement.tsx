import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Button,
  Input,
  Modal,
  Popconfirm,
  Segmented,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  CheckCircleOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
  StarOutlined,
  StopOutlined,
  VerticalAlignTopOutlined,
} from '@ant-design/icons';
import {
  approveAdminPost,
  deleteAdminPost,
  listAdminPosts,
  listPendingAdminPosts,
  rejectAdminPost,
  updateAdminPostFlags,
  type AdminPost,
  type AdminPostQuery,
} from '../../api/adminManagement';
import type { Numeric } from '../../api/posts';

type PostMode = 'all' | 'pending';

const STATUS_OPTIONS = [
  { label: 'All Status', value: -1 },
  { label: 'Draft', value: 0 },
  { label: 'Published', value: 1 },
  { label: 'Reviewing', value: 2 },
  { label: 'Rejected', value: 3 },
  { label: 'Closed', value: 4 },
];

function statusTag(status: number) {
  switch (status) {
    case 1:
      return <Tag color="green">Published</Tag>;
    case 2:
      return <Tag color="gold">Reviewing</Tag>;
    case 3:
      return <Tag color="red">Rejected</Tag>;
    case 4:
      return <Tag>Closed</Tag>;
    default:
      return <Tag>Draft</Tag>;
  }
}

function toId(value: Numeric) {
  return String(value);
}

function toNumber(value: Numeric | undefined) {
  return Number(value ?? 0);
}

export default function PostManagement() {
  const queryClient = useQueryClient();
  const [mode, setMode] = useState<PostMode>('pending');
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState(-1);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [rejectingPost, setRejectingPost] = useState<AdminPost | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  const params: AdminPostQuery = useMemo(
    () => ({
      pageNum: page,
      pageSize,
      keyword: keyword.trim() || undefined,
      status: mode === 'all' && status >= 0 ? status : undefined,
    }),
    [keyword, mode, page, pageSize, status],
  );

  const postsQuery = useQuery({
    queryKey: ['admin-management', 'posts', mode, params],
    queryFn: () => (mode === 'pending' ? listPendingAdminPosts(params) : listAdminPosts(params)),
  });

  const refreshPosts = async () => {
    await queryClient.invalidateQueries({ queryKey: ['admin-management', 'posts'] });
    await queryClient.invalidateQueries({ queryKey: ['admin-management', 'overview'] });
  };

  const approveMutation = useMutation({
    mutationFn: approveAdminPost,
    onSuccess: async (response) => {
      if (response.code === 200) {
        message.success('Post approved');
        await refreshPosts();
      } else {
        message.error(response.message || 'Failed to approve post');
      }
    },
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: Numeric; reason?: string }) => rejectAdminPost(id, reason),
    onSuccess: async (response) => {
      if (response.code === 200) {
        message.success('Post rejected');
        setRejectingPost(null);
        setRejectReason('');
        await refreshPosts();
      } else {
        message.error(response.message || 'Failed to reject post');
      }
    },
  });

  const flagMutation = useMutation({
    mutationFn: ({ record, field }: { record: AdminPost; field: 'top' | 'essence' }) =>
      updateAdminPostFlags(
        record.id,
        field === 'top' ? !record.top : undefined,
        field === 'essence' ? !record.essence : undefined,
      ),
    onSuccess: async (response) => {
      if (response.code === 200) {
        message.success('Post flags updated');
        await refreshPosts();
      } else {
        message.error(response.message || 'Failed to update flags');
      }
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteAdminPost,
    onSuccess: async (response) => {
      if (response.code === 200) {
        message.success('Post deleted');
        await refreshPosts();
      } else {
        message.error(response.message || 'Failed to delete post');
      }
    },
  });

  const records = postsQuery.data?.data?.records ?? [];
  const total = toNumber(postsQuery.data?.data?.total);
  const busy = approveMutation.isPending || rejectMutation.isPending || flagMutation.isPending || deleteMutation.isPending;

  const columns: ColumnsType<AdminPost> = [
    {
      title: 'Post',
      dataIndex: 'title',
      key: 'title',
      render: (_value, record) => (
        <Space direction="vertical" size={2}>
          <Typography.Text strong>{record.title}</Typography.Text>
          <Typography.Text type="secondary" ellipsis className="admin-post-summary">
            {record.summary || record.content || '-'}
          </Typography.Text>
          <Space wrap>
            {record.top && <Tag color="blue">Top</Tag>}
            {record.essence && <Tag color="purple">Essence</Tag>}
            {record.auditReason && <Tag color="orange">{record.auditReason}</Tag>}
          </Space>
        </Space>
      ),
    },
    {
      title: 'Author',
      dataIndex: 'authorName',
      key: 'authorName',
      width: 160,
      render: (value?: string, record?: AdminPost) => value || `#${record?.userId ?? '-'}`,
    },
    {
      title: 'Scope',
      key: 'scope',
      width: 190,
      render: (_value, record) => (
        <Space direction="vertical" size={0}>
          <span>{record.boardName || '-'}</span>
          <span className="admin-muted">{record.categoryName || '-'}</span>
        </Space>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (value: number) => statusTag(value),
    },
    {
      title: 'Signals',
      key: 'signals',
      width: 170,
      render: (_value, record) => (
        <Space wrap>
          <Tag>{record.viewCount} views</Tag>
          <Tag>{record.likeCount} likes</Tag>
          <Tag>{record.commentCount} comments</Tag>
        </Space>
      ),
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (value?: string) => (value ? new Date(value).toLocaleString() : '-'),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 280,
      fixed: 'right',
      render: (_value, record) => (
        <Space wrap>
          {record.status !== 1 && (
            <Tooltip title="Approve">
              <Button
                size="small"
                type="primary"
                icon={<CheckCircleOutlined />}
                onClick={() => approveMutation.mutate(record.id)}
              />
            </Tooltip>
          )}
          {record.status !== 3 && (
            <Tooltip title="Reject">
              <Button size="small" icon={<StopOutlined />} onClick={() => setRejectingPost(record)} />
            </Tooltip>
          )}
          <Tooltip title={record.top ? 'Remove top' : 'Set top'}>
            <Button
              size="small"
              icon={<VerticalAlignTopOutlined />}
              type={record.top ? 'primary' : 'default'}
              onClick={() => flagMutation.mutate({ record, field: 'top' })}
            />
          </Tooltip>
          <Tooltip title={record.essence ? 'Remove essence' : 'Set essence'}>
            <Button
              size="small"
              icon={<StarOutlined />}
              type={record.essence ? 'primary' : 'default'}
              onClick={() => flagMutation.mutate({ record, field: 'essence' })}
            />
          </Tooltip>
          <Popconfirm title="Delete this post?" onConfirm={() => deleteMutation.mutate(record.id)}>
            <Tooltip title="Delete">
              <Button size="small" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="admin-management-page">
      <div className="page-heading">
        <div>
          <span className="eyebrow">Moderation</span>
          <h1>Post Management</h1>
          <p>Review pending posts, moderate published content, and adjust ranking flags.</p>
        </div>
        <div className="page-toolbar admin-filterbar">
          <Segmented
            value={mode}
            options={[
              { label: 'Pending', value: 'pending' },
              { label: 'All Posts', value: 'all' },
            ]}
            onChange={(value) => {
              setMode(value as PostMode);
              setPage(1);
            }}
          />
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="Search title or content"
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setPage(1);
            }}
          />
          {mode === 'all' && (
            <Select
              value={status}
              options={STATUS_OPTIONS}
              onChange={(value) => {
                setStatus(value);
                setPage(1);
              }}
            />
          )}
          <Button icon={<ReloadOutlined />} onClick={() => void postsQuery.refetch()}>
            Refresh
          </Button>
        </div>
      </div>

      <section className="section-card admin-table-card">
        <Table<AdminPost>
          rowKey={(record) => toId(record.id)}
          loading={postsQuery.isLoading || busy}
          columns={columns}
          dataSource={records}
          scroll={{ x: 1180 }}
          pagination={{
            current: page,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (nextPage, nextSize) => {
              setPage(nextPage);
              setPageSize(nextSize);
            },
          }}
        />
      </section>

      <Modal
        title="Reject Post"
        open={rejectingPost != null}
        okText="Reject"
        confirmLoading={rejectMutation.isPending}
        onCancel={() => {
          setRejectingPost(null);
          setRejectReason('');
        }}
        onOk={() => {
          if (rejectingPost) {
            rejectMutation.mutate({ id: rejectingPost.id, reason: rejectReason.trim() || undefined });
          }
        }}
      >
        <Input.TextArea
          rows={4}
          maxLength={500}
          showCount
          value={rejectReason}
          placeholder="Reason shown to the author"
          onChange={(event) => setRejectReason(event.target.value)}
        />
      </Modal>
    </div>
  );
}
