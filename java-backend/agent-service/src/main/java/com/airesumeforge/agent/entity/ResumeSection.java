package com.airesumeforge.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 简历模块实体
 * 对应数据库 resume_sections 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("resume_sections")
public class ResumeSection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resumeId;

    private String sectionCode;

    private String sectionTitle;

    private String sectionType;

    private String schemaType;

    private String contentJson;

    private Boolean visible;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}