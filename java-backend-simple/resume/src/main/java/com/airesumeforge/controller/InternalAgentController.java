package com.airesumeforge.controller;

import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.dto.agent.internal.request.InternalBootstrapRequest;
import com.airesumeforge.dto.agent.internal.request.RunEventBatchRequest;
import com.airesumeforge.dto.agent.internal.request.RunStatusUpdateRequest;
import com.airesumeforge.dto.interview.internal.request.InternalInterviewBootstrapRequest;
import com.airesumeforge.dto.interview.internal.request.InternalInterviewQuestionAnalysisRequest;
import com.airesumeforge.service.InternalAgentSupportService;
import com.airesumeforge.dto.agent.internal.response.InternalBootstrapResponse;
import com.airesumeforge.dto.interview.internal.response.InternalInterviewBootstrapResponse;
import com.airesumeforge.dto.interview.internal.response.InternalInterviewRoundDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Python Agent 内部接口控制器
 * 仅供 Python 后端调用，不面向前端
 */
@RestController
@RequestMapping("/internal/agent")
@RequiredArgsConstructor
public class InternalAgentController {

    private final InternalAgentSupportService internalAgentSupportService;

    /**
     * 加载 Agent 启动上下文
     *
     * @param request bootstrap 请求
     * @return 简历快照、schema、历史消息和约束
     */
    @PostMapping("/bootstrap")
    public InternalBootstrapResponse bootstrap(@Valid @RequestBody InternalBootstrapRequest request) {
        return internalAgentSupportService.bootstrap(request);
    }

    @PostMapping("/interviewBootstrap")
    public InternalInterviewBootstrapResponse interviewBootstrap(@RequestBody InternalInterviewBootstrapRequest request) {
        return internalAgentSupportService.interviewBootstrap(request);
    }


    /**
     * 批量保存 Python 上报的 run 事件
     *
     * @param runId   run ID
     * @param request 事件批量请求
     * @return 空响应
     */
    @PostMapping("/runs/{runId}/events/saveBatch")
    public ApiResponse<Void> saveRunEvents(@PathVariable Long runId,
                                           @Valid @RequestBody RunEventBatchRequest request) {
        internalAgentSupportService.saveRunEvents(runId, request);
        return ApiResponse.ok();
    }

    /**
     * 更新 run 状态
     *
     * @param runId   run ID
     * @param request 状态更新请求
     * @return 空响应
     */
    @PostMapping("/runs/{runId}/status")
    public ApiResponse<Void> updateRunStatus(@PathVariable Long runId,
                                             @Valid @RequestBody RunStatusUpdateRequest request) {
        internalAgentSupportService.updateRunStatus(runId, request);
        return ApiResponse.ok();
    }

    @PostMapping("/interview/rounds/{roundId}/analysis")
    public ApiResponse<Void> updateQuestionAnalysis(
            @PathVariable Long roundId,
            @Valid @RequestBody InternalInterviewQuestionAnalysisRequest request) {
        internalAgentSupportService.updateQuestionAnalysis(roundId, request);
        return ApiResponse.ok();
    }

    /**
     *  得到用户回答的问题答案
     * @param roundId 轮次
     * @return 返回问题和问题答案
     */
    @GetMapping("/interview/rounds/{roundId}")
    public ApiResponse<InternalInterviewRoundDetailResponse> getQuestionAnswer(@PathVariable Long roundId) {
        if (roundId == null) {
            return ApiResponse.error("roundId 不能为空");
        }

        return ApiResponse.ok(internalAgentSupportService.getQuestionAnswer(roundId));
    }


}

