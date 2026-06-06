package com.example.aforum.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 内容审核结果实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_review")
public class ContentReview extends BaseEntity {

    private String contentType;

    private Long contentId;

    private String decision;

    private String riskLevel;

    private BigDecimal riskScore;

    private String reason;

    private String suggestion;

    private String modelName;

    private String promptVersion;
}
