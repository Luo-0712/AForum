package com.example.aforum.search.service;

import com.example.aforum.search.config.SearchProperties;
import com.example.aforum.search.vo.SearchHealthVO;
import com.example.aforum.search.vo.SearchReindexVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "false", matchIfMissing = false)
public class NoopPostSearchIndexService implements PostSearchIndexService {

    private final SearchProperties searchProperties;

    @Override
    public void indexPost(Long postId) {
    }

    @Override
    public void deletePost(Long postId) {
    }

    @Override
    public void updatePostStats(Long postId) {
    }

    @Override
    public SearchReindexVO reindexAllPublishedPosts() {
        return SearchReindexVO.builder()
                .indexedCount(0)
                .failedCount(0)
                .indexAlias(searchProperties.getElasticsearch().getPostIndex())
                .physicalIndex(searchProperties.getElasticsearch().getPostIndexVersion())
                .build();
    }

    @Override
    public SearchHealthVO health() {
        return SearchHealthVO.builder()
                .enabled(false)
                .available(false)
                .aliasExists(false)
                .indexAlias(searchProperties.getElasticsearch().getPostIndex())
                .physicalIndex(searchProperties.getElasticsearch().getPostIndexVersion())
                .message("搜索功能已关闭，使用 MySQL 降级查询")
                .build();
    }
}
