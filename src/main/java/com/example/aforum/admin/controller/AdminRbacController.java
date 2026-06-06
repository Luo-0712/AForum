package com.example.aforum.admin.controller;

import com.example.aforum.admin.dto.CreateRoleDTO;
import com.example.aforum.admin.dto.UpdateRoleDTO;
import com.example.aforum.admin.dto.UpdateRolePermissionsDTO;
import com.example.aforum.admin.dto.UpdateRoleStatusDTO;
import com.example.aforum.admin.dto.UpdateUserRolesDTO;
import com.example.aforum.admin.service.AdminRbacService;
import com.example.aforum.admin.vo.PermissionVO;
import com.example.aforum.admin.vo.RoleVO;
import com.example.aforum.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminRbacController {

    private final AdminRbacService adminRbacService;

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<List<RoleVO>> listRoles() {
        return Result.success(adminRbacService.listRoles());
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<RoleVO> getRole(@PathVariable Long id) {
        return Result.success(adminRbacService.getRole(id));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<RoleVO> createRole(@Valid @RequestBody CreateRoleDTO dto) {
        return Result.success(adminRbacService.createRole(dto));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<RoleVO> updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleDTO dto) {
        return Result.success(adminRbacService.updateRole(id, dto));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<RoleVO> updateRolePermissions(@PathVariable Long id,
                                                @Valid @RequestBody UpdateRolePermissionsDTO dto) {
        return Result.success(adminRbacService.updateRolePermissions(id, dto));
    }

    @PutMapping("/roles/{id}/status")
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<RoleVO> updateRoleStatus(@PathVariable Long id, @Valid @RequestBody UpdateRoleStatusDTO dto) {
        return Result.success(adminRbacService.updateRoleStatus(id, dto));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAnyAuthority('permission:manage', 'role:manage')")
    public Result<List<PermissionVO>> listPermissions() {
        return Result.success(adminRbacService.listPermissions());
    }

    @GetMapping("/users/{id}/roles")
    @PreAuthorize("hasAnyAuthority('user:list', 'user:edit')")
    public Result<List<RoleVO>> getUserRoles(@PathVariable Long id) {
        return Result.success(adminRbacService.getUserRoles(id));
    }

    @PutMapping("/users/{id}/roles")
    @PreAuthorize("hasAuthority('user:edit')")
    public Result<List<RoleVO>> updateUserRoles(@PathVariable Long id, @Valid @RequestBody UpdateUserRolesDTO dto) {
        return Result.success(adminRbacService.updateUserRoles(id, dto));
    }
}
