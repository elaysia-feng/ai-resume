package com.airesumeforge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 简历主表实体
 * 对应数据库 resumes 表
 * @author 爱门
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("resumes")
public class Resume {

    /**
     * 简历ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID（来自JWT）
     */
    private Long userId;

    /**
     * 简历标题，默认"我的简历"
     */
    private String title;

    /**
     * 模板名：classic / modern / creative
     */
    private String template;

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
