package com.example.aforum.message.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aforum.auth.entity.Role;
import com.example.aforum.auth.entity.UserRole;
import com.example.aforum.auth.mapper.RoleMapper;
import com.example.aforum.auth.mapper.UserRoleMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MessageApiIntegrationTests {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Test
    void commentNotificationCanBeListedMarkedReadAndAllRead() throws Exception {
        AuthSession author = registerAndGetSession("notice-author");
        AuthSession commenter = registerAndGetSession("notice-commenter");
        String postId = createPost(author.token(), "Notice " + UUID.randomUUID(), "Post for notification.")
                .path("data")
                .path("id")
                .asText();

        createComment(commenter.token(), postId, "这是一条通知评论。");

        JsonNode notifications = getWithToken("/api/notifications", author.token());
        JsonNode first = notifications.path("data").path("records").get(0);
        assertThat(first.path("type").asText()).isEqualTo("comment");
        assertThat(first.path("readStatus").asInt()).isEqualTo(0);

        JsonNode unread = getWithToken("/api/notifications/unread-count", author.token());
        long unreadBefore = unread.path("data").path("notificationUnreadCount").asLong();
        assertThat(unreadBefore).isGreaterThanOrEqualTo(1);

        readJson(mockMvc.perform(put("/api/notifications/{id}/read", first.path("id").asText())
                        .header("Authorization", "Bearer " + author.token()))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode afterRead = getWithToken("/api/notifications/unread-count", author.token());
        assertThat(afterRead.path("data").path("notificationUnreadCount").asLong()).isEqualTo(unreadBefore - 1);

        createComment(commenter.token(), postId, "第二条通知评论。");
        readJson(mockMvc.perform(put("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + author.token()))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode afterAllRead = getWithToken("/api/notifications/unread-count", author.token());
        assertThat(afterAllRead.path("data").path("notificationUnreadCount").asLong()).isZero();
    }

    @Test
    void replyAndInteractionEventsCreateNotificationsOnce() throws Exception {
        AuthSession author = registerAndGetSession("event-author");
        AuthSession actor = registerAndGetSession("event-actor");
        AuthSession replier = registerAndGetSession("event-replier");
        String postId = createPost(author.token(), "Events " + UUID.randomUUID(), "Post for events.")
                .path("data")
                .path("id")
                .asText();
        String commentId = createComment(actor.token(), postId, "一条可回复评论。")
                .path("data")
                .path("id")
                .asText();

        postWithToken("/api/posts/" + postId + "/like", actor.token());
        postWithToken("/api/posts/" + postId + "/like", actor.token());
        postWithToken("/api/posts/" + postId + "/favorite", actor.token());
        postWithToken("/api/users/" + author.userId() + "/follow", actor.token());
        createReply(replier.token(), postId, commentId, actor.userId(), "回复评论。");

        JsonNode authorNotifications = getWithToken("/api/notifications", author.token());
        JsonNode authorRecords = authorNotifications.path("data").path("records");
        assertThat(authorRecords).anySatisfy(record -> assertThat(record.path("type").asText()).isEqualTo("like"));
        assertThat(authorRecords).anySatisfy(record -> assertThat(record.path("type").asText()).isEqualTo("favorite"));
        assertThat(authorRecords).anySatisfy(record -> assertThat(record.path("type").asText()).isEqualTo("follow"));
        long likeCount = countType(authorRecords, "like");
        assertThat(likeCount).isEqualTo(1);

        JsonNode actorNotifications = getWithToken("/api/notifications", actor.token());
        assertThat(actorNotifications.path("data").path("records"))
                .anySatisfy(record -> assertThat(record.path("type").asText()).isEqualTo("reply"));
    }

    @Test
    void privateMessageConversationCanBeReadAndMarkedRead() throws Exception {
        AuthSession sender = registerAndGetSession("msg-sender");
        AuthSession receiver = registerAndGetSession("msg-receiver");

        JsonNode sent = readJson(mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + sender.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "receiverId", receiver.userId(),
                                "content", "你好，这是一条私信。"))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(sent.path("data").path("content").asText()).isEqualTo("你好，这是一条私信。");

        JsonNode conversations = getWithToken("/api/messages/conversations", receiver.token());
        JsonNode firstConversation = conversations.path("data").path("records").get(0);
        assertThat(firstConversation.path("peerUserId").asText()).isEqualTo(sender.userId());
        assertThat(firstConversation.path("unreadCount").asInt()).isEqualTo(1);

        JsonNode messages = getWithToken("/api/messages/conversations/" + sender.userId(), receiver.token());
        assertThat(messages.path("data").path("records").get(0).path("content").asText()).isEqualTo("你好，这是一条私信。");

        readJson(mockMvc.perform(put("/api/messages/conversations/{peerUserId}/read", sender.userId())
                        .header("Authorization", "Bearer " + receiver.token()))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode unread = getWithToken("/api/messages/unread-count", receiver.token());
        assertThat(unread.path("data").asLong()).isZero();
    }

    @Test
    void systemNotificationRequiresAdminAndCanBroadcast() throws Exception {
        AuthSession receiver = registerAndGetSession("system-receiver");
        AuthSession normal = registerAndGetSession("system-normal");
        AuthSession admin = registerAndGetSession("system-admin");
        assignRole(Long.valueOf(admin.userId()), "ADMIN");

        mockMvc.perform(post("/api/admin/notifications/system")
                        .header("Authorization", "Bearer " + normal.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "receiverIds", new String[]{receiver.userId()},
                                "title", "系统通知",
                                "content", "普通用户不能发送"))))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(readJson(result).path("code").asInt()).isEqualTo(403));

        readJson(mockMvc.perform(post("/api/admin/notifications/system")
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "receiverIds", new String[]{receiver.userId()},
                                "title", "系统通知",
                                "content", "这是一条系统通知"))))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode notifications = getWithToken("/api/notifications?type=system", receiver.token());
        assertThat(notifications.path("data").path("records"))
                .anySatisfy(record -> assertThat(record.path("title").asText()).isEqualTo("系统通知"));
    }

    @Test
    void anonymousUserCannotReadCommunicationApis() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/messages/conversations"))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode postWithToken(String url, String token) throws Exception {
        return readJson(mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn());
    }

    private JsonNode getWithToken(String url, String token) throws Exception {
        return readJson(mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
    }

    private AuthSession registerAndGetSession(String prefix) throws Exception {
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
        return new AuthSession(
                response.path("data").path("accessToken").asText(),
                response.path("data").path("user").path("id").asText()
        );
    }

    private JsonNode createPost(String token, String title, String content) throws Exception {
        return readJson(mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", title, "content", content))))
                .andExpect(status().isOk())
                .andReturn());
    }

    private JsonNode createComment(String token, String postId, String content) throws Exception {
        return readJson(mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", content))))
                .andExpect(status().isOk())
                .andReturn());
    }

    private JsonNode createReply(String token, String postId, String parentId, String replyUserId, String content) throws Exception {
        return readJson(mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "content", content,
                                "parentId", parentId,
                                "replyUserId", replyUserId))))
                .andExpect(status().isOk())
                .andReturn());
    }

    private long countType(JsonNode records, String type) {
        long count = 0;
        for (JsonNode record : records) {
            if (type.equals(record.path("type").asText())) {
                count++;
            }
        }
        return count;
    }

    private void assignRole(Long userId, String roleCode) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, roleCode));
        assertThat(role).isNotNull();
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private record AuthSession(String token, String userId) {
    }
}
