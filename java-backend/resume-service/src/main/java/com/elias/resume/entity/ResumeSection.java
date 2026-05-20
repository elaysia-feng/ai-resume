package com.elias.resume.entity;

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

    /**
     * 模块ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属简历ID
     */
    private Long resumeId;

    /**
     * 模块代码：BASIC / JOB_INTENT / SUMMARY / EXPERIENCE / EDUCATION /
     *         SKILLS / SELF_EVALUATION / PROJECTS / CAMPUS /
     *         CERTIFICATES / INTERNSHIP / LAC_CERTIFICATES / CUSTOM
     */
    private String sectionCode;

    /**
     * 模块标题（如"工作经验"）
     */
    private String sectionTitle;

    /**
     * 模块类型：SYSTEM（系统内置不可删除）/ CUSTOM（用户自定义）
     */
    private String sectionType;

    /**
     * 内容Schema类型：TEXT（单文本）/ LIST（列表）/ TAGS（标签组）
     */
    private String schemaType;

    /**
     * 模块内容的JSON字符串
     * TEXT: {text: "..."}
     * LIST: {items: [{...}, {...}]}
     * TAGS: {items: [{name, proficiency}, ...]}
     */
    private String contentJson;

    /**
     * 是否显示，默认true
     */
    private Boolean visible;

    /**
     * 排序序号
     */
    private Integer sortOrder;

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
