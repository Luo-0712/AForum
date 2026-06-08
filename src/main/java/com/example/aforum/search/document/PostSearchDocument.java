package com.example.aforum.search.document;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PostSearchDocument {

    private Long id;
    private String title;
    private String content;
    private String summary;
    private String coverUrl;
    private Long authorId;
    private String authorName;
    private Long categoryId;
    private String categoryName;
    private Long boardId;
    private String boardName;
    private Integer status;
    private Boolean top;
    private Boolean essence;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
