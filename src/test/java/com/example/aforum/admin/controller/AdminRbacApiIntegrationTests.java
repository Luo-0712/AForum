package com.example.aforum.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aforum.auth.entity.Permission;
import com.example.aforum.auth.entity.Role;
import com.example.aforum.auth.entity.RolePermission;
import com.example.aforum.auth.entity.UserRole;
import com.example.aforum.auth.mapper.PermissionMapper;
import com.example.aforum.auth.mapper.RoleMapper;
import com.example.aforum.auth.mapper.RolePermissionMapper;
import com.example.aforum.auth.mapper.UserRoleMapper;
import com.example.aforum.user.entity.User;
import com.example.aforum.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminRbacApiIntegrationTests {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Test
    void ordinaryUserCannotListRolesAndMeHasNoAdminPermissions() throws Exception {
        TestAccount user = registerAccount("rbac-user");

        mockMvc.perform(get("/api/admin/roles")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isForbidden());

        JsonNode me = readJson(mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(me.path("code").asInt()).isEqualTo(200);
        assertThat(strings(me.path("data").path("roles"))).contains("USER");
        assertThat(strings(me.path("data").path("permissions"))).doesNotContain("role:manage");
    }

    @Test
    void superAdminCanManageRolesPermissionsAndReadPermissionList() throws Exception {
        TestAccount superAdmin = registerAccount("rbac-super");
        assignRole(superAdmin.userId(), "SUPER_ADMIN");

        JsonNode roles = readJson(mockMvc.perform(get("/api/admin/roles")
                        .header("Authorization", "Bearer " + superAdmin.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(roles.path("code").asInt()).isEqualTo(200);
        assertThat(strings(roles.path("data").findValues("code"))).contains("SUPER_ADMIN");

        JsonNode created = readJson(mockMvc.perform(post("/api/admin/roles")
                        .header("Authorization", "Bearer " + superAdmin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "内容审核员",
                                "code", "REVIEWER_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(),
                                "description", "用于RBAC集成测试"))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(created.path("code").asInt()).isEqualTo(200);

        long reviewerRoleId = created.path("data").path("id").asLong();
        long reviewPermissionId = permissionId("post:review");
        JsonNode updated = readJson(mockMvc.perform(put("/api/admin/roles/{id}/permissions", reviewerRoleId)
                        .header("Authorization", "Bearer " + superAdmin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("permissionIds", new Long[]{reviewPermissionId}))))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(updated.path("code").asInt()).isEqualTo(200);
        assertThat(strings(updated.path("data").path("permissionCodes"))).contains("post:review");

        JsonNode permissions = readJson(mockMvc.perform(get("/api/admin/permissions")
                        .header("Authorization", "Bearer " + superAdmin.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(permissions.path("code").asInt()).isEqualTo(200);
        assertThat(strings(permissions.path("data").findValues("code"))).contains("role:manage", "permission:manage");
    }

    @Test
    void adminCanReadUserRolesButCannotAssignSuperAdmin() throws Exception {
        TestAccount admin = registerAccount("rbac-admin");
        assignRole(admin.userId(), "ADMIN");
        TestAccount target = registerAccount("rbac-target");

        JsonNode currentRoles = readJson(mockMvc.perform(get("/api/admin/users/{id}/roles", target.userId())
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(currentRoles.path("code").asInt()).isEqualTo(200);

        JsonNode blocked = readJson(mockMvc.perform(put("/api/admin/users/{id}/roles", target.userId())
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("roleIds", new Long[]{roleId("SUPER_ADMIN")}))))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(blocked.path("code").asInt()).isEqualTo(403);
    }

    @Test
    void disabledRoleStopsGrantingPermissionsOnNextRequest() throws Exception {
        TestAccount temporaryAdmin = registerAccount("rbac-temp");
        Role role = createRoleWithPermission("TEMP_RBAC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(),
                "role:manage");
        assignRole(temporaryAdmin.userId(), role.getCode());

        JsonNode allowed = readJson(mockMvc.perform(get("/api/admin/roles")
                        .header("Authorization", "Bearer " + temporaryAdmin.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(allowed.path("code").asInt()).isEqualTo(200);

        role.setStatus(1);
        roleMapper.updateById(role);

        mockMvc.perform(get("/api/admin/roles")
                        .header("Authorization", "Bearer " + temporaryAdmin.token()))
                .andExpect(status().isForbidden());
    }

    private TestAccount registerAccount(String prefix) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String username = prefix + "-" + suffix.substring(0, 10);
        JsonNode response = readJson(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", PASSWORD,
                                "email", username + "@example.test"))))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(response.path("code").asInt()).isEqualTo(200);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        assertThat(user).isNotNull();
        return new TestAccount(response.path("data").path("accessToken").asText(), user.getId(), username);
    }

    private void assignRole(Long userId, String roleCode) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, roleCode));
        assertThat(role).isNotNull();
        Long existing = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, role.getId()));
        if (existing == 0) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }
    }

    private Role createRoleWithPermission(String roleCode, String permissionCode) {
        Role role = new Role();
        role.setName(roleCode);
        role.setCode(roleCode);
        role.setDescription("temporary test role");
        role.setStatus(0);
        roleMapper.insert(role);

        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(role.getId());
        rolePermission.setPermissionId(permissionId(permissionCode));
        rolePermissionMapper.insert(rolePermission);
        return role;
    }

    private Long roleId(String code) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, code));
        assertThat(role).isNotNull();
        return role.getId();
    }

    private Long permissionId(String code) {
        Permission permission = permissionMapper.selectOne(new LambdaQueryWrapper<Permission>().eq(Permission::getCode, code));
        assertThat(permission).isNotNull();
        return permission.getId();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private java.util.List<String> strings(Iterable<JsonNode> nodes) {
        return StreamSupport.stream(nodes.spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }

    private record TestAccount(String token, Long userId, String username) {
    }
}
