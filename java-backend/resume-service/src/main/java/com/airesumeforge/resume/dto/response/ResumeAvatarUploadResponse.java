package com.airesumeforge.resume.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历头像上传响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeAvatarUploadResponse {

    /**
     * 上传后的头像访问地址
     */
    private String avatarUrl;
}
