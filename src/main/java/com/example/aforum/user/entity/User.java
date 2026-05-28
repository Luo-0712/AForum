package com.example.aforum.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 个人简介 */
    private String bio;

    /** 用户类型：0-普通用户 1-VIP 2-管理员 3-超级管理员 */
    private Integer userType;

    /** 状态：0-正常 1-禁用 2-封禁 */
    private Integer status;

    /** 最后登录时间 */
    private java.time.LocalDateTime lastLoginAt;

    /** 最后登录IP */
    private String lastLoginIp;
}
