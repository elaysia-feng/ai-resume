package com.airesumeforge.dto.interview.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试模拟面板响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewBoardResponse {

    /**
     * run ID
     */
    private Long runId;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * run 状态：QUEUED / RUNNING / WAITING_USER / SUCCESS / FAILED
     */
    private String status;

    /**
     * 当前题目，当前没有待答题时可为空
     */
    private InterviewQuestionResponse currentQuestion;

    /**
     * 面试总结
     */
    private String summary;

    /**
     * 错误信息
     */
    private String errorMessage;
}

