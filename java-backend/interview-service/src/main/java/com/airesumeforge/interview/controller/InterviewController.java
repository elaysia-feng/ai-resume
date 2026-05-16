package com.airesumeforge.interview.controller;

import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.interview.dto.request.CreateInterviewSessionRequest;
import com.airesumeforge.interview.dto.request.StartInterviewRunRequest;
import com.airesumeforge.interview.dto.request.InterviewQuestionRoundAnswerRequest;
import com.airesumeforge.interview.dto.request.InterviewQuestionRoundPageRequest;
import com.airesumeforge.interview.dto.internal.request.InternalInterviewQuestionCreateRequest;
import com.airesumeforge.interview.service.InterviewService;
import com.airesumeforge.interview.dto.response.InterviewBoardResponse;
import com.airesumeforge.interview.dto.response.InterviewAnswerResponse;
import com.airesumeforge.interview.dto.response.InterviewQuestionRoundPageResponse;
import com.airesumeforge.interview.dto.internal.response.InternalInterviewQuestionCreateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping ("/api/interview")
@RequiredArgsConstructor
public class InterviewController {
    private final InterviewService interviewService;

    /**
     * 创建模拟面试会话
     * @param request 会话请求
     * @return null
     */
    @PostMapping("/session")
    public ApiResponse<Long> createInterview (@RequestBody CreateInterviewSessionRequest request) {
        Long sessionId = interviewService.createInterviewSession(request);
        return ApiResponse.ok(sessionId);
    }

    /**
     *  开启一个 agentRun
     * @param request 开启run的请求参数
     * @return 返回runId
     */
    @PostMapping("/runs")
    public ApiResponse<Long> startRun(@RequestBody StartInterviewRunRequest request) {
        Long runId = interviewService.startRun(request);

        return ApiResponse.ok(runId);
    }

    /**
     *  查询当前的题(只查询一个)
     * @param runId
     * @return
     */
    @GetMapping("/runs/{runId}/board")
    public ApiResponse<InterviewBoardResponse> questionBoard (@PathVariable Long runId) {
        if (runId == null) {
            return ApiResponse.error("runId 不能为空");
        }
        return ApiResponse.ok(interviewService.questionBoard(runId));
    }

    /**
     * 插入题目到数据库
     * @param runId agent的会话
     * @param request 接口请求
     * @return 返回第几轮,题目roundId
     */
    @PostMapping("/runs/{runId}/rounds")
    public ApiResponse<InternalInterviewQuestionCreateResponse> createQuestionRound (@PathVariable Long runId, @RequestBody InternalInterviewQuestionCreateRequest request) throws JsonProcessingException {
        if (runId == null) {
            return ApiResponse.error("runId 不能为空");
        }

        return ApiResponse.ok(interviewService.createQuestionRound(runId, request));
    }

    @PostMapping("/interquestion-rounds/{roundId}/answer")
    public ApiResponse<InterviewAnswerResponse> submitAnswer (@PathVariable Long roundId, @RequestBody InterviewQuestionRoundAnswerRequest request) {
        if (roundId == null) {
            return ApiResponse.error("roundId 不能为空");
        }

        return ApiResponse.ok(interviewService.submitAnswer(roundId, request));
    }


    @GetMapping("/runs/{runId}/question-rounds")
    public ApiResponse<InterviewQuestionRoundPageResponse> pageQuestionRounds(
            @PathVariable Long runId,
            @Valid InterviewQuestionRoundPageRequest request) {

        return ApiResponse.ok(interviewService.pageQuestionRounds(runId, request));
    }

    @PostMapping("/runs/{runId}/finish")
    public ApiResponse<Void> finishRun (@PathVariable Long runId) {
        if (runId == null) {
            return ApiResponse.error("roundId 不能为空");
        }
        interviewService.finishRun(runId);

        return  ApiResponse.ok();
    }

}
