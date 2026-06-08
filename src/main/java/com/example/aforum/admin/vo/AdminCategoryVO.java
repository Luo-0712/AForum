package com.example.aforum.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminCategoryVO {

    private Long id;
    private String name;
    private String code;
    private String description;
    private String icon;
    private Long parentId;
    private Integer sort;
    private Integer status;
    private Long postCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
