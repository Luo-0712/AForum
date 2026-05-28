package com.example.aforum.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户角色关联实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_role")
public class UserRole extends BaseEntity {

    /** 用户ID */
    private Long userId;

    /** 角色ID */
    private Long roleId;
}
