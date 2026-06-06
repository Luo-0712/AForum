package com.example.aforum.interaction.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InteractionApiIntegrationTests {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postLikeIsIdempotentAndRestorable() throws Exception {
        AuthSession author = registerAndGetSession("like-author");
        AuthSession liker = registerAndGetSession("like-user");
        String postId = createPost(author.token(), "Like flow " + UUID.randomUUID(), "Post for like flow.")
                .path("data")
                .path("id")
                .asText();

        JsonNode firstLike = postWithToken("/api/posts/" + postId + "/like", liker.token());
        JsonNode secondLike = postWithToken("/api/posts/" + postId + "/like", liker.token());
        assertThat(firstLike.path("data").path("active").asBoolean()).isTrue();
        assertThat(firstLike.path("data").path("count").asInt()).isEqualTo(1);
        assertThat(secondLike.path("data").path("count").asInt()).isEqualTo(1);

        JsonNode authedDetail = readJson(mockMvc.perform(get("/api/posts/{id}", postId)
                        .header("Authorization", "Bearer " + liker.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(authedDetail.path("data").path("liked").asBoolean()).isTrue();
        assertThat(authedDetail.path("data").path("favorited").asBoolean()).isFalse();

        JsonNode firstUnlike = deleteWithToken("/api/posts/" + postId + "/like", liker.token());
        JsonNode secondUnlike = deleteWithToken("/api/posts/" + postId + "/like", liker.token());
        assertThat(firstUnlike.path("data").path("active").asBoolean()).isFalse();
        assertThat(firstUnlike.path("data").path("count").asInt()).isEqualTo(0);
        assertThat(secondUnlike.path("data").path("count").asInt()).isEqualTo(0);

        JsonNode restoredLike = postWithToken("/api/posts/" + postId + "/like", liker.token());
        assertThat(restoredLike.path("data").path("count").asInt()).isEqualTo(1);

        JsonNode anonymousDetail = readJson(mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(anonymousDetail.path("data").path("liked").asBoolean()).isFalse();
        assertThat(anonymousDetail.path("data").path("likeCount").asInt()).isEqualTo(1);
    }

    @Test
    void commentLikeReturnsCurrentUserState() throws Exception {
        AuthSession author = registerAndGetSession("comment-author");
        AuthSession liker = registerAndGetSession("comment-liker");
        String postId = createPost(author.token(), "Comment like " + UUID.randomUUID(), "Post for comment like.")
                .path("data")
                .path("id")
                .asText();
        String commentId = createComment(author.token(), postId, "Comment to like.")
                .path("data")
                .path("id")
                .asText();

        JsonNode liked = postWithToken("/api/comments/" + commentId + "/like", liker.token());
        assertThat(liked.path("data").path("active").asBoolean()).isTrue();
        assertThat(liked.path("data").path("count").asInt()).isEqualTo(1);

        JsonNode authedComments = readJson(mockMvc.perform(get("/api/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + liker.token()))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode comment = authedComments.path("data").get(0);
        assertThat(comment.path("liked").asBoolean()).isTrue();
        assertThat(comment.path("likeCount").asInt()).isEqualTo(1);

        JsonNode anonymousComments = readJson(mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(anonymousComments.path("data").get(0).path("liked").asBoolean()).isFalse();

        JsonNode unliked = deleteWithToken("/api/comments/" + commentId + "/like", liker.token());
        assertThat(unliked.path("data").path("active").asBoolean()).isFalse();
        assertThat(unliked.path("data").path("count").asInt()).isEqualTo(0);
    }

    @Test
    void favoritePostAppearsInProfileListAndCanBeRemoved() throws Exception {
        AuthSession author = registerAndGetSession("favorite-author");
        AuthSession collector = registerAndGetSession("favorite-user");
        String title = "Favorite flow " + UUID.randomUUID();
        String postId = createPost(author.token(), title, "Post for favorite flow.")
                .path("data")
                .path("id")
                .asText();

        JsonNode favorite = postWithToken("/api/posts/" + postId + "/favorite", collector.token());
        assertThat(favorite.path("data").path("active").asBoolean()).isTrue();
        assertThat(favorite.path("data").path("count").asInt()).isEqualTo(1);

        JsonNode favorites = readJson(mockMvc.perform(get("/api/users/{id}/favorites", collector.userId())
                        .header("Authorization", "Bearer " + collector.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(favorites.path("data").path("records"))
                .anySatisfy(record -> {
                    assertThat(record.path("id").asText()).isEqualTo(postId);
                    assertThat(record.path("favorited").asBoolean()).isTrue();
                });

        JsonNode profile = readJson(mockMvc.perform(get("/api/users/{id}", collector.userId())
                        .header("Authorization", "Bearer " + collector.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(profile.path("data").path("favoriteCount").asInt()).isEqualTo(1);

        JsonNode unfavorite = deleteWithToken("/api/posts/" + postId + "/favorite", collector.token());
        assertThat(unfavorite.path("data").path("active").asBoolean()).isFalse();
        assertThat(unfavorite.path("data").path("count").asInt()).isEqualTo(0);
    }

    @Test
    void followListsProfileStateAndSelfFollowGuard() throws Exception {
        AuthSession follower = registerAndGetSession("follower");
        AuthSession target = registerAndGetSession("target");

        JsonNode selfFollow = postWithToken("/api/users/" + follower.userId() + "/follow", follower.token());
        assertThat(selfFollow.path("code").asInt()).isEqualTo(400);

        JsonNode follow = postWithToken("/api/users/" + target.userId() + "/follow", follower.token());
        JsonNode followAgain = postWithToken("/api/users/" + target.userId() + "/follow", follower.token());
        assertThat(follow.path("data").path("following").asBoolean()).isTrue();
        assertThat(follow.path("data").path("followerCount").asInt()).isEqualTo(1);
        assertThat(follow.path("data").path("followingCount").asInt()).isEqualTo(1);
        assertThat(followAgain.path("data").path("followerCount").asInt()).isEqualTo(1);

        JsonNode targetProfile = readJson(mockMvc.perform(get("/api/users/{id}", target.userId())
                        .header("Authorization", "Bearer " + follower.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(targetProfile.path("data").path("followedByCurrentUser").asBoolean()).isTrue();
        assertThat(targetProfile.path("data").path("followerCount").asInt()).isEqualTo(1);

        JsonNode following = readJson(mockMvc.perform(get("/api/users/{id}/following", follower.userId())
                        .header("Authorization", "Bearer " + follower.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(following.path("data").path("records"))
                .anySatisfy(record -> assertThat(record.path("id").asText()).isEqualTo(target.userId()));

        JsonNode followers = readJson(mockMvc.perform(get("/api/users/{id}/followers", target.userId())
                        .header("Authorization", "Bearer " + follower.token()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(followers.path("data").path("records"))
                .anySatisfy(record -> assertThat(record.path("id").asText()).isEqualTo(follower.userId()));

        JsonNode unfollow = deleteWithToken("/api/users/" + target.userId() + "/follow", follower.token());
        JsonNode unfollowAgain = deleteWithToken("/api/users/" + target.userId() + "/follow", follower.token());
        assertThat(unfollow.path("data").path("following").asBoolean()).isFalse();
        assertThat(unfollow.path("data").path("followerCount").asInt()).isEqualTo(0);
        assertThat(unfollowAgain.path("data").path("followerCount").asInt()).isEqualTo(0);
    }

    @Test
    void anonymousUserCannotWriteInteractions() throws Exception {
        AuthSession author = registerAndGetSession("anon-author");
        String postId = createPost(author.token(), "Anonymous guard " + UUID.randomUUID(), "Post for auth guard.")
                .path("data")
                .path("id")
                .asText();

        mockMvc.perform(post("/api/posts/{id}/like", postId))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode postWithToken(String url, String token) throws Exception {
        return readJson(mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn());
    }

    private JsonNode deleteWithToken(String url, String token) throws Exception {
        return readJson(mockMvc.perform(delete(url)
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

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private record AuthSession(String token, String userId) {
    }
}
