package com.example.aforum.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子收藏实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("post_favorite")
public class PostFavorite extends BaseEntity {

    /** 帖子ID */
    private Long postId;

    /** 用户ID */
    private Long userId;
}
