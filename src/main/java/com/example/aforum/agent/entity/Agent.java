package com.example.aforum.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能体实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent")
public class Agent extends BaseEntity {

    /** 智能体名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 类型：chatbot-聊天机器人 assistant-助手 */
    private String type;

    /** 配置（JSON格式） */
    private String config;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 创建者ID */
    private Long userId;
}
