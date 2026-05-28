package com.example.aforum.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色权限关联实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("role_permission")
public class RolePermission extends BaseEntity {

    /** 角色ID */
    private Long roleId;

    /** 权限ID */
    private Long permissionId;
}
