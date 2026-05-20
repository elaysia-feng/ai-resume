package com.elias.interview.entity;

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
 * AI 面试模拟轮次实体
 * 对应数据库 ai_interview_round 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_interview_round")
public class AiInterviewRound {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;


    /**
     * 本场面试模拟任务的 run ID
     */
    private Long runId;

    /**
     * 所属 Agent 会话ID
     */
    private Long sessionId;

    /**
     * 关联简历ID
     */
    private Long resumeId;

    /**
     * 第几轮问题，从1开始
     */
    private Integer roundNo;

    /**
     * 题干
     */
    private String questionText;

    /**
     * 选项JSON，例如 A/B/C/D
     */
    private String optionsJson;

    /**
     * 用户本轮完整回答，JSON 字符串
     * 例如: {"selectedOption": "A", "supplementText": "因为..."}
     */
    private String userAnswer;

    /**
     * Python 对本轮回答的分析结果，JSON 字符串
     */
    private String analysisJson;

    /**
     * 状态：WAITING_ANSWER / ANSWERED / FINISHED
     */
    private String status;

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
}
