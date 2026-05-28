package com.example.aforum.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comment")
public class Comment extends BaseEntity {

    /** 内容 */
    private String content;

    /** 帖子ID */
    private Long postId;

    /** 用户ID */
    private Long userId;

    /** 父评论ID（0表示顶级评论） */
    private Long parentId;

    /** 回复用户ID */
    private Long replyUserId;

    /** 点赞数 */
    private Integer likeCount;

    /** 状态：0-正常 1-隐藏 */
    private Integer status;
}
