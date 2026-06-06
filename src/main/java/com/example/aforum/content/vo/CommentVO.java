package com.example.aforum.content.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentVO {

    private Long id;
    private String content;
    private Long postId;
    private Long userId;
    private String authorName;
    private Long parentId;
    private Long replyUserId;
    private Integer likeCount;
    private Integer status;
    private LocalDateTime createdAt;
}
