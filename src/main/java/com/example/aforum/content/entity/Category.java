package com.example.aforum.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分类实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {

    /** 分类名称 */
    private String name;

    /** 分类编码 */
    private String code;

    /** 描述 */
    private String description;

    /** 图标 */
    private String icon;

    /** 父分类ID（0表示顶级分类） */
    private Long parentId;

    /** 排序 */
    private Integer sort;

    /** 状态：0-正常 1-禁用 */
    private Integer status;
}
