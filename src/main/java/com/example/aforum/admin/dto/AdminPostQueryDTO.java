package com.example.aforum.admin.dto;

import lombok.Data;

@Data
public class AdminPostQueryDTO {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String keyword;

    private Long userId;

    private Long categoryId;

    private Long boardId;

    private Integer status;
}
