package com.example.aforum.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 */
@Getter
@AllArgsConstructor
public enum UserStatus {

    ACTIVE(0, "正常"),
    DISABLED(1, "禁用"),
    BANNED(2, "封禁");

    private final Integer code;
    private final String description;
}
