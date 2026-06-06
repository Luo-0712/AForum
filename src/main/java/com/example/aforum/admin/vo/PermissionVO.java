package com.example.aforum.admin.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionVO {

    private Long id;
    private String name;
    private String code;
    private String resourceType;
    private String url;
    private Long parentId;
    private Integer sort;
    private Integer status;
}
