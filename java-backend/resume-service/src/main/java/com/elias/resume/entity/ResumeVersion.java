package com.elias.resume.entity;

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
 * 简历版本实体
 * 对应数据库 resume_versions 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("resume_versions")
public class ResumeVersion {

    /**
     * 版本ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属简历ID
     */
    private Long resumeId;

    /**
     * 版本号，从1开始递增
     */
    private Integer versionNo;

    /**
     * 版本名称，如 v1、投递后端版
     */
    private String versionName;

    /**
     * 保存版本时的简历标题
     */
    private String resumeTitle;

    /**
     * 保存版本时的简历模板
     */
    private String resumeTemplate;

    /**
     * 版本来源：MANUAL / AUTO / AI
     */
    private String source;

    /**
     * 创建时间，插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
