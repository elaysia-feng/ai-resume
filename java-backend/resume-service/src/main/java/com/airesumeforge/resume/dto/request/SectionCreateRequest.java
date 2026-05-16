package com.airesumeforge.resume.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加简历模块请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionCreateRequest {

    /**
     * 模块代码。
     * 系统内置模块允许补回，但同一份简历内不允许重复创建同一系统模块。
     */
    @NotBlank(message = "sectionCode 不能为空")
    private String sectionCode;

    /**
     * 模块标题
     */
    @NotBlank(message = "sectionTitle 不能为空")
    private String sectionTitle;

    /**
     * 内容Schema类型：TEXT / LIST / TAGS
     */
    @NotBlank(message = "schemaType 不能为空")
    private String schemaType;

    /**
     * 初始内容JSON字符串
     */
    private String contentJson = "{}";

    /**
     * 是否显示，默认true
     */
    private Boolean visible = true;
}
