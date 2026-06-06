package com.example.aforum.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RoleVO {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer status;
    private List<Long> permissionIds;
    private List<String> permissionCodes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
