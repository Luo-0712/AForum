package com.example.aforum.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification")
public class Notification extends BaseEntity {

    /** 接收用户ID */
    private Long userId;

    /** 发送用户ID */
    private Long senderId;

    /** 通知类型：like-点赞 comment-评论 follow-关注 system-系统 */
    private String type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 关联ID（帖子ID、评论ID等） */
    private Long targetId;

    /** 关联类型：post-帖子 comment-评论 */
    private String targetType;

    /** 是否已读：0-未读 1-已读 */
    private Integer readStatus;
}
