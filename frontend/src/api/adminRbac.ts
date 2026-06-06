import api from './axios';

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export type Numeric = number | string;

export interface Role {
  id: Numeric;
  name: string;
  code: string;
  description?: string;
  status: number;
  permissionIds: Numeric[];
  permissionCodes: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface Permission {
  id: Numeric;
  name: string;
  code: string;
  resourceType?: string;
  url?: string;
  parentId?: Numeric;
  sort?: number;
  status: number;
}

export interface RolePayload {
  name: string;
  code: string;
  description?: string;
}

export function listRoles() {
  return api.get<unknown, ApiResponse<Role[]>>('/admin/roles');
}

export function createRole(payload: RolePayload) {
  return api.post<unknown, ApiResponse<Role>>('/admin/roles', payload);
}

export function updateRole(id: Numeric, payload: RolePayload) {
  return api.put<unknown, ApiResponse<Role>>(`/admin/roles/${id}`, payload);
}

export function updateRoleStatus(id: Numeric, status: number) {
  return api.put<unknown, ApiResponse<Role>>(`/admin/roles/${id}/status`, { status });
}

export function updateRolePermissions(id: Numeric, permissionIds: Numeric[]) {
  return api.put<unknown, ApiResponse<Role>>(`/admin/roles/${id}/permissions`, { permissionIds });
}

export function listPermissions() {
  return api.get<unknown, ApiResponse<Permission[]>>('/admin/permissions');
}

export function getUserRoles(userId: Numeric) {
  return api.get<unknown, ApiResponse<Role[]>>(`/admin/users/${userId}/roles`);
}

export function updateUserRoles(userId: Numeric, roleIds: Numeric[]) {
  return api.put<unknown, ApiResponse<Role[]>>(`/admin/users/${userId}/roles`, { roleIds });
}
