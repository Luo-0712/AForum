package com.example.aforum.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子标签关联实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("post_tag")
public class PostTag extends BaseEntity {

    /** 帖子ID */
    private Long postId;

    /** 标签ID */
    private Long tagId;
}
