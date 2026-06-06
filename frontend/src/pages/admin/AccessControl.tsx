import { type ReactNode, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Button,
  Checkbox,
  Empty,
  Form,
  Input,
  InputNumber,
  List,
  Popconfirm,
  Segmented,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import {
  CheckCircleOutlined,
  KeyOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SaveOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import {
  createRole,
  getUserRoles,
  listPermissions,
  listRoles,
  updateRole,
  updateRolePermissions,
  updateRoleStatus,
  updateUserRoles,
  type Numeric,
  type Permission,
  type Role,
  type RolePayload,
} from '../../api/adminRbac';
import { useAuthStore } from '../../store/auth';

type RoleFormValues = RolePayload;
type AccessMode = 'roles' | 'permissions' | 'users';
type AccessModeOption = {
  label: string;
  value: AccessMode;
  icon: ReactNode;
};

const RESOURCE_LABELS: Record<string, string> = {
  menu: '菜单',
  api: '接口',
  button: '按钮',
};
const EMPTY_ROLES: Role[] = [];
const EMPTY_PERMISSIONS: Permission[] = [];

function toId(value: Numeric) {
  return String(value);
}

function roleStatusLabel(status: number) {
  return status === 0 ? '启用' : '禁用';
}

function groupPermissions(permissions: Permission[]) {
  return permissions.reduce<Record<string, Permission[]>>((groups, permission) => {
    const key = permission.resourceType || 'other';
    groups[key] = groups[key] || [];
    groups[key].push(permission);
    return groups;
  }, {});
}

export default function AccessControl() {
  const user = useAuthStore((state) => state.user);
  const [roleForm] = Form.useForm<RoleFormValues>();
  const queryClient = useQueryClient();
  const currentRoles = user?.roles ?? [];
  const currentPermissions = user?.permissions ?? [];
  const isSuperAdmin = currentRoles.includes('SUPER_ADMIN');
  const hasPermission = (permission: string) => isSuperAdmin || currentPermissions.includes(permission);
  const canManageRoles = hasPermission('role:manage');
  const canReadPermissions = canManageRoles || hasPermission('permission:manage');
  const canManageUserRoles = canManageRoles && hasPermission('user:edit');
  const defaultMode = canManageRoles ? 'roles' : canReadPermissions ? 'permissions' : 'users';
  const [activeMode, setActiveMode] = useState<AccessMode>(defaultMode);
  const [selectedRoleId, setSelectedRoleId] = useState<string | null>(null);
  const [selectedPermissionIds, setSelectedPermissionIds] = useState<string[]>([]);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [userId, setUserId] = useState<number | null>(null);
  const [loadedUserId, setLoadedUserId] = useState<number | null>(null);
  const [selectedUserRoleIds, setSelectedUserRoleIds] = useState<string[]>([]);

  const rolesQuery = useQuery({
    queryKey: ['admin-rbac', 'roles'],
    queryFn: listRoles,
    enabled: canManageRoles,
  });

  const permissionsQuery = useQuery({
    queryKey: ['admin-rbac', 'permissions'],
    queryFn: listPermissions,
    enabled: canReadPermissions,
  });

  const userRolesQuery = useQuery({
    queryKey: ['admin-rbac', 'user-roles', loadedUserId],
    queryFn: () => getUserRoles(loadedUserId as number),
    enabled: loadedUserId != null && canManageUserRoles,
  });

  const roles = rolesQuery.data?.data ?? EMPTY_ROLES;
  const permissions = permissionsQuery.data?.data ?? EMPTY_PERMISSIONS;
  const selectedRole = roles.find((role) => toId(role.id) === selectedRoleId) ?? null;
  const enabledRoleOptions = roles
    .filter((role) => role.status === 0)
    .map((role) => ({ label: `${role.name} (${role.code})`, value: toId(role.id) }));
  const groupedPermissions = useMemo(() => groupPermissions(permissions), [permissions]);
  const activePermissionCount = permissions.filter((permission) => permission.status === 0).length;
  const activeRoleCount = roles.filter((role) => role.status === 0).length;
  const availableModes = useMemo(() => {
    const modes: AccessModeOption[] = [];
    if (canManageRoles) {
      modes.push({ label: '角色', value: 'roles', icon: <SafetyCertificateOutlined /> });
    }
    if (canReadPermissions) {
      modes.push({ label: '权限', value: 'permissions', icon: <KeyOutlined /> });
    }
    if (canManageUserRoles) {
      modes.push({ label: '用户角色', value: 'users', icon: <TeamOutlined /> });
    }
    return modes;
  }, [canManageRoles, canManageUserRoles, canReadPermissions]);
  const effectiveMode = availableModes.some((mode) => mode.value === activeMode) ? activeMode : availableModes[0]?.value;

  const refreshRoles = () => queryClient.invalidateQueries({ queryKey: ['admin-rbac', 'roles'] });
  const refreshPermissions = () => queryClient.invalidateQueries({ queryKey: ['admin-rbac', 'permissions'] });
  const refreshAccessData = async () => {
    const tasks = [];
    if (canManageRoles) {
      tasks.push(refreshRoles());
    }
    if (canReadPermissions) {
      tasks.push(refreshPermissions());
    }
    await Promise.all(tasks);
  };

  const saveRoleMutation = useMutation({
    mutationFn: (values: RoleFormValues) => {
      const payload = {
        name: values.name.trim(),
        code: values.code.trim().toUpperCase(),
        description: values.description?.trim() || undefined,
      };
      return editingRole ? updateRole(editingRole.id, payload) : createRole(payload);
    },
    onSuccess: async (response) => {
      if (response.code === 200) {
        message.success(editingRole ? '角色已更新' : '角色已创建');
        setEditingRole(response.data);
        setSelectedRoleId(toId(response.data.id));
        setSelectedPermissionIds((response.data.permissionIds ?? []).map(toId));
        await refreshRoles();
      } else {
        message.error(response.message || '保存角色失败');
      }
    },
  });

  const statusMutation = useMutation({
    mutationFn: ({ role, status }: { role: Role; status: number }) => updateRoleStatus(role.id, status),
    onSuccess: async (response) => {
      if (response.code === 200) {
        message.success(`角色已${roleStatusLabel(response.data.status)}`);
        await refreshRoles();
      } else {
        message.error(response.message || '更新角色状态失败');
      }
    },
  });

  const permissionMutation = useMutation({
    mutationFn: () => {
      if (!selectedRole) {
        throw new Error('请选择角色');
      }
      return updateRolePermissions(selectedRole.id, selectedPermissionIds);
    },
    onSuccess: async (response) => {
      if (response.code === 200) {
        message.success('角色权限已保存');
        await refreshRoles();
      } else {
        message.error(response.message || '保存角色权限失败');
      }
    },
  });

  const saveUserRolesMutation = useMutation({
    mutationFn: () => {
      if (loadedUserId == null) {
        throw new Error('请先加载用户');
      }
      return updateUserRoles(loadedUserId, selectedUserRoleIds);
    },
    onSuccess: async (response) => {
      if (response.code === 200) {
        message.success('用户角色已保存');
        await queryClient.invalidateQueries({ queryKey: ['admin-rbac', 'user-roles', loadedUserId] });
      } else {
        message.error(response.message || '保存用户角色失败');
      }
    },
  });

  const startCreateRole = () => {
    setEditingRole(null);
    setSelectedRoleId(null);
    setSelectedPermissionIds([]);
    roleForm.resetFields();
  };

  const selectRole = (role: Role) => {
    setEditingRole(role);
    setSelectedRoleId(toId(role.id));
    setSelectedPermissionIds((role.permissionIds ?? []).map(toId));
    roleForm.setFieldsValue({
      name: role.name,
      code: role.code,
      description: role.description,
    });
  };

  const loadUserRoles = async () => {
    if (!userId) {
      message.warning('请输入用户 ID');
      return;
    }
    setLoadedUserId(userId);
    const response = await queryClient.fetchQuery({
      queryKey: ['admin-rbac', 'user-roles', userId],
      queryFn: () => getUserRoles(userId),
    });
    if (response.code === 200) {
      setSelectedUserRoleIds((response.data ?? []).map((role) => toId(role.id)));
      message.success('用户角色已加载');
    } else {
      message.error(response.message || '加载用户角色失败');
    }
  };

  const permissionColumns = [
    {
      title: '权限',
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: Permission) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{name}</Typography.Text>
          <Typography.Text type="secondary" className="rbac-code">
            {record.code}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '类型',
      dataIndex: 'resourceType',
      key: 'resourceType',
      width: 96,
      render: (value?: string) => <Tag>{RESOURCE_LABELS[value || ''] || value || '其他'}</Tag>,
    },
    {
      title: '地址',
      dataIndex: 'url',
      key: 'url',
      ellipsis: true,
      render: (value?: string) => value || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 92,
      render: (value: number) => <Tag color={value === 0 ? 'green' : 'default'}>{roleStatusLabel(value)}</Tag>,
    },
  ];

  return (
    <div className="access-page">
      <div className="page-heading">
        <div>
          <span className="eyebrow">Access Control</span>
          <h1>权限管理</h1>
          <p>维护角色、权限绑定和用户角色归属。</p>
        </div>
        <div className="page-toolbar">
          <Button icon={<ReloadOutlined />} onClick={() => void refreshAccessData()}>
            刷新
          </Button>
          {availableModes.length > 0 && (
            <Segmented
              value={effectiveMode}
              onChange={(value) => setActiveMode(value as AccessMode)}
              options={availableModes}
            />
          )}
        </div>
      </div>

      <div className="rbac-metrics">
        <div>
          <span>启用角色</span>
          <strong>{activeRoleCount}</strong>
        </div>
        <div>
          <span>权限点</span>
          <strong>{permissions.length}</strong>
        </div>
        <div>
          <span>启用权限</span>
          <strong>{activePermissionCount}</strong>
        </div>
      </div>

      {availableModes.length === 0 && (
        <section className="section-card rbac-panel">
          <Empty description="当前账号暂无权限管理入口" />
        </section>
      )}

      {effectiveMode === 'roles' && canManageRoles && (
        <div className="rbac-workspace">
          <section className="section-card rbac-panel rbac-role-list">
            <div className="rbac-panel__header">
              <div>
                <h2>角色</h2>
                <p>选择角色后编辑基础信息与权限。</p>
              </div>
              <Button type="primary" onClick={startCreateRole}>
                新建
              </Button>
            </div>
            {rolesQuery.isLoading ? (
              <div className="center-panel">
                <Spin />
              </div>
            ) : roles.length > 0 ? (
              <List
                dataSource={roles}
                renderItem={(role) => (
                  <List.Item
                    className={selectedRoleId === toId(role.id) ? 'rbac-role-item is-active' : 'rbac-role-item'}
                    onClick={() => selectRole(role)}
                    actions={[
                      <Popconfirm
                        key="status"
                        title={`确认${role.status === 0 ? '禁用' : '启用'}该角色？`}
                        onConfirm={(event) => {
                          event?.stopPropagation();
                          statusMutation.mutate({ role, status: role.status === 0 ? 1 : 0 });
                        }}
                      >
                        <Button
                          size="small"
                          type={role.status === 0 ? 'default' : 'primary'}
                          onClick={(event) => event.stopPropagation()}
                        >
                          {role.status === 0 ? '禁用' : '启用'}
                        </Button>
                      </Popconfirm>,
                    ]}
                  >
                    <List.Item.Meta
                      title={
                        <Space wrap>
                          <span>{role.name}</span>
                          <Tag color={role.status === 0 ? 'green' : 'default'}>{roleStatusLabel(role.status)}</Tag>
                        </Space>
                      }
                      description={
                        <Space direction="vertical" size={2}>
                          <span className="rbac-code">{role.code}</span>
                          <span>{role.description || '暂无描述'}</span>
                        </Space>
                      }
                    />
                  </List.Item>
                )}
              />
            ) : (
              <Empty description="暂无角色" />
            )}
          </section>

          <section className="section-card rbac-panel">
            <div className="rbac-panel__header">
              <div>
                <h2>{editingRole ? '编辑角色' : '新建角色'}</h2>
                <p>角色编码会自动转为大写。</p>
              </div>
            </div>
            <Form form={roleForm} layout="vertical" onFinish={(values) => saveRoleMutation.mutate(values)}>
              <Form.Item name="name" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}>
                <Input placeholder="例如：内容审核员" />
              </Form.Item>
              <Form.Item
                name="code"
                label="角色编码"
                rules={[
                  { required: true, message: '请输入角色编码' },
                  { pattern: /^[A-Z][A-Z0-9_]{1,49}$/, message: '请输入大写英文、数字或下划线' },
                ]}
              >
                <Input placeholder="REVIEWER" onChange={(event) => roleForm.setFieldValue('code', event.target.value.toUpperCase())} />
              </Form.Item>
              <Form.Item name="description" label="描述">
                <Input.TextArea rows={3} maxLength={200} showCount placeholder="说明这个角色的职责边界" />
              </Form.Item>
              <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saveRoleMutation.isPending}>
                保存角色
              </Button>
            </Form>
          </section>

          <section className="section-card rbac-panel rbac-permission-picker">
            <div className="rbac-panel__header">
              <div>
                <h2>权限绑定</h2>
                <p>{selectedRole ? `正在配置 ${selectedRole.name}` : '先选择一个角色。'}</p>
              </div>
              <Button
                type="primary"
                icon={<CheckCircleOutlined />}
                disabled={!selectedRole}
                loading={permissionMutation.isPending}
                onClick={() => permissionMutation.mutate()}
              >
                保存权限
              </Button>
            </div>
            {permissionsQuery.isLoading ? (
              <div className="center-panel">
                <Spin />
              </div>
            ) : selectedRole ? (
              <Checkbox.Group value={selectedPermissionIds} onChange={(values) => setSelectedPermissionIds(values.map(String))}>
                <div className="permission-groups">
                  {Object.entries(groupedPermissions).map(([type, items]) => (
                    <div className="permission-group" key={type}>
                      <h3>{RESOURCE_LABELS[type] || '其他'}</h3>
                      <div className="permission-check-grid">
                        {items.map((permission) => (
                          <Checkbox key={toId(permission.id)} value={toId(permission.id)} disabled={permission.status !== 0}>
                            <span>{permission.name}</span>
                            <small>{permission.code}</small>
                          </Checkbox>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </Checkbox.Group>
            ) : (
              <Empty description="请选择角色" />
            )}
          </section>
        </div>
      )}

      {effectiveMode === 'permissions' && canReadPermissions && (
        <section className="section-card rbac-panel">
          <div className="rbac-panel__header">
            <div>
              <h2>权限清单</h2>
              <p>权限点由后端初始化和迁移维护，前端用于核对绑定。</p>
            </div>
          </div>
          <Table
            rowKey={(record) => toId(record.id)}
            loading={permissionsQuery.isLoading}
            columns={permissionColumns}
            dataSource={permissions}
            pagination={{ pageSize: 10 }}
          />
        </section>
      )}

      {effectiveMode === 'users' && canManageUserRoles && (
        <section className="section-card rbac-panel rbac-user-roles">
          <div className="rbac-panel__header">
            <div>
              <h2>用户角色</h2>
              <p>输入用户 ID 后加载并调整其角色。</p>
            </div>
          </div>
          <Space wrap className="rbac-user-loader">
            <InputNumber min={1} placeholder="用户 ID" value={userId} onChange={(value) => setUserId(value ? Number(value) : null)} />
            <Button icon={<ReloadOutlined />} loading={userRolesQuery.isFetching} onClick={loadUserRoles}>
              加载用户角色
            </Button>
          </Space>
          <div className="rbac-user-role-picker">
            {loadedUserId == null ? (
              <Empty description="请输入用户 ID 并加载角色" />
            ) : (
              <>
                <Typography.Title level={4}>用户 #{loadedUserId}</Typography.Title>
                <Checkbox.Group value={selectedUserRoleIds} onChange={(values) => setSelectedUserRoleIds(values.map(String))}>
                  <div className="permission-check-grid">
                    {enabledRoleOptions.map((option) => (
                      <Checkbox key={option.value} value={option.value}>
                        {option.label}
                      </Checkbox>
                    ))}
                  </div>
                </Checkbox.Group>
                <div className="rbac-form-actions">
                  <Button
                    type="primary"
                    icon={<SaveOutlined />}
                    loading={saveUserRolesMutation.isPending}
                    onClick={() => saveUserRolesMutation.mutate()}
                  >
                    保存用户角色
                  </Button>
                </div>
              </>
            )}
          </div>
        </section>
      )}
    </div>
  );
}
