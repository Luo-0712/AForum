package com.example.aforum.search.service.impl;

import com.example.aforum.common.constant.PostStatus;
import com.example.aforum.common.result.PageResult;
import com.example.aforum.search.config.SearchProperties;
import com.example.aforum.search.dto.SearchPostQueryDTO;
import com.example.aforum.search.service.PostSearchService;
import com.example.aforum.search.vo.SearchHighlightVO;
import com.example.aforum.search.vo.SearchPostVO;
import com.example.aforum.interaction.service.InteractionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchPostSearchService implements PostSearchService {

    private static final int MAX_PAGE_SIZE = 50;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SearchProperties searchProperties;
    private final MysqlPostSearchFallback mysqlPostSearchFallback;
    private final InteractionService interactionService;

    @Override
    public PageResult<SearchPostVO> searchPosts(Long currentUserId, SearchPostQueryDTO query) {
        if (!StringUtils.hasText(query.getQ())) {
            return mysqlPostSearchFallback.search(currentUserId, query);
        }
        try {
            return searchByElasticsearch(currentUserId, query);
        } catch (Exception ex) {
            log.warn("Elasticsearch 搜索失败，准备降级到 MySQL，keyword={}", query.getQ(), ex);
            if (searchProperties.isFallbackToMysql()) {
                return mysqlPostSearchFallback.search(currentUserId, query);
            }
            throw new IllegalStateException("搜索服务不可用", ex);
        }
    }

    private PageResult<SearchPostVO> searchByElasticsearch(Long currentUserId, SearchPostQueryDTO query) throws IOException {
        long pageNum = sanitizePageNum(query.getPageNum());
        long pageSize = sanitizePageSize(query.getPageSize());
        Request request = new Request("POST", "/" + searchProperties.getElasticsearch().getPostIndex() + "/_search");
        request.setJsonEntity(objectMapper.writeValueAsString(searchPayload(query, pageNum, pageSize)));
        Response response = restClient.performRequest(request);
        JsonNode root = objectMapper.readTree(response.getEntity().getContent());
        JsonNode hits = root.path("hits");
        long total = hits.path("total").path("value").asLong(0);
        List<SearchPostVO> records = new ArrayList<>();
        for (JsonNode hit : hits.path("hits")) {
            records.add(toSearchPostVO(hit.path("_source"), hit.path("highlight"), currentUserId));
        }
        return new PageResult<>(records, total, pageSize, pageNum);
    }

    private Map<String, Object> searchPayload(SearchPostQueryDTO query, long pageNum, long pageSize) {
        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("must", List.of(Map.of("multi_match", Map.of(
                "query", query.getQ().trim(),
                "fields", List.of("title^4", "summary^2", "content", "authorName^1.5"),
                "type", "best_fields"
        ))));

        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(Map.of("term", Map.of("status", PostStatus.PUBLISHED.getCode())));
        if (query.getCategoryId() != null) {
            filters.add(Map.of("term", Map.of("categoryId", query.getCategoryId())));
        }
        if (query.getBoardId() != null) {
            filters.add(Map.of("term", Map.of("boardId", query.getBoardId())));
        }
        if (query.getAuthorId() != null) {
            filters.add(Map.of("term", Map.of("authorId", query.getAuthorId())));
        }
        bool.put("filter", filters);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", (pageNum - 1) * pageSize);
        payload.put("size", pageSize);
        payload.put("query", Map.of("bool", bool));
        payload.put("sort", sort(query.getSort()));
        if (!Boolean.FALSE.equals(query.getHighlight())) {
            payload.put("highlight", Map.of(
                    "pre_tags", List.of("<mark>"),
                    "post_tags", List.of("</mark>"),
                    "fields", Map.of(
                            "title", Map.of("number_of_fragments", 0),
                            "summary", Map.of("fragment_size", 120, "number_of_fragments", 1),
                            "content", Map.of("fragment_size", 140, "number_of_fragments", 2)
                    )
            ));
        }
        return payload;
    }

    private List<Map<String, Object>> sort(String sort) {
        return switch (StringUtils.hasText(sort) ? sort : "relevance") {
            case "latest" -> List.of(
                    Map.of("createdAt", Map.of("order", "desc")),
                    Map.of("_score", Map.of("order", "desc"))
            );
            case "hot" -> List.of(
                    Map.of("commentCount", Map.of("order", "desc")),
                    Map.of("likeCount", Map.of("order", "desc")),
                    Map.of("viewCount", Map.of("order", "desc")),
                    Map.of("_score", Map.of("order", "desc"))
            );
            default -> List.of(
                    Map.of("_score", Map.of("order", "desc")),
                    Map.of("top", Map.of("order", "desc")),
                    Map.of("essence", Map.of("order", "desc")),
                    Map.of("createdAt", Map.of("order", "desc"))
            );
        };
    }

    private SearchPostVO toSearchPostVO(JsonNode source, JsonNode highlight, Long currentUserId) {
        Long postId = longValue(source.path("id"));
        return SearchPostVO.builder()
                .id(postId)
                .title(text(source.path("title")))
                .summary(text(source.path("summary")))
                .coverUrl(text(source.path("coverUrl")))
                .userId(longValue(source.path("authorId")))
                .authorName(text(source.path("authorName")))
                .categoryId(longValue(source.path("categoryId")))
                .categoryName(text(source.path("categoryName")))
                .boardId(longValue(source.path("boardId")))
                .boardName(text(source.path("boardName")))
                .status(source.path("status").isMissingNode() ? null : source.path("status").asInt())
                .viewCount(source.path("viewCount").asInt(0))
                .likeCount(source.path("likeCount").asInt(0))
                .commentCount(source.path("commentCount").asInt(0))
                .favoriteCount(source.path("favoriteCount").asInt(0))
                .liked(interactionService.isPostLiked(currentUserId, postId))
                .favorited(interactionService.isPostFavorited(currentUserId, postId))
                .top(source.path("top").asBoolean(false))
                .essence(source.path("essence").asBoolean(false))
                .createdAt(localDateTime(source.path("createdAt")))
                .updatedAt(localDateTime(source.path("updatedAt")))
                .highlights(toHighlightVO(highlight))
                .build();
    }

    private SearchHighlightVO toHighlightVO(JsonNode highlight) {
        if (highlight == null || highlight.isMissingNode()) {
            return null;
        }
        return SearchHighlightVO.builder()
                .title(strings(highlight.path("title")))
                .summary(strings(highlight.path("summary")))
                .content(strings(highlight.path("content")))
                .build();
    }

    private List<String> strings(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return values;
    }

    private String text(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private Long longValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asLong();
    }

    private LocalDateTime localDateTime(JsonNode node) {
        String value = text(node);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return LocalDateTime.parse(value.replace(' ', 'T'));
    }

    private long sanitizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    private long sanitizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
