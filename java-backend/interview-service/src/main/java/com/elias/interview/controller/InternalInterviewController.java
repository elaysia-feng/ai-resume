package com.elias.interview.controller;

import com.elias.common.ApiResponse;
import com.elias.common.dto.interview.internal.request.InternalInterviewQuestionAnalysisRequest;
import com.elias.common.dto.interview.internal.request.InternalInterviewQuestionCreateRequest;
import com.elias.common.dto.interview.internal.response.InternalInterviewQuestionCreateResponse;
import com.elias.common.dto.interview.internal.response.InternalInterviewRoundDetailResponse;
import com.elias.interview.service.InterviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/agent/interview")
@RequiredArgsConstructor
public class InternalInterviewController {

    private final InterviewService interviewService;

    /**
     * Python 创建一轮面试题。
     *
     * @param runId run ID
     * @param request 题目创建请求
     * @return roundId 和 roundNo
     */
    @PostMapping("/runs/{runId}/rounds")
    public ApiResponse<InternalInterviewQuestionCreateResponse> createQuestionRound(
            @PathVariable Long runId,
            @Valid @RequestBody InternalInterviewQuestionCreateRequest request) throws JsonProcessingException {
        return ApiResponse.ok(interviewService.createQuestionRound(runId, request));
    }

    /**
     * Python 回写面试题分析结果。
     *
     * @param roundId 题目轮次 ID
     * @param request 分析结果请求
     * @return 空响应
     */
    @PostMapping("/rounds/{roundId}/analysis")
    public ApiResponse<Void> updateQuestionAnalysis(
            @PathVariable Long roundId,
            @Valid @RequestBody InternalInterviewQuestionAnalysisRequest request) throws JsonProcessingException {
        interviewService.updateQuestionAnalysis(roundId, request);
        return ApiResponse.ok();
    }

    /**
     * Python 获取用户回答，用于恢复 graph 后继续分析。
     *
     * @param roundId 题目轮次 ID
     * @return 题目和用户答案
     */
    @GetMapping("/rounds/{roundId}")
    public ApiResponse<InternalInterviewRoundDetailResponse> getQuestionAnswer(@PathVariable Long roundId) {
        return ApiResponse.ok(interviewService.getQuestionAnswer(roundId));
    }
}
