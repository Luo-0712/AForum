package com.example.aforum.search.service.impl;

import com.example.aforum.common.result.PageResult;
import com.example.aforum.content.dto.PostQueryDTO;
import com.example.aforum.content.service.PostService;
import com.example.aforum.content.vo.PostSummaryVO;
import com.example.aforum.search.dto.SearchPostQueryDTO;
import com.example.aforum.search.vo.SearchPostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class MysqlPostSearchFallback {

    private static final int MAX_PAGE_SIZE = 50;

    private final PostService postService;

    public PageResult<SearchPostVO> search(Long currentUserId, SearchPostQueryDTO query) {
        PostQueryDTO postQuery = new PostQueryDTO();
        postQuery.setKeyword(StringUtils.hasText(query.getQ()) ? query.getQ().trim() : null);
        postQuery.setPageNum(sanitizePageNum(query.getPageNum()));
        postQuery.setPageSize(sanitizePageSize(query.getPageSize()));
        postQuery.setUserId(query.getAuthorId());
        postQuery.setCategoryId(query.getCategoryId());
        postQuery.setBoardId(query.getBoardId());
        postQuery.setSort(query.getSort());
        PageResult<PostSummaryVO> page = postService.listPosts(currentUserId, postQuery);
        return new PageResult<>(
                page.getRecords().stream().map(this::toSearchVO).toList(),
                page.getTotal(),
                page.getSize(),
                page.getCurrent()
        );
    }

    private SearchPostVO toSearchVO(PostSummaryVO post) {
        return SearchPostVO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .summary(post.getSummary())
                .coverUrl(post.getCoverUrl())
                .userId(post.getUserId())
                .authorName(post.getAuthorName())
                .categoryId(post.getCategoryId())
                .categoryName(post.getCategoryName())
                .boardId(post.getBoardId())
                .boardName(post.getBoardName())
                .status(post.getStatus())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .favoriteCount(post.getFavoriteCount())
                .liked(post.getLiked())
                .favorited(post.getFavorited())
                .createdAt(post.getCreatedAt())
                .build();
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
