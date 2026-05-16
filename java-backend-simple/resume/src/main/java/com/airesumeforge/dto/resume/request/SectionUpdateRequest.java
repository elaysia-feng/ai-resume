package com.airesumeforge.dto.resume.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新简历模块请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionUpdateRequest {

    /**
     * 模块标题
     */
    private String sectionTitle;

    /**
     * 内容JSON字符串
     */
    private String contentJson;

    /**
     * 是否显示
     */
    private Boolean visible;
}

