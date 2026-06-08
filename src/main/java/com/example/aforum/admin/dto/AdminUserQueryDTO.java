package com.example.aforum.admin.dto;

import lombok.Data;

@Data
public class AdminUserQueryDTO {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String keyword;

    private Integer status;

    private Integer userType;
}
