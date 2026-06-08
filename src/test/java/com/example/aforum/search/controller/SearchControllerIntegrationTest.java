package com.example.aforum.search.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aforum.auth.entity.Role;
import com.example.aforum.auth.entity.UserRole;
import com.example.aforum.auth.mapper.RoleMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchControllerIntegrationTest {

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
    private UserRoleMapper userRoleMapper;

    @Test
    void publicSearchUsesMysqlFallbackWhenElasticsearchDisabled() throws Exception {
        String token = registerAndGetToken("search-user");
        String unique = "搜索验收 " + UUID.randomUUID();
        createPost(token, unique, "这是一条包含 Spring Elasticsearch 关键词的正常内容。");

        JsonNode response = readJson(mockMvc.perform(get("/api/search")
                        .param("q", unique)
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("sort", "latest"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(response.path("code").asInt()).isEqualTo(200);
        assertThat(response.path("data").path("records"))
                .anySatisfy(record -> assertThat(record.path("title").asText()).isEqualTo(unique));
    }

    @Test
    void publicSearchDoesNotReturnRejectedPosts() throws Exception {
        String token = registerAndGetToken("search-audit");
        String rejectedTitle = "搜索拒绝帖 " + UUID.randomUUID();
        createPost(token, rejectedTitle, "这段内容包含 spam，应被 mock 审核拦截。");

        JsonNode response = readJson(mockMvc.perform(get("/api/search")
                        .param("q", rejectedTitle)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(response.path("code").asInt()).isEqualTo(200);
        assertThat(response.path("data").path("records")).isEmpty();
    }

    @Test
    void publicCanLoadSearchFilterOptions() throws Exception {
        JsonNode response = readJson(mockMvc.perform(get("/api/search/options"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(response.path("code").asInt()).isEqualTo(200);
        assertThat(response.path("data").path("categories")).isNotEmpty();
        assertThat(response.path("data").path("boards")).isNotEmpty();
    }

    @Test
    void adminCanReadDisabledSearchHealthWithPermission() throws Exception {
        TestAccount admin = registerAccount("search-admin");
        assignRole(admin.userId(), "ADMIN");

        JsonNode response = readJson(mockMvc.perform(get("/api/admin/search/health")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(response.path("code").asInt()).isEqualTo(200);
        assertThat(response.path("data").path("enabled").asBoolean()).isFalse();
    }

    private String registerAndGetToken(String prefix) throws Exception {
        return registerAccount(prefix).token();
    }

    private TestAccount registerAccount(String prefix) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String username = prefix + "-" + suffix.substring(0, 12);
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
        return new TestAccount(response.path("data").path("accessToken").asText(), user.getId());
    }

    private void assignRole(Long userId, String roleCode) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, roleCode));
        assertThat(role).isNotNull();
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    private void createPost(String token, String title, String content) throws Exception {
        readJson(mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", title, "content", content))))
                .andExpect(status().isOk())
                .andReturn());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private record TestAccount(String token, Long userId) {
    }
}
