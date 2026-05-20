package com.elias.agent.service;

import com.elias.agent.dto.run.request.AgentApproveRequest;
import com.elias.agent.dto.run.request.AgentCancelRequest;
import com.elias.agent.dto.run.request.AgentContinueRequest;
import com.elias.agent.dto.run.request.AgentRunStartRequest;
import com.elias.agent.dto.run.response.AgentApproveResponse;
import com.elias.agent.dto.run.response.AgentEventResponse;
import com.elias.agent.dto.run.response.AgentRunDetailResponse;
import com.elias.agent.dto.run.response.AgentRunQueueResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Agent run 业务接口
 * ServiceImpl 负责 run 创建、状态流转、事件订阅和审批应用。
 *
 * MQ 改造后的职责边界：
 * 1. Java 负责创建 run、入队、查询、SSE 事件订阅。
 * 2. Python worker 负责消费任务、执行 LangGraph、回写事件和状态。
 * 3. 旧 stream 方法只做兼容，新链路优先使用 startRun / continueRun / streamRunEvents。
 */
public interface AgentRunService {

    /**
     * 创建 Agent run 并投递到 MQ。
     *
     * @param sessionId Agent 会话ID
     * @param request   启动请求
     * @return 入队结果
     */
    AgentRunQueueResponse startRun(Long sessionId, AgentRunStartRequest request);

    /**
     * 启动 Agent run 并返回 SSE 流
     *
     * @param sessionId Agent 会话ID
     * @param request   启动请求
     * @return SSE emitter
     */
    SseEmitter startRunStream(Long sessionId, AgentRunStartRequest request);

    /**
     * 用户补充信息后把旧 run 重新投递到 MQ。
     *
     * @param runId   run ID
     * @param request 继续请求
     * @return 入队结果
     */
    AgentRunQueueResponse continueRun(Long runId, AgentContinueRequest request);

    /**
     * 用户补充信息后继续 Agent run
     *
     * @param runId   run ID
     * @param request 继续请求
     * @return SSE emitter
     */
    SseEmitter continueRunStream(Long runId, AgentContinueRequest request);

    /**
     * 订阅 run 事件流。
     *
     * @param runId    run ID
     * @param afterSeq 只推送该序号之后的事件
     * @return SSE emitter
     */
    SseEmitter streamRunEvents(Long runId, Integer afterSeq);

    /**
     * 用户确认后应用 patch
     *
     * @param runId   run ID
     * @param request 审批请求
     * @return 应用结果
     */
    AgentApproveResponse approveRun(Long runId, AgentApproveRequest request);

    /**
     * 取消 Agent run
     *
     * @param runId   run ID
     * @param request 取消请求
     */
    void cancelRun(Long runId, AgentCancelRequest request);

    /**
     * 查询 run 详情
     *
     * @param runId run ID
     * @return run 详情
     */
    AgentRunDetailResponse getRunDetail(Long runId);

    /**
     * 查询 run 事件列表
     *
     * @param runId    run ID
     * @param page     页码
     * @param pageSize 每页数量
     * @return 事件列表
     */
    List<AgentEventResponse> listRunEvents(Long runId, Integer page, Integer pageSize);
}
