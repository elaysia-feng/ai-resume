package com.airesumeforge.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 面试轮次实体
 * 对应数据库 ai_interview_round 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_interview_round")
public class AiInterviewRound {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long runId;

    private Long sessionId;

    private Integer roundNo;

    private String questionText;

    private String optionsJson;

    private String userAnswer;

    private String analysisJson;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}