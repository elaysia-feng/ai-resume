package com.airesumeforge.agent.controller;

import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.agent.dto.run.request.AgentApproveRequest;
import com.airesumeforge.agent.dto.run.request.AgentCancelRequest;
import com.airesumeforge.agent.dto.run.request.AgentContinueRequest;
import com.airesumeforge.agent.dto.run.request.AgentRunStartRequest;
import com.airesumeforge.agent.service.AgentRunService;
import com.airesumeforge.agent.dto.run.response.AgentApproveResponse;
import com.airesumeforge.agent.dto.run.response.AgentEventResponse;
import com.airesumeforge.agent.dto.run.response.AgentRunDetailResponse;
import com.airesumeforge.agent.dto.run.response.AgentRunQueueResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Agent run 控制器
 * 负责前端 Agent 任务入口、事件订阅、继续、取消、审批和查询。
 *
 * 新前端推荐流程：
 * 1. POST /sessions/{sessionId}/runs 先拿 runId 和 QUEUED。
 * 2. GET /runs/{runId}/events/stream 订阅事件。
 * 3. 断线后用 afterSeq 继续补事件。
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentRunController {

    private final AgentRunService agentRunService;

    /**
     * 创建 Agent run 并入队。
     *
     * @param sessionId 会话ID
     * @param request   启动请求
     * @return runId 和 QUEUED 状态
     */
    @PostMapping("/sessions/{sessionId}/runs")
    public ApiResponse<AgentRunQueueResponse> startRun(@PathVariable Long sessionId,
                                                       @Valid @RequestBody AgentRunStartRequest request) {
        return ApiResponse.ok(agentRunService.startRun(sessionId, request));
    }

    /**
     * 启动 Agent run
     *
     * 兼容旧入口：创建 run 后立刻返回该 run 的事件流。
     * 新前端不建议再依赖"创建请求本身就是流式输出"。
     *
     * @param sessionId 会话ID
     * @param request   启动请求
     * @return SSE 流
     */
    @PostMapping("/sessions/{sessionId}/runs/stream")
    public SseEmitter startRunStream(@PathVariable Long sessionId,
                                     @Valid @RequestBody AgentRunStartRequest request) {
        return agentRunService.startRunStream(sessionId, request);
    }

    /**
     * 用户补充信息后把 run 重新入队。
     *
     * @param runId   run ID
     * @param request 继续请求
     * @return runId 和 QUEUED 状态
     */
    @PostMapping("/runs/{runId}/continue")
    public ApiResponse<AgentRunQueueResponse> continueRun(@PathVariable Long runId,
                                                          @Valid @RequestBody AgentContinueRequest request) {
        return ApiResponse.ok(agentRunService.continueRun(runId, request));
    }

    /**
     * 用户补充信息后继续 run
     *
     * 兼容旧入口：continue 入队后立刻返回同一个 run 的事件流。
     *
     * @param runId   run ID
     * @param request 继续请求
     * @return SSE 流
     */
    @PostMapping("/runs/{runId}/continue/stream")
    public SseEmitter continueRunStream(@PathVariable Long runId,
                                        @Valid @RequestBody AgentContinueRequest request) {
        return agentRunService.continueRunStream(runId, request);
    }

    /**
     * 订阅 run 事件流。
     *
     * 事件来源是 ai_run_event 表，不直接访问 Python。
     * 这样前端刷新页面后仍然可以从最后一个 eventSeq 补齐事件。
     *
     * @param runId    run ID
     * @param afterSeq 已接收的最后事件序号
     * @return SSE 流
     */
    @GetMapping("/runs/{runId}/events/stream")
    public SseEmitter streamRunEvents(@PathVariable Long runId,
                                      @RequestParam(defaultValue = "0") Integer afterSeq) {
        return agentRunService.streamRunEvents(runId, afterSeq);
    }

    /**
     * 确认应用 Agent 生成的 patch
     *
     * @param runId   run ID
     * @param request 审批请求
     * @return 应用结果
     */
    @PostMapping("/runs/{runId}/approve")
    public ApiResponse<AgentApproveResponse> approveRun(@PathVariable Long runId,
                                                        @Valid @RequestBody AgentApproveRequest request) {
        return ApiResponse.ok(agentRunService.approveRun(runId, request));
    }

    /**
     * 取消 run
     *
     * @param runId   run ID
     * @param request 取消请求
     * @return 空响应
     */
    @PostMapping("/runs/{runId}/cancel")
    public ApiResponse<Void> cancelRun(@PathVariable Long runId,
                                       @RequestBody(required = false) AgentCancelRequest request) {
        agentRunService.cancelRun(runId, request == null ? new AgentCancelRequest() : request);
        return ApiResponse.ok();
    }

    /**
     * 查询 run 详情
     *
     * @param runId run ID
     * @return run 详情
     */
    @GetMapping("/runs/{runId}")
    public ApiResponse<AgentRunDetailResponse> getRunDetail(@PathVariable Long runId) {
        return ApiResponse.ok(agentRunService.getRunDetail(runId));
    }

    /**
     * 查询 run 事件列表
     *
     * @param runId    run ID
     * @param page     页码
     * @param pageSize 每页数量
     * @return 事件列表
     */
    @GetMapping("/runs/{runId}/events")
    public ApiResponse<List<AgentEventResponse>> listRunEvents(@PathVariable Long runId,
                                                               @RequestParam(defaultValue = "1") Integer page,
                                                               @RequestParam(defaultValue = "100") Integer pageSize) {
        return ApiResponse.ok(agentRunService.listRunEvents(runId, page, pageSize));
    }
}
