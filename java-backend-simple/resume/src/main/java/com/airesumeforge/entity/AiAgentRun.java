package com.airesumeforge.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent run 实体
 * 对应数据库 ai_agent_run 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_agent_run")
public class AiAgentRun {

    /**
     * run ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 所属 Agent 会话ID
     */
    private Long sessionId;

    /**
     * 关联简历ID
     */
    private Long resumeId;

    /**
     * 场景编码，例如 JD_CUSTOMIZE
     */
    private String sceneCode;

    /**
     * run 状态：PENDING / QUEUED / RUNNING / WAITING_USER / WAITING_CONFIRM / SUCCESS / FAILED / CANCELLED
     */
    private String status;

    /**
     * 当前阶段：BOOTSTRAP / SUPERVISOR / REWRITER 等
     */
    private String currentStage;

    /**
     * 用户原始输入
     */
    private String userInput;

    /**
     * 目标岗位 JD
     */
    private String jobDescription;

    /**
     * 本次 run 只允许修改的目标模块ID
     */
    private Long targetSectionId;

    /**
     * Clarifier 追问 payload，JSON 字符串
     */
    private String clarificationPayload;

    /**
     * ApprovalPackager 生成的审批 payload，JSON 字符串
     * 包含本次 run 生成的候选 patch 列表，等用户确认后由 Java 应用到简历
     */
    private String approvalPayload;

    /**
     * 本次 run 结果摘要
     */
    private String resultSummary;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 客户端幂等请求ID
     */
    private String clientRequestId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
}
