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
 * 简历版本模块实体
 * 对应数据库 resume_version_sections 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("resume_version_sections")
public class ResumeVersionSection {

    /**
     * 版本模块ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属版本ID
     */
    private Long versionId;

    /**
     * 模块代码
     */
    private String sectionCode;

    /**
     * 模块标题
     */
    private String sectionTitle;

    /**
     * 模块类型：SYSTEM / CUSTOM
     */
    private String sectionType;

    /**
     * 内容 Schema 类型：TEXT / LIST / TAGS
     */
    private String schemaType;

    /**
     * 模块内容 JSON
     */
    private String contentJson;

    /**
     * 是否显示
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
}
