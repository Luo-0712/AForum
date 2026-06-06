package com.example.aforum.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Agent 动作建议实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_action")
public class AgentAction extends BaseEntity {

    private Long agentId;

    private String actionType;

    private String targetType;

    private Long targetId;

    private String payload;

    private String status;

    private String riskLevel;

    private String proposedBy;

    private Long approvedBy;

    private LocalDateTime executedAt;

    private String errorMessage;
}
