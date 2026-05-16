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
 * Agent消息实体
 * 对应数据库 ai_message 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_message")
public class AgentMessage {

    /**
     * 消息ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属会话ID
     */
    private Long sessionId;

    /**
     * 消息角色：USER / ASSISTANT / SYSTEM / TOOL
     */
    private String role;

    /**
     * 消息正文
     */
    private String content;

    /**
     * 内容类型：TEXT / JSON
     */
    private String contentType;

    /**
     * 会话内顺序号，从1开始递增
     */
    private Integer seqNo;

    /**
     * 消息状态：SUCCESS / FAILED
     */
    private String status;

    /**
     * 工具名称，可为空
     */
    private String toolName;

    /**
     * 扩展字段JSON，可记录模型名、token、原始返回等
     */
    private String extraJson;

    /**
     * 创建时间，插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
