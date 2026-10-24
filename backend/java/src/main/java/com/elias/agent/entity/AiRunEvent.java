package com.elias.agent.entity;

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
 * Agent run 事件实体
 * 对应数据库 ai_run_event 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_run_event")
public class AiRunEvent {

    /**
     * 事件ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属 run ID
     */
    private Long runId;

    /**
     * run 内递增事件序号
     */
    private Integer eventSeq;

    /**
     * 事件类型，例如 stage.changed
     */
    private String eventType;

    /**
     * 当前阶段编码
     */
    private String stageCode;

    /**
     * 展示消息
     */
    private String message;

    /**
     * 事件附加数据，JSON 字符串
     */
    private String payloadJson;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
