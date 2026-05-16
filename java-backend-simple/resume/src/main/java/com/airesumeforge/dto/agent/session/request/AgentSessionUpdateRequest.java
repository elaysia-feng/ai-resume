package com.airesumeforge.dto.agent.session.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新Agent会话请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentSessionUpdateRequest {

    /**
     * 会话标题
     */
    private String sessionTitle;

    /**
     * 当前会话复用的目标岗位JD
     */
    private String jobDescription;

    /**
     * 当前会话长期记忆摘要
     */
    private String summary;

    /**
     * 会话状态：ACTIVE / ARCHIVED / DELETED
     */
    private String status;
}

