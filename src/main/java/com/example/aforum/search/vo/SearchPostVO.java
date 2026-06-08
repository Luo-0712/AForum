package com.example.aforum.search.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SearchPostVO {

    private Long id;
    private String title;
    private String summary;
    private String coverUrl;
    private Long userId;
    private String authorName;
    private Long categoryId;
    private String categoryName;
    private Long boardId;
    private String boardName;
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Boolean liked;
    private Boolean favorited;
    private Boolean top;
    private Boolean essence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private SearchHighlightVO highlights;
}
