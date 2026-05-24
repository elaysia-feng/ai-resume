package com.elias.interview.controller;

import com.elias.common.ApiResponse;
import com.elias.interview.dto.request.InterviewQuestionRoundAnswerRequest;
import com.elias.interview.dto.request.InterviewQuestionRoundPageRequest;
import com.elias.interview.dto.request.StartInterviewRunRequest;
import com.elias.interview.dto.response.InterviewAnswerResponse;
import com.elias.interview.dto.response.InterviewBoardResponse;
import com.elias.interview.dto.response.InterviewQuestionRoundPageResponse;
import com.elias.interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    /**
     * 开启一个面试 Agent run。
     *
     * @param request 开启 run 的请求参数
     * @return runId
     */
    @PostMapping("/runs")
    public ApiResponse<Long> startRun(@RequestBody StartInterviewRunRequest request) {
        return ApiResponse.ok(interviewService.startRun(request));
    }

    /**
     * 查询当前面试看板，只返回当前待回答题目。
     *
     * @param runId run ID
     * @return 面试看板
     */
    @GetMapping("/runs/{runId}/board")
    public ApiResponse<InterviewBoardResponse> questionBoard(@PathVariable Long runId) {
        if (runId == null) {
            return ApiResponse.error("runId 不能为空");
        }
        return ApiResponse.ok(interviewService.questionBoard(runId));
    }

    /**
     * 提交某一轮面试题答案。
     *
     * @param roundId 题目轮次 ID
     * @param request 答案请求
     * @return 提交结果
     */
    @PostMapping("/interquestion-rounds/{roundId}/answer")
    public ApiResponse<InterviewAnswerResponse> submitAnswer(
            @PathVariable Long roundId,
            @RequestBody InterviewQuestionRoundAnswerRequest request) {
        if (roundId == null) {
            return ApiResponse.error("roundId 不能为空");
        }
        return ApiResponse.ok(interviewService.submitAnswer(roundId, request));
    }

    /**
     * 分页查询已回答的面试题历史。
     *
     * @param runId run ID
     * @param request 分页请求
     * @return 面试题历史
     */
    @GetMapping("/runs/{runId}/question-rounds")
    public ApiResponse<InterviewQuestionRoundPageResponse> pageQuestionRounds(
            @PathVariable Long runId,
            @Valid InterviewQuestionRoundPageRequest request) {
        return ApiResponse.ok(interviewService.pageQuestionRounds(runId, request));
    }

    /**
     * 手动结束面试 run。
     *
     * @param runId run ID
     * @return 空响应
     */
    @PostMapping("/runs/{runId}/finish")
    public ApiResponse<Void> finishRun(@PathVariable Long runId) {
        if (runId == null) {
            return ApiResponse.error("runId 不能为空");
        }
        interviewService.finishRun(runId);
        return ApiResponse.ok();
    }
}
