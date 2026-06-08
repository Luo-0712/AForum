package com.example.aforum.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminBoardVO {

    private Long id;
    private String name;
    private String code;
    private String description;
    private String icon;
    private Integer sort;
    private Integer status;
    private Integer postCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
