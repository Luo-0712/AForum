import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Segmented, Select, Space, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  createAdminBoard,
  createAdminCategory,
  deleteAdminBoard,
  deleteAdminCategory,
  listAdminBoards,
  listAdminCategories,
  updateAdminBoard,
  updateAdminCategory,
  type AdminBoard,
  type AdminBoardPayload,
  type AdminCategory,
  type AdminCategoryPayload,
} from '../../api/adminManagement';
import type { Numeric } from '../../api/posts';

type ResourceMode = 'boards' | 'categories';
type FormValues = AdminBoardPayload & AdminCategoryPayload;

function toId(value: Numeric) {
  return String(value);
}

function statusTag(status: number) {
  return status === 0 ? <Tag color="green">Enabled</Tag> : <Tag>Disabled</Tag>;
}

function cleanPayload(values: FormValues): FormValues {
  return {
    name: values.name.trim(),
    code: values.code.trim(),
    description: values.description?.trim() || undefined,
    icon: values.icon?.trim() || undefined,
    parentId: values.parentId ?? 0,
    sort: Number(values.sort ?? 0),
    status: Number(values.status ?? 0),
  };
}

export default function BoardCategoryManagement() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<FormValues>();
  const [mode, setMode] = useState<ResourceMode>('boards');
  const [editingBoard, setEditingBoard] = useState<AdminBoard | null>(null);
  const [editingCategory, setEditingCategory] = useState<AdminCategory | null>(null);
  const modalOpen = editingBoard !== null || editingCategory !== null;

  const boardsQuery = useQuery({
    queryKey: ['admin-management', 'boards'],
    queryFn: listAdminBoards,
  });

  const categoriesQuery = useQuery({
    queryKey: ['admin-management', 'categories'],
    queryFn: listAdminCategories,
  });

  const boards = boardsQuery.data?.data ?? [];
  const categories = categoriesQuery.data?.data ?? [];

  const refreshTaxonomy = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['admin-management', 'boards'] }),
      queryClient.invalidateQueries({ queryKey: ['admin-management', 'categories'] }),
      queryClient.invalidateQueries({ queryKey: ['admin-management', 'overview'] }),
    ]);
  };

  const saveBoardMutation = useMutation({
    mutationFn: (values: FormValues) => {
      const payload = cleanPayload(values);
      return editingBoard?.id ? updateAdminBoard(editingBoard.id, payload) : createAdminBoard(payload);
    },
    onSuccess: async (response) => {
      if (response.code === 200) {
        message.success('Board saved');
        closeModal();
        await refreshTaxonomy();
      } else {
        message.error(response.message || 'Failed to save board');
      }
    },
  });

  const saveCategoryMutation = useMutation({
    mutationFn: (values: FormValues) => {
      const payload = cleanPayload(values);
      return editingCategory?.id ? updateAdminCategory(editingCategory.id, payload) : createAdminCategory(payload);
    },
    onSuccess: async (response) => {
      if (response.code === 200) {
        message.success('Category saved');
        closeModal();
        await refreshTaxonomy();
      } else {
        message.error(response.message || 'Failed to save category');
      }
    },
  });

  const deleteBoardMutation = useMutation({
    mutationFn: deleteAdminBoard,
    onSuccess: async (response) => {
      if (response.code === 200) {
        message.success('Board deleted');
        await refreshTaxonomy();
      } else {
        message.error(response.message || 'Failed to delete board');
      }
    },
  });

  const deleteCategoryMutation = useMutation({
    mutationFn: deleteAdminCategory,
    onSuccess: async (response) => {
      if (response.code === 200) {
        message.success('Category deleted');
        await refreshTaxonomy();
      } else {
        message.error(response.message || 'Failed to delete category');
      }
    },
  });

  const closeModal = () => {
    setEditingBoard(null);
    setEditingCategory(null);
    form.resetFields();
  };

  const openCreate = () => {
    if (mode === 'boards') {
      setEditingBoard({ id: '', name: '', code: '', sort: 0, status: 0, postCount: 0 });
    } else {
      setEditingCategory({ id: '', name: '', code: '', sort: 0, status: 0, postCount: 0, parentId: 0 });
    }
    form.setFieldsValue({ name: '', code: '', description: '', icon: '', parentId: 0, sort: 0, status: 0 });
  };

  const openBoard = (record: AdminBoard) => {
    setMode('boards');
    setEditingBoard(record);
    form.setFieldsValue(record);
  };

  const openCategory = (record: AdminCategory) => {
    setMode('categories');
    setEditingCategory(record);
    form.setFieldsValue({ ...record, parentId: record.parentId ?? 0 });
  };

  const categoryParentOptions = useMemo(
    () => [
      { label: 'Top Level', value: 0 },
      ...categories
        .filter((category) => !editingCategory?.id || toId(category.id) !== toId(editingCategory.id))
        .map((category) => ({ label: category.name, value: Number(category.id) })),
    ],
    [categories, editingCategory?.id],
  );

  const boardColumns: ColumnsType<AdminBoard> = [
    {
      title: 'Board',
      dataIndex: 'name',
      key: 'name',
      render: (value: string, record) => (
        <Space direction="vertical" size={0}>
          <strong>{value}</strong>
          <span className="rbac-code">{record.code}</span>
        </Space>
      ),
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (value?: string) => value || '-',
    },
    {
      title: 'Posts',
      dataIndex: 'postCount',
      key: 'postCount',
      width: 100,
    },
    {
      title: 'Sort',
      dataIndex: 'sort',
      key: 'sort',
      width: 90,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (value: number) => statusTag(value),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 160,
      render: (_value, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openBoard(record)} />
          <Popconfirm title="Delete this board?" onConfirm={() => deleteBoardMutation.mutate(record.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const categoryColumns: ColumnsType<AdminCategory> = [
    {
      title: 'Category',
      dataIndex: 'name',
      key: 'name',
      render: (value: string, record) => (
        <Space direction="vertical" size={0}>
          <strong>{value}</strong>
          <span className="rbac-code">{record.code}</span>
        </Space>
      ),
    },
    {
      title: 'Parent',
      dataIndex: 'parentId',
      key: 'parentId',
      width: 160,
      render: (value?: Numeric) => categories.find((category) => toId(category.id) === String(value))?.name || 'Top Level',
    },
    {
      title: 'Posts',
      dataIndex: 'postCount',
      key: 'postCount',
      width: 100,
      render: (value: Numeric) => Number(value ?? 0),
    },
    {
      title: 'Sort',
      dataIndex: 'sort',
      key: 'sort',
      width: 90,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (value: number) => statusTag(value),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 160,
      render: (_value, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openCategory(record)} />
          <Popconfirm title="Delete this category?" onConfirm={() => deleteCategoryMutation.mutate(record.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const saving = saveBoardMutation.isPending || saveCategoryMutation.isPending;
  const deleting = deleteBoardMutation.isPending || deleteCategoryMutation.isPending;
  const activeRows = mode === 'boards' ? boards : categories;

  return (
    <div className="admin-management-page">
      <div className="page-heading">
        <div>
          <span className="eyebrow">Structure</span>
          <h1>Boards & Categories</h1>
          <p>Maintain forum navigation, posting destinations, and taxonomy status.</p>
        </div>
        <div className="page-toolbar admin-filterbar">
          <Segmented
            value={mode}
            options={[
              { label: 'Boards', value: 'boards' },
              { label: 'Categories', value: 'categories' },
            ]}
            onChange={(value) => setMode(value as ResourceMode)}
          />
          <Button icon={<ReloadOutlined />} onClick={() => void refreshTaxonomy()}>
            Refresh
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            New
          </Button>
        </div>
      </div>

      <section className="section-card admin-table-card">
        <Table<AdminBoard | AdminCategory>
          rowKey={(record) => toId(record.id)}
          loading={boardsQuery.isLoading || categoriesQuery.isLoading || deleting}
          columns={(mode === 'boards' ? boardColumns : categoryColumns) as ColumnsType<AdminBoard | AdminCategory>}
          dataSource={activeRows}
          scroll={{ x: 880 }}
          pagination={{ pageSize: 10 }}
        />
      </section>

      <Modal
        title={mode === 'boards' ? 'Board' : 'Category'}
        open={modalOpen}
        okText="Save"
        confirmLoading={saving}
        onCancel={closeModal}
        onOk={() => form.submit()}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={(values) => (mode === 'boards' ? saveBoardMutation.mutate(values) : saveCategoryMutation.mutate(values))}
        >
          <Form.Item name="name" label="Name" rules={[{ required: true, message: 'Name is required' }]}>
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item name="code" label="Code" rules={[{ required: true, message: 'Code is required' }]}>
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={3} maxLength={200} showCount />
          </Form.Item>
          <Form.Item name="icon" label="Icon">
            <Input maxLength={100} />
          </Form.Item>
          {mode === 'categories' && (
            <Form.Item name="parentId" label="Parent">
              <Select options={categoryParentOptions} />
            </Form.Item>
          )}
          <Space className="admin-modal-row" align="start">
            <Form.Item name="sort" label="Sort">
              <InputNumber min={0} precision={0} />
            </Form.Item>
            <Form.Item name="status" label="Status">
              <Select
                options={[
                  { label: 'Enabled', value: 0 },
                  { label: 'Disabled', value: 1 },
                ]}
              />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
}
