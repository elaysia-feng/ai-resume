package com.elias.controller.agent;

import com.elias.common.ApiResponse;
import com.elias.dto.common.agent.internal.request.InternalAgentRunCreateRequest;
import com.elias.dto.common.agent.internal.request.InternalBootstrapRequest;
import com.elias.dto.common.agent.internal.request.RunEventBatchRequest;
import com.elias.dto.common.agent.internal.request.RunStatusUpdateRequest;
import com.elias.dto.common.agent.internal.response.InternalAgentRunCreateResponse;
import com.elias.dto.common.agent.internal.response.InternalAgentRunDetailResponse;
import com.elias.dto.common.agent.internal.response.InternalBootstrapResponse;
import com.elias.dto.common.interview.internal.request.InternalInterviewBootstrapRequest;
import com.elias.dto.common.interview.internal.response.InternalInterviewBootstrapResponse;
import com.elias.service.agent.InternalAgentSupportService;
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

    @PostMapping("/runs/interview")
    public ApiResponse<InternalAgentRunCreateResponse> createInterviewRun(
            @RequestBody InternalAgentRunCreateRequest request) {
        return ApiResponse.ok(internalAgentSupportService.createInterviewRun(request));
    }

    @GetMapping("/runs/{runId}")
    public ApiResponse<InternalAgentRunDetailResponse> getInternalRunDetail(@PathVariable Long runId) {
        return ApiResponse.ok(internalAgentSupportService.getInternalRunDetail(runId));
    }

    /**
     * 批量保存 Python 上报的 run 事件
     *
     * <p>events/batch 是 Python 侧实际调用的路径，events/saveBatch 保留为历史兼容别名。</p>
     *
     * @param runId   run ID
     * @param request 事件批量请求
     * @return 空响应
     */
    @PostMapping({"/runs/{runId}/events/saveBatch", "/runs/{runId}/events/batch"})
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

    /**
     * Python worker 执行前抢占 run。
     */
    @PostMapping("/runs/{runId}/claim")
    public ApiResponse<Boolean> claimRun(@PathVariable Long runId) {
        return ApiResponse.ok(internalAgentSupportService.claimRun(runId));
    }

    @GetMapping("/runs/{runId}/status")
    public ApiResponse<String> getRunStatus(@PathVariable Long runId) {
        return ApiResponse.ok(internalAgentSupportService.getRunStatus(runId));
    }

}
