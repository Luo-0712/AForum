package com.example.aforum.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 帖子状态枚举
 */
@Getter
@AllArgsConstructor
public enum PostStatus {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    REVIEWING(2, "审核中"),
    REJECTED(3, "已拒绝"),
    CLOSED(4, "已关闭");

    private final Integer code;
    private final String description;
}
