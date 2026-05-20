package com.elias.interview.entity;

import com.baomidou.mybatisplus.annotation.*;
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

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long sessionId;

    private Long resumeId;

    private String sceneCode;

    private String status;

    private String currentStage;

    private String userInput;

    private String jobDescription;

    private String selectedSectionIdsJson;

    private String clarificationPayload;

    private String approvalPayload;

    private String resultSummary;

    private String errorMessage;

    private String clientRequestId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;
}
