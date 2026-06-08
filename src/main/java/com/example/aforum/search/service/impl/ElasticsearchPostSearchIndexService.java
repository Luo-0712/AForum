package com.example.aforum.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aforum.common.constant.PostStatus;
import com.example.aforum.content.entity.Board;
import com.example.aforum.content.entity.Category;
import com.example.aforum.content.entity.Post;
import com.example.aforum.content.mapper.BoardMapper;
import com.example.aforum.content.mapper.CategoryMapper;
import com.example.aforum.content.mapper.PostMapper;
import com.example.aforum.search.config.SearchProperties;
import com.example.aforum.search.document.PostSearchDocument;
import com.example.aforum.search.service.PostSearchIndexService;
import com.example.aforum.search.vo.SearchHealthVO;
import com.example.aforum.search.vo.SearchReindexVO;
import com.example.aforum.user.entity.User;
import com.example.aforum.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchPostSearchIndexService implements PostSearchIndexService {

    private static final int BULK_SIZE = 500;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SearchProperties searchProperties;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final BoardMapper boardMapper;

    @Override
    public void indexPost(Long postId) {
        if (postId == null) {
            return;
        }
        try {
            ensureIndexReady();
            PostSearchDocument document = buildDocument(postId);
            if (document == null) {
                deletePost(postId);
                return;
            }
            Request request = new Request("PUT", "/" + writeIndex() + "/_doc/" + postId);
            request.setJsonEntity(objectMapper.writeValueAsString(document));
            restClient.performRequest(request);
        } catch (Exception ex) {
            log.warn("同步帖子搜索索引失败，postId={}", postId, ex);
        }
    }

    @Override
    public void deletePost(Long postId) {
        if (postId == null) {
            return;
        }
        try {
            ensureIndexReady();
            Request request = new Request("DELETE", "/" + aliasIndex() + "/_doc/" + postId);
            restClient.performRequest(request);
        } catch (Exception ex) {
            log.warn("删除帖子搜索索引失败，postId={}", postId, ex);
        }
    }

    @Override
    public void updatePostStats(Long postId) {
        if (postId == null) {
            return;
        }
        Post post = postMapper.selectById(postId);
        if (post == null || !PostStatus.PUBLISHED.getCode().equals(post.getStatus())) {
            deletePost(postId);
            return;
        }
        try {
            ensureIndexReady();
            Map<String, Object> payload = Map.of("doc", Map.of(
                    "viewCount", defaultInt(post.getViewCount()),
                    "likeCount", defaultInt(post.getLikeCount()),
                    "commentCount", defaultInt(post.getCommentCount()),
                    "favoriteCount", defaultInt(post.getFavoriteCount()),
                    "updatedAt", post.getUpdatedAt()
            ));
            Request request = new Request("POST", "/" + aliasIndex() + "/_update/" + postId);
            request.setJsonEntity(objectMapper.writeValueAsString(payload));
            restClient.performRequest(request);
        } catch (Exception ex) {
            log.warn("更新帖子搜索统计字段失败，postId={}", postId, ex);
        }
    }

    @Override
    public SearchReindexVO reindexAllPublishedPosts() {
        long indexed = 0;
        long failed = 0;
        try {
            ensureIndexReady();
            clearCurrentIndex();
            long page = 1;
            while (true) {
                List<Post> posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, PostStatus.PUBLISHED.getCode())
                        .orderByAsc(Post::getId)
                        .last("LIMIT " + ((page - 1) * BULK_SIZE) + "," + BULK_SIZE));
                if (posts.isEmpty()) {
                    break;
                }
                BulkResult result = bulkIndex(posts);
                indexed += result.indexedCount();
                failed += result.failedCount();
                if (posts.size() < BULK_SIZE) {
                    break;
                }
                page++;
            }
        } catch (Exception ex) {
            failed++;
            log.warn("重建帖子搜索索引失败", ex);
        }
        return SearchReindexVO.builder()
                .indexedCount(indexed)
                .failedCount(failed)
                .indexAlias(aliasIndex())
                .physicalIndex(writeIndex())
                .build();
    }

    @Override
    public SearchHealthVO health() {
        boolean available = false;
        boolean aliasExists = false;
        String message = "Elasticsearch 不可用";
        try {
            restClient.performRequest(new Request("GET", "/"));
            available = true;
            aliasExists = exists("/_alias/" + aliasIndex());
            message = aliasExists ? "Elasticsearch 可用" : "Elasticsearch 可用，但索引别名尚未创建";
        } catch (Exception ex) {
            message = ex.getMessage();
        }
        return SearchHealthVO.builder()
                .enabled(true)
                .available(available)
                .aliasExists(aliasExists)
                .indexAlias(aliasIndex())
                .physicalIndex(writeIndex())
                .message(message)
                .build();
    }

    public PostSearchDocument buildDocument(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || !PostStatus.PUBLISHED.getCode().equals(post.getStatus())) {
            return null;
        }
        return PostSearchDocument.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .summary(post.getSummary())
                .coverUrl(post.getCoverImage())
                .authorId(post.getUserId())
                .authorName(userName(post.getUserId()))
                .categoryId(post.getCategoryId())
                .categoryName(categoryName(post.getCategoryId()))
                .boardId(post.getBoardId())
                .boardName(boardName(post.getBoardId()))
                .status(post.getStatus())
                .top(Boolean.TRUE.equals(post.getTop()))
                .essence(Boolean.TRUE.equals(post.getEssence()))
                .viewCount(defaultInt(post.getViewCount()))
                .likeCount(defaultInt(post.getLikeCount()))
                .commentCount(defaultInt(post.getCommentCount()))
                .favoriteCount(defaultInt(post.getFavoriteCount()))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private BulkResult bulkIndex(List<Post> posts) throws IOException {
        StringBuilder payload = new StringBuilder();
        long expected = 0;
        for (Post post : posts) {
            PostSearchDocument document = buildDocument(post.getId());
            if (document == null) {
                continue;
            }
            payload.append(objectMapper.writeValueAsString(Map.of(
                    "index", Map.of("_index", aliasIndex(), "_id", String.valueOf(post.getId()))
            ))).append('\n');
            payload.append(objectMapper.writeValueAsString(document)).append('\n');
            expected++;
        }
        if (expected == 0) {
            return new BulkResult(0, 0);
        }
        Request request = new Request("POST", "/_bulk");
        request.addParameter("refresh", "true");
        request.setEntity(new org.apache.http.nio.entity.NStringEntity(
                payload.toString(), org.apache.http.entity.ContentType.APPLICATION_JSON));
        Response response = restClient.performRequest(request);
        JsonNode body = objectMapper.readTree(response.getEntity().getContent());
        long failures = 0;
        if (body.path("errors").asBoolean(false)) {
            for (JsonNode item : body.path("items")) {
                JsonNode index = item.path("index");
                if (index.has("error")) {
                    failures++;
                    log.warn("批量写入帖子搜索索引失败: {}", index.path("error"));
                }
            }
        }
        return new BulkResult(expected - failures, failures);
    }

    private void ensureIndexReady() throws IOException {
        if (!exists("/" + writeIndex())) {
            Request create = new Request("PUT", "/" + writeIndex());
            create.setJsonEntity(objectMapper.writeValueAsString(indexDefinition()));
            restClient.performRequest(create);
        }
        if (!exists("/_alias/" + aliasIndex())) {
            Map<String, Object> payload = Map.of("actions", List.of(Map.of("add", Map.of(
                    "index", writeIndex(),
                    "alias", aliasIndex()
            ))));
            Request alias = new Request("POST", "/_aliases");
            alias.setJsonEntity(objectMapper.writeValueAsString(payload));
            restClient.performRequest(alias);
        }
    }

    private void clearCurrentIndex() throws IOException {
        Map<String, Object> payload = Map.of("query", Map.of("match_all", Map.of()));
        Request request = new Request("POST", "/" + aliasIndex() + "/_delete_by_query");
        request.addParameter("conflicts", "proceed");
        request.addParameter("refresh", "true");
        request.setJsonEntity(objectMapper.writeValueAsString(payload));
        restClient.performRequest(request);
    }

    private boolean exists(String endpoint) throws IOException {
        try {
            Response response = restClient.performRequest(new Request("HEAD", endpoint));
            return response.getStatusLine().getStatusCode() == HttpStatus.OK.value();
        } catch (org.elasticsearch.client.ResponseException ex) {
            if (ex.getResponse().getStatusLine().getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                return false;
            }
            throw ex;
        }
    }

    private Map<String, Object> indexDefinition() {
        Map<String, Object> textField = textField();
        Map<String, Object> titleField = new LinkedHashMap<>(textField);
        titleField.put("fields", Map.of("keyword", Map.of("type", "keyword", "ignore_above", 256)));
        Map<String, Object> authorNameField = new LinkedHashMap<>(textField);
        authorNameField.put("fields", Map.of("keyword", Map.of("type", "keyword", "ignore_above", 128)));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("id", Map.of("type", "keyword"));
        properties.put("title", titleField);
        properties.put("content", textField());
        properties.put("summary", textField());
        properties.put("coverUrl", Map.of("type", "keyword", "ignore_above", 512));
        properties.put("authorId", Map.of("type", "long"));
        properties.put("authorName", authorNameField);
        properties.put("categoryId", Map.of("type", "long"));
        properties.put("categoryName", Map.of("type", "keyword"));
        properties.put("boardId", Map.of("type", "long"));
        properties.put("boardName", Map.of("type", "keyword"));
        properties.put("status", Map.of("type", "integer"));
        properties.put("top", Map.of("type", "boolean"));
        properties.put("essence", Map.of("type", "boolean"));
        properties.put("viewCount", Map.of("type", "integer"));
        properties.put("likeCount", Map.of("type", "integer"));
        properties.put("commentCount", Map.of("type", "integer"));
        properties.put("favoriteCount", Map.of("type", "integer"));
        properties.put("createdAt", dateField());
        properties.put("updatedAt", dateField());
        return Map.of("mappings", Map.of("properties", properties));
    }

    private Map<String, Object> textField() {
        return Map.of("type", "text", "analyzer", "standard");
    }

    private Map<String, Object> dateField() {
        return Map.of("type", "date", "format", "yyyy-MM-dd HH:mm:ss||strict_date_optional_time||epoch_millis");
    }

    private String userName(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            return "已注销用户";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private String categoryName(Long categoryId) {
        Category category = categoryId == null ? null : categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    private String boardName(Long boardId) {
        Board board = boardId == null ? null : boardMapper.selectById(boardId);
        return board == null ? null : board.getName();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String aliasIndex() {
        return searchProperties.getElasticsearch().getPostIndex();
    }

    private String writeIndex() {
        return searchProperties.getElasticsearch().getPostIndexVersion();
    }

    private record BulkResult(long indexedCount, long failedCount) {
    }
}
