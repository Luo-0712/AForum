package com.example.aforum.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aforum.auth.entity.Role;
import com.example.aforum.auth.entity.UserRole;
import com.example.aforum.auth.mapper.RoleMapper;
import com.example.aforum.auth.mapper.UserRoleMapper;
import com.example.aforum.content.entity.Board;
import com.example.aforum.content.entity.Category;
import com.example.aforum.content.entity.Post;
import com.example.aforum.content.mapper.BoardMapper;
import com.example.aforum.content.mapper.CategoryMapper;
import com.example.aforum.content.mapper.PostMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminManagementApiIntegrationTests {

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

    @Autowired
    private BoardMapper boardMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private PostMapper postMapper;

    @Test
    void ordinaryUserCannotAccessAdminManagementApis() throws Exception {
        TestAccount user = registerAccount("admin-mgmt-user");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/boards")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanManageBoardsCategoriesUsersAndAuditPosts() throws Exception {
        TestAccount admin = registerAccount("admin-mgmt-admin");
        assignRole(admin.userId(), "ADMIN");
        TestAccount author = registerAccount("admin-mgmt-author");

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        JsonNode createdBoard = readJson(mockMvc.perform(post("/api/admin/boards")
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Board " + suffix,
                                "code", "board_" + suffix,
                                "description", "integration board",
                                "sort", 9,
                                "status", 0))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(createdBoard.path("code").asInt()).isEqualTo(200);
        long boardId = createdBoard.path("data").path("id").asLong();

        JsonNode updatedBoard = readJson(mockMvc.perform(put("/api/admin/boards/{id}", boardId)
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Board Updated " + suffix,
                                "code", "board_" + suffix,
                                "description", "updated",
                                "sort", 11,
                                "status", 0))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(updatedBoard.path("data").path("sort").asInt()).isEqualTo(11);

        JsonNode createdCategory = readJson(mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Category " + suffix,
                                "code", "cat_" + suffix,
                                "description", "integration category",
                                "sort", 6,
                                "status", 0))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(createdCategory.path("code").asInt()).isEqualTo(200);
        long categoryId = createdCategory.path("data").path("id").asLong();

        Post pendingPost = createPost(author.userId(), boardId, categoryId, 2, "Pending " + suffix);
        JsonNode pending = readJson(mockMvc.perform(get("/api/admin/audit/pending")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(pending.path("data").path("records").findValues("id").stream().map(JsonNode::asLong))
                .contains(pendingPost.getId());

        JsonNode approved = readJson(mockMvc.perform(put("/api/admin/audit/posts/{id}/approve", pendingPost.getId())
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(approved.path("data").path("status").asInt()).isEqualTo(1);
        assertThat(postMapper.selectById(pendingPost.getId()).getStatus()).isEqualTo(1);
        assertThat(boardMapper.selectById(boardId).getPostCount()).isEqualTo(1);

        JsonNode flagged = readJson(mockMvc.perform(put("/api/admin/posts/{id}/flags", pendingPost.getId())
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("top", true, "essence", true))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(flagged.path("data").path("top").asBoolean()).isTrue();
        assertThat(flagged.path("data").path("essence").asBoolean()).isTrue();

        Post secondPost = createPost(author.userId(), boardId, categoryId, 2, "Rejected " + suffix);
        JsonNode rejected = readJson(mockMvc.perform(put("/api/admin/audit/posts/{id}/reject", secondPost.getId())
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "needs revision"))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(rejected.path("data").path("status").asInt()).isEqualTo(3);
        assertThat(rejected.path("data").path("auditReason").asText()).contains("needs revision");

        JsonNode users = readJson(mockMvc.perform(get("/api/admin/users")
                        .param("keyword", author.username())
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(users.path("data").path("records").findValues("username").stream().map(JsonNode::asText))
                .contains(author.username());

        JsonNode disabled = readJson(mockMvc.perform(put("/api/admin/users/{id}/status", author.userId())
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", 1))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(disabled.path("data").path("status").asInt()).isEqualTo(1);

        JsonNode overview = readJson(mockMvc.perform(get("/api/admin/stats/overview")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(overview.path("data").path("userCount").asLong()).isGreaterThan(0);
        assertThat(overview.path("data").path("pendingPostCount").asLong()).isGreaterThanOrEqualTo(0);

        JsonNode trends = readJson(mockMvc.perform(get("/api/admin/stats/trends")
                        .param("days", "7")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(trends.path("data")).hasSize(7);
        assertThat(trends.path("data").get(0).path("date").asText()).isNotBlank();

        mockMvc.perform(delete("/api/admin/posts/{id}", secondPost.getId())
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk());
        assertThat(postMapper.selectById(secondPost.getId())).isNull();
    }

    @Test
    void boardAndCategoryDeletionRejectsReferencedRecords() throws Exception {
        TestAccount admin = registerAccount("admin-mgmt-delete");
        assignRole(admin.userId(), "ADMIN");
        TestAccount author = registerAccount("admin-mgmt-delete-author");
        Board board = createBoard("used_board_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        Category category = createCategory("used_cat_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        createPost(author.userId(), board.getId(), category.getId(), 1, "Referenced post");

        JsonNode boardDelete = readJson(mockMvc.perform(delete("/api/admin/boards/{id}", board.getId())
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(boardDelete.path("code").asInt()).isEqualTo(409);

        JsonNode categoryDelete = readJson(mockMvc.perform(delete("/api/admin/categories/{id}", category.getId())
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(categoryDelete.path("code").asInt()).isEqualTo(409);
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

    private Post createPost(Long userId, Long boardId, Long categoryId, int status, String title) {
        Post post = new Post();
        post.setTitle(title);
        post.setContent("content for " + title);
        post.setSummary("summary for " + title);
        post.setUserId(userId);
        post.setBoardId(boardId);
        post.setCategoryId(categoryId);
        post.setStatus(status);
        post.setTop(false);
        post.setEssence(false);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setFavoriteCount(0);
        postMapper.insert(post);
        return post;
    }

    private Board createBoard(String code) {
        Board board = new Board();
        board.setName(code);
        board.setCode(code);
        board.setDescription(code);
        board.setSort(1);
        board.setStatus(0);
        board.setPostCount(0);
        boardMapper.insert(board);
        return board;
    }

    private Category createCategory(String code) {
        Category category = new Category();
        category.setName(code);
        category.setCode(code);
        category.setDescription(code);
        category.setParentId(0L);
        category.setSort(1);
        category.setStatus(0);
        categoryMapper.insert(category);
        return category;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private record TestAccount(String token, Long userId, String username) {
    }
}
