package com.airesumeforge.agent.entity;

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
 * Agent会话实体
 * 对应数据库 ai_session 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_session")
public class AgentSession {

    /**
     * 会话ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 关联简历ID，可为空
     */
    private Long resumeId;

    /**
     * 会话场景：OPTIMIZE / MATCH / SUMMARY / CHAT
     */
    private String sceneCode;

    /**
     * 会话标题
     */
    private String sessionTitle;

    /**
     * 当前会话复用的目标岗位JD
     */
    private String jobDescription;

    /**
     * 当前会话长期记忆摘要
     */
    private String summary;

    /**
     * 派生来源会话ID，用于New Session复制JD但清空历史
     */
    private Long parentSessionId;

    /**
     * 会话状态：ACTIVE / ARCHIVED / DELETED
     */
    private String status;

    /**
     * 最后一条消息时间
     */
    private LocalDateTime lastMessageAt;

    /**
     * 创建时间，插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间，插入和更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
