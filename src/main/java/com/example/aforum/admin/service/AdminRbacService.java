package com.example.aforum.admin.service;

import com.example.aforum.admin.dto.CreateRoleDTO;
import com.example.aforum.admin.dto.UpdateRoleDTO;
import com.example.aforum.admin.dto.UpdateRolePermissionsDTO;
import com.example.aforum.admin.dto.UpdateRoleStatusDTO;
import com.example.aforum.admin.dto.UpdateUserRolesDTO;
import com.example.aforum.admin.vo.PermissionVO;
import com.example.aforum.admin.vo.RoleVO;

import java.util.List;

public interface AdminRbacService {

    List<RoleVO> listRoles();

    RoleVO getRole(Long id);

    RoleVO createRole(CreateRoleDTO dto);

    RoleVO updateRole(Long id, UpdateRoleDTO dto);

    RoleVO updateRolePermissions(Long id, UpdateRolePermissionsDTO dto);

    RoleVO updateRoleStatus(Long id, UpdateRoleStatusDTO dto);

    List<PermissionVO> listPermissions();

    List<RoleVO> getUserRoles(Long userId);

    List<RoleVO> updateUserRoles(Long userId, UpdateUserRolesDTO dto);
}
