package com.airesumeforge.resume.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 保存简历版本请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeVersionSaveRequest {

    /**
     * 版本名称，不传则由后端自动生成
     */
    private String versionName;

    /**
     * 版本来源：MANUAL / AUTO / AI
     */
    private String source = "MANUAL";
}
