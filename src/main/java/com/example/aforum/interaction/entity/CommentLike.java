package com.example.aforum.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comment_like")
public class CommentLike extends BaseEntity {

    private Long commentId;

    private Long userId;
}
