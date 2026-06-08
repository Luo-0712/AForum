import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Avatar, Button, Input, Modal, Popconfirm, Select, Space, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ReloadOutlined, SearchOutlined, StopOutlined, TeamOutlined, UserSwitchOutlined } from '@ant-design/icons';
import {
  listAdminUsers,
  updateAdminUserStatus,
  type AdminUser,
  type AdminUserQuery,
} from '../../api/adminManagement';
import { listRoles, updateUserRoles } from '../../api/adminRbac';
import type { Numeric } from '../../api/posts';

const STATUS_OPTIONS = [
  { label: 'All Status', value: -1 },
  { label: 'Active', value: 0 },
  { label: 'Disabled', value: 1 },
  { label: 'Banned', value: 2 },
];

const TYPE_OPTIONS = [
  { label: 'All Types', value: -1 },
  { label: 'User', value: 0 },
  { label: 'VIP', value: 1 },
  { label: 'Admin', value: 2 },
  { label: 'Super Admin', value: 3 },
];

function statusTag(status: number) {
  if (status === 0) {
    return <Tag color="green">Active</Tag>;
  }
  if (status === 2) {
    return <Tag color="red">Banned</Tag>;
  }
  return <Tag>Disabled</Tag>;
}

function typeLabel(userType: number) {
  return ['User', 'VIP', 'Admin', 'Super Admin'][userType] ?? `Type ${userType}`;
}

function toId(value: Numeric) {
  return String(value);
}

function toNumber(value: Numeric | undefined) {
  return Number(value ?? 0);
}

export default function UserManagement() {
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState(-1);
  const [userType, setUserType] = useState(-1);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [roleUser, setRoleUser] = useState<AdminUser | null>(null);
  const [selectedRoleIds, setSelectedRoleIds] = useState<Numeric[]>([]);

  const params: AdminUserQuery = useMemo(
    () => ({
      pageNum: page,
      pageSize,
      keyword: keyword.trim() || undefined,
      status: status >= 0 ? status : undefined,
      userType: userType >= 0 ? userType : undefined,
    }),
    [keyword, page, pageSize, status, userType],
  );

  const usersQuery = useQuery({
    queryKey: ['admin-management', 'users', params],
    queryFn: () => listAdminUsers(params),
  });

  const rolesQuery = useQuery({
    queryKey: ['admin-rbac', 'roles'],
    queryFn: listRoles,
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, nextStatus }: { id: Numeric; nextStatus: number }) => updateAdminUserStatus(id, nextStatus),
    onSuccess: async (response) => {
      if (response.code === 200) {
        message.success('User status updated');
        await queryClient.invalidateQueries({ queryKey: ['admin-management', 'users'] });
      } else {
        message.error(response.message || 'Failed to update user');
      }
    },
  });

  const roleMutation = useMutation({
    mutationFn: ({ id, roleIds }: { id: Numeric; roleIds: Numeric[] }) => updateUserRoles(id, roleIds),
    onSuccess: async (response) => {
      if (response.code === 200) {
        message.success('User roles updated');
        setRoleUser(null);
        setSelectedRoleIds([]);
        await queryClient.invalidateQueries({ queryKey: ['admin-management', 'users'] });
      } else {
        message.error(response.message || 'Failed to update roles');
      }
    },
  });

  const records = usersQuery.data?.data?.records ?? [];
  const total = toNumber(usersQuery.data?.data?.total);
  const roles = rolesQuery.data?.data ?? [];
  const editableRoles = roles.filter((role) => role.status === 0);
  const roleOptions = editableRoles.map((role) => ({
    label: `${role.name} (${role.code})`,
    value: role.id,
  }));

  const openRoleEditor = (record: AdminUser) => {
    setRoleUser(record);
    setSelectedRoleIds(
      editableRoles
        .filter((role) => record.roles?.includes(role.code))
        .map((role) => role.id),
    );
  };

  const columns: ColumnsType<AdminUser> = [
    {
      title: 'User',
      dataIndex: 'username',
      key: 'username',
      render: (_value, record) => (
        <Space>
          <Avatar src={record.avatar}>{record.username?.slice(0, 1)?.toUpperCase()}</Avatar>
          <Space direction="vertical" size={0}>
            <strong>{record.nickname || record.username}</strong>
            <span className="admin-muted">{record.email || record.username}</span>
          </Space>
        </Space>
      ),
    },
    {
      title: 'Roles',
      dataIndex: 'roles',
      key: 'roles',
      render: (roles: string[]) => (
        <Space wrap>
          {(roles ?? []).map((role) => (
            <Tag key={role} color={role === 'SUPER_ADMIN' ? 'gold' : role === 'ADMIN' ? 'blue' : 'default'}>
              {role}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: 'Type',
      dataIndex: 'userType',
      key: 'userType',
      width: 130,
      render: (value: number) => typeLabel(value),
    },
    {
      title: 'Posts',
      dataIndex: 'postCount',
      key: 'postCount',
      width: 100,
      render: (value: Numeric) => toNumber(value),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (value: number) => statusTag(value),
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
      width: 220,
      render: (_value, record) => (
        <Space wrap>
          {record.status === 0 ? (
            <Popconfirm
              title="Disable this user?"
              onConfirm={() => statusMutation.mutate({ id: record.id, nextStatus: 1 })}
            >
              <Button size="small" icon={<StopOutlined />}>
                Disable
              </Button>
            </Popconfirm>
          ) : (
            <Popconfirm
              title="Enable this user?"
              onConfirm={() => statusMutation.mutate({ id: record.id, nextStatus: 0 })}
            >
              <Button size="small" type="primary" icon={<UserSwitchOutlined />}>
                Enable
              </Button>
            </Popconfirm>
          )}
          {record.status !== 2 && (
            <Popconfirm
              title="Ban this user?"
              onConfirm={() => statusMutation.mutate({ id: record.id, nextStatus: 2 })}
            >
              <Button size="small" danger>
                Ban
              </Button>
            </Popconfirm>
          )}
          <Button size="small" icon={<TeamOutlined />} onClick={() => openRoleEditor(record)}>
            Roles
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="admin-management-page">
      <div className="page-heading">
        <div>
          <span className="eyebrow">Users</span>
          <h1>User Management</h1>
          <p>Search accounts, inspect roles, and control account availability.</p>
        </div>
        <div className="page-toolbar admin-filterbar">
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="Search username, nickname, email"
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setPage(1);
            }}
          />
          <Select value={status} options={STATUS_OPTIONS} onChange={(value) => { setStatus(value); setPage(1); }} />
          <Select value={userType} options={TYPE_OPTIONS} onChange={(value) => { setUserType(value); setPage(1); }} />
          <Button icon={<ReloadOutlined />} onClick={() => void usersQuery.refetch()}>
            Refresh
          </Button>
        </div>
      </div>

      <section className="section-card admin-table-card">
        <Table<AdminUser>
          rowKey={(record) => toId(record.id)}
          loading={usersQuery.isLoading || rolesQuery.isLoading || statusMutation.isPending}
          columns={columns}
          dataSource={records}
          scroll={{ x: 1080 }}
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
        title={roleUser ? `Roles for ${roleUser.nickname || roleUser.username}` : 'User Roles'}
        open={roleUser != null}
        okText="Save"
        confirmLoading={roleMutation.isPending}
        onCancel={() => {
          setRoleUser(null);
          setSelectedRoleIds([]);
        }}
        onOk={() => {
          if (roleUser) {
            roleMutation.mutate({ id: roleUser.id, roleIds: selectedRoleIds });
          }
        }}
      >
        <Select
          mode="multiple"
          className="admin-full-control"
          placeholder="Select roles"
          value={selectedRoleIds}
          options={roleOptions}
          loading={rolesQuery.isLoading}
          onChange={(values) => setSelectedRoleIds(values)}
        />
      </Modal>
    </div>
  );
}
