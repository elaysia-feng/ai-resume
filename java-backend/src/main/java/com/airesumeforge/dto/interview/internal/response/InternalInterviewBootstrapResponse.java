package com.airesumeforge.dto.interview.internal.response;

import com.airesumeforge.dto.resume.response.ResumeSnapshotResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Python 面试模拟启动上下文响应
 *
 * @author 爱门
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InternalInterviewBootstrapResponse {

    /**
     * run ID
     */
    private Long runId;


    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 简历快照
     */
    private ResumeSnapshotResponse resume;

    /**
     * 当前会话复用的目标岗位JD
     */
    private String jobDescription;

    /**
     * 当前会话长期记忆摘要
     */
    private String summary;
}


