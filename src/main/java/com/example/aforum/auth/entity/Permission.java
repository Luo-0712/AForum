package com.example.aforum.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("permission")
public class Permission extends BaseEntity {

    /** 权限名称 */
    private String name;

    /** 权限编码 */
    private String code;

    /** 资源类型：menu-菜单 button-按钮 api-接口 */
    private String resourceType;

    /** 资源路径 */
    private String url;

    /** 父权限ID */
    private Long parentId;

    /** 排序 */
    private Integer sort;

    /** 状态：0-正常 1-禁用 */
    private Integer status;
}
