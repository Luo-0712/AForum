package com.example.aforum.content.dto;

import lombok.Data;

@Data
public class PostQueryDTO {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String keyword;

    private Long userId;

    private Long categoryId;

    private Long boardId;

    private String sort;
}
