package com.example.aforum.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 板块实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("board")
public class Board extends BaseEntity {

    /** 板块名称 */
    private String name;

    /** 板块编码 */
    private String code;

    /** 描述 */
    private String description;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 状态：0-正常 1-禁用 */
    private Integer status;

    /** 帖子数 */
    private Integer postCount;
}
