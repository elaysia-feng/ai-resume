package com.elias.interview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent会话实体
 * 对应数据库 ai_session 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_session")
public class AgentSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    private String sceneCode;

    private String sessionTitle;

    private String jobDescription;

    private String summary;

    private Long parentSessionId;

    private String status;

    private LocalDateTime lastMessageAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}