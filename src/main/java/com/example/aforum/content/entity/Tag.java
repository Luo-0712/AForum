package com.example.aforum.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标签实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tag")
public class Tag extends BaseEntity {

    /** 标签名称 */
    private String name;

    /** 标签编码 */
    private String code;

    /** 使用次数 */
    private Integer useCount;

    /** 状态：0-正常 1-禁用 */
    private Integer status;
}
