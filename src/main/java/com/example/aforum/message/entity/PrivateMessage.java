package com.example.aforum.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 私信实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("private_message")
public class PrivateMessage extends BaseEntity {

    /** 发送者ID */
    private Long senderId;

    /** 接收者ID */
    private Long receiverId;

    /** 内容 */
    private String content;

    /** 消息类型：text-文本 image-图片 */
    private String messageType;

    /** 是否已读：0-未读 1-已读 */
    private Integer readStatus;
}
