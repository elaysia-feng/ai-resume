package com.elias.resume.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 更新整份简历请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUpdateRequest {

    /**
     * 简历标题
     */
    private String title;

    /**
     * 模板名：classic / modern / creative
     */
    private String template;

    /**
     * 整份模块内容，key 为 sectionId，value 为模块内容 JSON
     */
    private Map<Long, JsonNode> sections;
}
