package com.example.aforum.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aforum.admin.dto.CreateRoleDTO;
import com.example.aforum.admin.dto.UpdateRoleDTO;
import com.example.aforum.admin.dto.UpdateRolePermissionsDTO;
import com.example.aforum.admin.dto.UpdateRoleStatusDTO;
import com.example.aforum.admin.dto.UpdateUserRolesDTO;
import com.example.aforum.admin.service.AdminRbacService;
import com.example.aforum.admin.vo.PermissionVO;
import com.example.aforum.admin.vo.RoleVO;
import com.example.aforum.auth.entity.Permission;
import com.example.aforum.auth.entity.Role;
import com.example.aforum.auth.entity.RolePermission;
import com.example.aforum.auth.entity.UserRole;
import com.example.aforum.auth.mapper.PermissionMapper;
import com.example.aforum.auth.mapper.RoleMapper;
import com.example.aforum.auth.mapper.RolePermissionMapper;
import com.example.aforum.auth.mapper.UserRoleMapper;
import com.example.aforum.auth.service.AuthorizationService;
import com.example.aforum.common.constant.UserStatus;
import com.example.aforum.common.constant.UserType;
import com.example.aforum.common.exception.BusinessException;
import com.example.aforum.common.result.ErrorCode;
import com.example.aforum.common.util.SecurityUtils;
import com.example.aforum.user.entity.User;
import com.example.aforum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminRbacServiceImpl implements AdminRbacService {

    private static final int ACTIVE = 0;
    private static final int DISABLED = 1;
    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final Set<String> BUILT_IN_ROLE_CODES = Set.of("SUPER_ADMIN", "ADMIN", "USER", "VIP");

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserMapper userMapper;
    private final AuthorizationService authorizationService;

    @Override
    public List<RoleVO> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId))
                .stream()
                .map(this::toRoleVO)
                .toList();
    }

    @Override
    public RoleVO getRole(Long id) {
        return toRoleVO(requireRole(id));
    }

    @Override
    @Transactional
    public RoleVO createRole(CreateRoleDTO dto) {
        String code = normalizeRoleCode(dto.getCode());
        ensureRoleCodeAvailable(code, null);

        Role role = new Role();
        role.setName(dto.getName().trim());
        role.setCode(code);
        role.setDescription(trimToNull(dto.getDescription()));
        role.setStatus(ACTIVE);
        roleMapper.insert(role);
        return toRoleVO(role);
    }

    @Override
    @Transactional
    public RoleVO updateRole(Long id, UpdateRoleDTO dto) {
        Role role = requireRole(id);
        requireCanTouchRole(role);

        String code = normalizeRoleCode(dto.getCode());
        if (BUILT_IN_ROLE_CODES.contains(role.getCode()) && !role.getCode().equals(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "内置角色编码不可修改");
        }
        ensureRoleCodeAvailable(code, role.getId());

        role.setName(dto.getName().trim());
        role.setCode(code);
        role.setDescription(trimToNull(dto.getDescription()));
        roleMapper.updateById(role);
        return toRoleVO(role);
    }

    @Override
    @Transactional
    public RoleVO updateRolePermissions(Long id, UpdateRolePermissionsDTO dto) {
        Role role = requireRole(id);
        requireCanTouchRole(role);
        if (DISABLED == role.getStatus()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "禁用角色不能分配权限");
        }

        List<Long> permissionIds = distinctIds(dto.getPermissionIds());
        List<Permission> permissions = loadEnabledPermissions(permissionIds);

        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, role.getId()));
        for (Permission permission : permissions) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(role.getId());
            rolePermission.setPermissionId(permission.getId());
            rolePermissionMapper.insert(rolePermission);
        }
        return toRoleVO(role);
    }

    @Override
    @Transactional
    public RoleVO updateRoleStatus(Long id, UpdateRoleStatusDTO dto) {
        Role role = requireRole(id);
        requireCanTouchRole(role);
        if (SUPER_ADMIN.equals(role.getCode()) && DISABLED == dto.getStatus()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "超级管理员角色不可禁用");
        }
        role.setStatus(dto.getStatus());
        roleMapper.updateById(role);
        return toRoleVO(role);
    }

    @Override
    public List<PermissionVO> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                        .orderByAsc(Permission::getParentId)
                        .orderByAsc(Permission::getSort)
                        .orderByAsc(Permission::getId))
                .stream()
                .map(this::toPermissionVO)
                .toList();
    }

    @Override
    public List<RoleVO> getUserRoles(Long userId) {
        requireUser(userId);
        return rolesForUser(userId).stream()
                .map(this::toRoleVO)
                .toList();
    }

    @Override
    @Transactional
    public List<RoleVO> updateUserRoles(Long userId, UpdateUserRolesDTO dto) {
        User user = requireUser(userId);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean actingSuperAdmin = authorizationService.hasRole(currentUserId, SUPER_ADMIN);

        List<Long> roleIds = distinctIds(dto.getRoleIds());
        List<Role> requestedRoles = loadEnabledRoles(roleIds);
        boolean assignsSuperAdmin = requestedRoles.stream().anyMatch(role -> SUPER_ADMIN.equals(role.getCode()));
        boolean targetHadSuperAdmin = rolesForUser(userId).stream().anyMatch(role -> SUPER_ADMIN.equals(role.getCode()));

        if ((assignsSuperAdmin || targetHadSuperAdmin) && !actingSuperAdmin) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权分配或修改超级管理员角色");
        }
        if (targetHadSuperAdmin && !assignsSuperAdmin && countActiveSuperAdminUsers() <= 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能移除最后一个超级管理员");
        }

        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (Role role : requestedRoles) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }

        user.setUserType(resolveUserType(requestedRoles));
        userMapper.updateById(user);

        return requestedRoles.stream().map(this::toRoleVO).toList();
    }

    private void requireCanTouchRole(Role role) {
        if (SUPER_ADMIN.equals(role.getCode())
                && !authorizationService.hasRole(SecurityUtils.getCurrentUserId(), SUPER_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改超级管理员角色");
        }
    }

    private Role requireRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void ensureRoleCodeAvailable(String code, Long currentRoleId) {
        Role existing = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, code));
        if (existing != null && !existing.getId().equals(currentRoleId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "角色编码已存在");
        }
    }

    private List<Role> loadEnabledRoles(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.size() != roleIds.size() || roles.stream().anyMatch(role -> ACTIVE != role.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色不存在或已禁用");
        }
        return roles;
    }

    private List<Permission> loadEnabledPermissions(List<Long> permissionIds) {
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        List<Permission> permissions = permissionMapper.selectBatchIds(permissionIds);
        if (permissions.size() != permissionIds.size()
                || permissions.stream().anyMatch(permission -> ACTIVE != permission.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "权限不存在或已禁用");
        }
        return permissions;
    }

    private List<Role> rolesForUser(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .distinct()
                .toList();
        return roleMapper.selectBatchIds(roleIds);
    }

    private long countActiveSuperAdminUsers() {
        Role superAdminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, SUPER_ADMIN).eq(Role::getStatus, ACTIVE)
        );
        if (superAdminRole == null) {
            return 0;
        }
        List<UserRole> superAssignments = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, superAdminRole.getId())
        );
        return superAssignments.stream()
                .map(UserRole::getUserId)
                .distinct()
                .map(userMapper::selectById)
                .filter(user -> user != null && UserStatus.ACTIVE.getCode().equals(user.getStatus()))
                .count();
    }

    private Integer resolveUserType(List<Role> roles) {
        Set<String> codes = roles.stream().map(Role::getCode).collect(java.util.stream.Collectors.toSet());
        if (codes.contains(SUPER_ADMIN)) {
            return UserType.SUPER_ADMIN.getCode();
        }
        if (codes.contains("ADMIN")) {
            return UserType.ADMIN.getCode();
        }
        if (codes.contains("VIP")) {
            return UserType.VIP.getCode();
        }
        return UserType.NORMAL.getCode();
    }

    private RoleVO toRoleVO(Role role) {
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getId())
        );
        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();
        List<String> permissionCodes = permissionIds.isEmpty()
                ? List.of()
                : permissionMapper.selectBatchIds(permissionIds).stream()
                        .map(Permission::getCode)
                        .sorted()
                        .toList();
        return RoleVO.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .description(role.getDescription())
                .status(role.getStatus())
                .permissionIds(permissionIds)
                .permissionCodes(permissionCodes)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    private PermissionVO toPermissionVO(Permission permission) {
        return PermissionVO.builder()
                .id(permission.getId())
                .name(permission.getName())
                .code(permission.getCode())
                .resourceType(permission.getResourceType())
                .url(permission.getUrl())
                .parentId(permission.getParentId())
                .sort(permission.getSort())
                .status(permission.getStatus())
                .build();
    }

    private List<Long> distinctIds(List<Long> ids) {
        return new LinkedHashSet<>(ids).stream().toList();
    }

    private String normalizeRoleCode(String code) {
        return code.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
