package com.example.aforum.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SearchPostQueryDTO {

    @Size(max = 100, message = "搜索关键词不能超过100个字符")
    @NotBlank(message = "搜索关键词不能为空")
    private String q;

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private Long categoryId;

    private Long boardId;

    private Long authorId;

    private String sort = "relevance";

    private Boolean highlight = true;
}
