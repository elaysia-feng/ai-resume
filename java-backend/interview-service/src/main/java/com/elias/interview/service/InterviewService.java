package com.elias.interview.service;

import com.elias.common.dto.interview.internal.request.InternalInterviewQuestionAnalysisRequest;
import com.elias.common.dto.interview.internal.request.InternalInterviewQuestionCreateRequest;
import com.elias.common.dto.interview.internal.response.InternalInterviewQuestionCreateResponse;
import com.elias.common.dto.interview.internal.response.InternalInterviewRoundDetailResponse;
import com.elias.interview.dto.request.InterviewQuestionRoundAnswerRequest;
import com.elias.interview.dto.request.InterviewQuestionRoundPageRequest;
import com.elias.interview.dto.request.StartInterviewRunRequest;
import com.elias.interview.dto.response.InterviewAnswerResponse;
import com.elias.interview.dto.response.InterviewBoardResponse;
import com.elias.interview.dto.response.InterviewQuestionRoundPageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;

public interface InterviewService {

    /**
     * 创建模拟面试会话。
     *
     * @param request 模拟会话传入参数
     * @return sessionId
     */

    /**
     * 开启一个面试 Agent run。
     *
     * @param request 开启 run 的请求参数
     * @return runId
     */
    Long startRun(StartInterviewRunRequest request);

    /**
     * 查询当前面试看板。
     *
     * @param runId run ID
     * @return 当前题目和 run 状态
     */
    InterviewBoardResponse questionBoard(Long runId);

    /**
     * 创建一轮面试题。
     *
     * @param runId run ID
     * @param request 题目创建请求
     * @return roundId 和 roundNo
     */
    InternalInterviewQuestionCreateResponse createQuestionRound(
            Long runId,
            InternalInterviewQuestionCreateRequest request) throws JsonProcessingException;

    /**
     * 回写面试题分析。
     *
     * @param roundId 题目轮次 ID
     * @param request 分析结果请求
     */
    void updateQuestionAnalysis(
            Long roundId,
            InternalInterviewQuestionAnalysisRequest request) throws JsonProcessingException;

    /**
     * 获取题目和用户答案。
     *
     * @param roundId 题目轮次 ID
     * @return 题目详情
     */
    InternalInterviewRoundDetailResponse getQuestionAnswer(Long roundId);

    /**
     * 提交面试答案。
     *
     * @param roundId 题目轮次 ID
     * @param request 答案请求
     * @return 提交结果
     */
    InterviewAnswerResponse submitAnswer(Long roundId, InterviewQuestionRoundAnswerRequest request);

    /**
     * 分页查询已回答的面试题历史。
     *
     * @param runId run ID
     * @param request 分页请求
     * @return 分页结果
     */
    InterviewQuestionRoundPageResponse pageQuestionRounds(
            Long runId,
            @Valid InterviewQuestionRoundPageRequest request);

    /**
     * 结束面试 run。
     *
     * @param runId run ID
     */
    void finishRun(Long runId);
}
