package com.example.aforum.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户关注实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_follow")
public class UserFollow extends BaseEntity {

    /** 关注者ID */
    private Long userId;

    /** 被关注者ID */
    private Long followUserId;
}
