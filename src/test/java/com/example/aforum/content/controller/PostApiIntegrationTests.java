package com.example.aforum.content.controller;

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
class PostApiIntegrationTests {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authenticatedUserCanCreatePostAndPublicListCanReadIt() throws Exception {
        String token = registerAndGetToken("poster");
        String title = "Spring MVP 验收帖 " + UUID.randomUUID();

        JsonNode created = createPost(token, title, "这是一个用于 MVP 回归的正常帖子内容。");

        assertThat(created.path("code").asInt()).isEqualTo(200);
        assertThat(created.path("data").path("status").asInt()).isEqualTo(1);
        assertThat(created.path("data").path("id").asText()).isNotBlank();

        JsonNode list = readJson(mockMvc.perform(get("/api/posts")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("keyword", title))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(list.path("code").asInt()).isEqualTo(200);
        assertThat(list.path("data").path("records"))
                .anySatisfy(record -> assertThat(record.path("title").asText()).isEqualTo(title));
    }

    @Test
    void publicCanReadPostDetailWithoutLogin() throws Exception {
        String token = registerAndGetToken("reader-seed");
        String title = "公开详情验收帖 " + UUID.randomUUID();
        String postId = createPost(token, title, "公开详情页应该允许未登录用户访问。")
                .path("data")
                .path("id")
                .asText();

        JsonNode detail = readJson(mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(detail.path("code").asInt()).isEqualTo(200);
        assertThat(detail.path("data").path("title").asText()).isEqualTo(title);
    }

    @Test
    void authenticatedUserCanCreateComment() throws Exception {
        String token = registerAndGetToken("commenter");
        String postId = createPost(token, "评论验收帖 " + UUID.randomUUID(), "这个帖子用于验证评论创建。")
                .path("data")
                .path("id")
                .asText();

        JsonNode comment = readJson(mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "这是一条正常评论。"))))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(comment.path("code").asInt()).isEqualTo(200);
        assertThat(comment.path("data").path("status").asInt()).isEqualTo(0);
        assertThat(comment.path("data").path("content").asText()).isEqualTo("这是一条正常评论。");
    }

    @Test
    void blockedTermPostIsRejectedByMockAudit() throws Exception {
        String token = registerAndGetToken("audit");
        String title = "审核拒绝验收帖 " + UUID.randomUUID();

        JsonNode rejected = createPost(token, title, "这段内容包含 spam，应被 mock 审核拦截。");

        assertThat(rejected.path("code").asInt()).isEqualTo(200);
        assertThat(rejected.path("data").path("status").asInt()).isEqualTo(3);
        assertThat(rejected.path("data").path("auditReason").asText()).contains("风控词");

        String postId = rejected.path("data").path("id").asText();
        JsonNode hiddenDetail = readJson(mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(hiddenDetail.path("code").asInt()).isEqualTo(404);
    }

    private String registerAndGetToken(String prefix) throws Exception {
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
        return response.path("data").path("accessToken").asText();
    }

    private JsonNode createPost(String token, String title, String content) throws Exception {
        return readJson(mockMvc.perform(post("/api/posts")
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
}
