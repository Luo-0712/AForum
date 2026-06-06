package com.example.aforum.content.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostDetailVO {

    private Long id;
    private String title;
    private String content;
    private String summary;
    private Long userId;
    private String authorName;
    private Long categoryId;
    private String categoryName;
    private Long boardId;
    private String boardName;
    private Integer status;
    private String auditReason;
    private List<String> suggestedTags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Boolean top;
    private Boolean essence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
