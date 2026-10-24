package com.elias.resume.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建简历请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeCreateRequest {

    /**
     * 简历标题，默认"我的简历"
     */
    private String title = "我的简历";

    /**
     * 模板名：classic / modern / creative
     */
    @NotBlank(message = "template 不能为空")
    private String template = "classic";
}
