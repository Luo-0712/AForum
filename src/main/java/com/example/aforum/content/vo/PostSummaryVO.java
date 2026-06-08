package com.example.aforum.content.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PostSummaryVO {

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
    private String auditReason;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Boolean liked;
    private Boolean favorited;
    private LocalDateTime createdAt;
    private LocalDateTime lastCommentAt;
}
