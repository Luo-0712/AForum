package com.example.aforum.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aforum.auth.entity.Permission;
import com.example.aforum.auth.entity.Role;
import com.example.aforum.auth.entity.RolePermission;
import com.example.aforum.auth.entity.UserRole;
import com.example.aforum.auth.mapper.PermissionMapper;
import com.example.aforum.auth.mapper.RoleMapper;
import com.example.aforum.auth.mapper.RolePermissionMapper;
import com.example.aforum.auth.mapper.UserRoleMapper;
import com.example.aforum.auth.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final int ACTIVE = 0;

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        return getActiveRolesByUserId(userId).stream()
                .map(Role::getCode)
                .distinct()
                .toList();
    }

    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        List<Role> roles = getActiveRolesByUserId(userId);
        if (roles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = roles.stream().map(Role::getId).toList();
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds)
        );
        if (rolePermissions.isEmpty()) {
            return List.of();
        }
        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();
        return permissionMapper.selectBatchIds(permissionIds).stream()
                .filter(permission -> ACTIVE == permission.getStatus())
                .map(Permission::getCode)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthoritiesByUserId(Long userId) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        getRoleCodesByUserId(userId).stream()
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
                .forEach(authorities::add);
        getPermissionCodesByUserId(userId).stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        return authorities;
    }

    @Override
    public boolean hasRole(Long userId, String roleCode) {
        return getRoleCodesByUserId(userId).contains(roleCode);
    }

    private List<Role> getActiveRolesByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
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
        return roleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> ACTIVE == role.getStatus())
                .toList();
    }
}
