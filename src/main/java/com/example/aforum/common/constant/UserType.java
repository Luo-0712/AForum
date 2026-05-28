package com.example.aforum.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户类型枚举
 */
@Getter
@AllArgsConstructor
public enum UserType {

    NORMAL(0, "普通用户"),
    VIP(1, "VIP用户"),
    ADMIN(2, "管理员"),
    SUPER_ADMIN(3, "超级管理员");

    private final Integer code;
    private final String description;
}
