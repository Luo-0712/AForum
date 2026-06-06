-- AI 审核结果表：记录模型或规则对帖子/评论的判定，便于后续替换真实 LLM。
CREATE TABLE IF NOT EXISTS `content_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `content_type` VARCHAR(20) NOT NULL COMMENT '内容类型：post/comment',
    `content_id` BIGINT NOT NULL COMMENT '内容ID',
    `decision` VARCHAR(20) NOT NULL COMMENT '结论：approved/rejected/review',
    `risk_level` VARCHAR(20) NOT NULL COMMENT '风险等级：low/medium/high',
    `risk_score` DECIMAL(5,4) NOT NULL DEFAULT 0 COMMENT '风险分',
    `reason` VARCHAR(500) COMMENT '审核原因',
    `suggestion` VARCHAR(500) COMMENT '修改建议',
    `model_name` VARCHAR(100) NOT NULL DEFAULT 'mock-rule-v1',
    `prompt_version` VARCHAR(50) NOT NULL DEFAULT 'rule-v1',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_content` (`content_type`, `content_id`),
    KEY `idx_decision` (`decision`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容审核结果表';

-- Agent 动作建议表：Agent 只能提出动作，业务服务负责审批和执行。
CREATE TABLE IF NOT EXISTS `agent_action` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `agent_id` BIGINT COMMENT '智能体ID',
    `action_type` VARCHAR(50) NOT NULL COMMENT '动作类型',
    `target_type` VARCHAR(50) NOT NULL COMMENT '目标类型',
    `target_id` BIGINT NOT NULL COMMENT '目标ID',
    `payload` JSON COMMENT '动作参数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'proposed' COMMENT '状态：proposed/approved/rejected/executed/failed',
    `risk_level` VARCHAR(20) NOT NULL DEFAULT 'low' COMMENT '风险等级',
    `proposed_by` VARCHAR(100) NOT NULL DEFAULT 'mock-rule-v1',
    `approved_by` BIGINT COMMENT '审批人ID',
    `executed_at` DATETIME COMMENT '执行时间',
    `error_message` VARCHAR(500) COMMENT '错误信息',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent动作建议表';
