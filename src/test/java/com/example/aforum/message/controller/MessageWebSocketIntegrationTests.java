package com.example.aforum.message.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MessageWebSocketIntegrationTests {

    private static final String PASSWORD = "password123";

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    @Test
    void subscribedUserReceivesRealtimeNotification() throws Exception {
        AuthSession author = registerAndGetSession("ws-author");
        AuthSession commenter = registerAndGetSession("ws-commenter");
        String postId = createPost(author.token(), "WebSocket " + UUID.randomUUID(), "Post for realtime push.")
                .path("data")
                .path("id")
                .asText();

        CompletableFuture<Map<String, Object>> notificationFuture = new CompletableFuture<>();
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        StompSession session = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws?token=" + author.token(),
                new StompSessionHandlerAdapter() {
                }
        ).get(5, TimeUnit.SECONDS);
        assertThat(simpUserRegistry.getUser(author.userId())).isNotNull();
        session.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                notificationFuture.complete((Map<String, Object>) payload);
            }
        });
        Thread.sleep(1500L);

        createComment(commenter.token(), postId, "触发实时通知。");

        Map<String, Object> message = notificationFuture.get(8, TimeUnit.SECONDS);
        assertThat(message.get("type")).isEqualTo("notification.created");
        assertThat(Long.parseLong(String.valueOf(message.get("unreadNotificationCount")))).isGreaterThanOrEqualTo(1);

        session.disconnect();
        stompClient.stop();
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

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private record AuthSession(String token, String userId) {
    }
}
