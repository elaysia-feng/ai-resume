package com.elias.interview.service;

import com.elias.interview.dto.request.CreateInterviewSessionRequest;
import com.elias.interview.dto.request.StartInterviewRunRequest;
import com.elias.interview.dto.request.InterviewQuestionRoundAnswerRequest;
import com.elias.interview.dto.request.InterviewQuestionRoundPageRequest;
import com.elias.interview.dto.internal.request.InternalInterviewQuestionCreateRequest;
import com.elias.interview.dto.response.InterviewBoardResponse;
import com.elias.interview.dto.response.InterviewAnswerResponse;
import com.elias.interview.dto.response.InterviewQuestionRoundPageResponse;
import com.elias.interview.dto.internal.response.InternalInterviewQuestionCreateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;

public interface InterviewService {
    /**
     * 创建模拟面试会话
     *
     * @param request 模拟会话传入参数
     * @return sessionId
     */
    Long createInterviewSession(CreateInterviewSessionRequest request);

    /**
     *  创建agent会话
     * @param request agent会话的请求
     * @return RunId
     */
    Long startRun(StartInterviewRunRequest request);

    /**
     *  查询question
     * @param runId agent会话的Id
     * @return 返回问题
     */
    InterviewBoardResponse questionBoard(Long runId);

    /**
     *
     * @param runId
     * @param request
     * @return
     */

    InternalInterviewQuestionCreateResponse createQuestionRound(Long runId, InternalInterviewQuestionCreateRequest request) throws JsonProcessingException;

    InterviewAnswerResponse submitAnswer(Long roundId, InterviewQuestionRoundAnswerRequest request);

    InterviewQuestionRoundPageResponse pageQuestionRounds(Long runId, @Valid InterviewQuestionRoundPageRequest request);

    void finishRun(Long runId);
}
