package com.example.aforum.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aforum.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent任务日志实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_task_log")
public class AgentTaskLog extends BaseEntity {

    /** 智能体ID */
    private Long agentId;

    /** 任务类型 */
    private String taskType;

    /** 输入内容 */
    private String input;

    /** 输出内容 */
    private String output;

    /** 状态：0-失败 1-成功 2-执行中 */
    private Integer status;

    /** 耗时（毫秒） */
    private Long duration;

    /** 错误信息 */
    private String errorMessage;
}
