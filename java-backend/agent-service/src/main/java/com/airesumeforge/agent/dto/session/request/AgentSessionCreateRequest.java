package com.airesumeforge.agent.dto.session.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建Agent会话请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentSessionCreateRequest {

    /**
     * 关联简历ID，可为空
     */
    private Long resumeId;

    /**
     * 会话场景：OPTIMIZE / MATCH / SUMMARY / CHAT
     */
    @NotBlank(message = "sceneCode 不能为空")
    private String sceneCode;

    /**
     * 会话标题，可为空
     */
    private String sessionTitle;

    /**
     * 当前会话复用的目标岗位JD，可为空
     */
    private String jobDescription;

    /**
     * 派生来源会话ID，可为空；用于 New Session 时从旧会话复制JD
     */
    private Long copyFromSessionId;

    /**
     * 是否从来源会话复制JD；只复制JD，不复制summary和messages
     */
    private Boolean copyJobDescription;
}
