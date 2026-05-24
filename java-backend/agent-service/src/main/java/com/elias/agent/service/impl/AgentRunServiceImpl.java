package com.elias.agent.service.impl;

import com.elias.common.client.ResumeClient;
import com.elias.common.AgentRunStage;
import com.elias.common.AgentRunStatus;
import com.elias.common.ApiResponse;
import com.elias.common.dto.request.ResumePatchApplyRequest;
import com.elias.common.dto.request.ResumeSectionPatchRequest;
import com.elias.common.dto.response.ResumeDetailResponse;
import com.elias.common.dto.response.ResumePatchApplyResponse;
import com.elias.common.context.UserContext;
import com.elias.agent.dto.run.request.AgentRunJobMessage;
import com.elias.agent.dto.run.request.AgentApproveRequest;
import com.elias.agent.dto.run.request.AgentCancelRequest;
import com.elias.agent.dto.run.request.AgentContinueRequest;
import com.elias.agent.dto.run.request.AgentRunStartRequest;
import com.elias.agent.entity.AgentMessage;
import com.elias.agent.entity.AgentSession;
import com.elias.agent.entity.AiAgentRun;
import com.elias.agent.entity.AiRunEvent;
import com.elias.common.exception.BusinessException;
import com.elias.agent.mapper.AgentMessageMapper;
import com.elias.agent.mapper.AgentSessionMapper;
import com.elias.agent.mapper.AiAgentRunMapper;
import com.elias.agent.mapper.AiRunEventMapper;
import com.elias.agent.service.AgentRunJobProducer;
import com.elias.agent.service.AgentRunService;
import com.elias.agent.dto.run.response.AgentApproveResponse;
import com.elias.agent.dto.run.response.AgentEventResponse;
import com.elias.agent.dto.run.response.AgentRunDetailResponse;
import com.elias.agent.dto.run.response.AgentRunQueueResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agent run 服务实现
 *
 * 当前主链路已经从"Java 同步代理 Python SSE"改为"Java 入队 + Python worker 消费"：
 * 1. startRun / continueRun 只负责校验、落库、入队，HTTP 请求不会等待 Python 执行完。
 * 2. streamRunEvents 只负责从 ai_run_event 补历史并等待新事件。
 * 3. Python worker 通过内部接口回写事件和状态，Java 表是前端查询的权威来源。
 */
@Service
@RequiredArgsConstructor
public class AgentRunServiceImpl implements AgentRunService {

    /**
     * 单次 Agent SSE 连接最长保留时间，避免浏览器长时间无响应时连接被过早关闭。
     */
    private static final long SSE_TIMEOUT = 300_000L;

    private static final long EVENT_POLL_INTERVAL_MS = 1_000L;

    /**
     * 单次 DB 轮询最多推送的事件数。
     * 前端断线重连时会带 afterSeq，所以这里不用一次查全量。
     */
    private static final int EVENT_BATCH_SIZE = 100;

    /**
     * 终态后额外空轮询次数。
     * Python 正常顺序是"先写事件，再改状态"，这里再等几次是为了兜极端网络时序。
     */
    private static final int END_STATUS_IDLE_POLL_COUNT = 2;

    /**
     * 这些状态表示 Python 不会再继续主动产生事件，本次 SSE 可以结束。
     */
    private static final List<String> STREAM_END_STATUS = List.of(
            AgentRunStatus.WAITING_USER.getCode(),
            AgentRunStatus.WAITING_CONFIRM.getCode(),
            AgentRunStatus.SUCCESS.getCode(),
            AgentRunStatus.FAILED.getCode(),
            AgentRunStatus.CANCELLED.getCode()
    );

    private final AiAgentRunMapper aiAgentRunMapper;
    private final AiRunEventMapper aiRunEventMapper;
    private final AgentSessionMapper agentSessionMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final ResumeClient resumeClient;
    private final AgentRunJobProducer agentRunJobProducer;
    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskExecutor agentStreamTaskExecutor;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    public AgentRunQueueResponse startRun(Long sessionId, AgentRunStartRequest request) {
        // 1. 创建 run 的事务只覆盖本地库写入；MQ 投递会延迟到事务提交后执行。
        // 2. 这样可以避免消息先发出，但 run 记录因为事务回滚而不存在。
        Long userId = UserContext.verifyGetUserId();
        AgentSession session = getOwnedSession(userId, sessionId);
        ResumeDetailResponse resume = getOwnedResume(userId, request.getResumeId());
        if (!resume.getId().equals(session.getResumeId())) {
            throw BusinessException.badRequest("run 的 resumeId 必须和 session 关联简历一致");
        }
        if (request.getJobDescription() != null && !request.getJobDescription().isBlank()) {
            // JD 属于 session 级长期上下文；用户本轮传了 JD，就更新到会话供后续 run 复用。
            session.setJobDescription(request.getJobDescription().trim());
            agentSessionMapper.updateById(session);
        }

        if (request.getClientRequestId() != null && !request.getClientRequestId().isBlank()) {
            AiAgentRun exists = aiAgentRunMapper.selectOne(new LambdaQueryWrapper<AiAgentRun>()
                    .eq(AiAgentRun::getUserId, userId)
                    .eq(AiAgentRun::getSessionId, sessionId)
                    .eq(AiAgentRun::getClientRequestId, request.getClientRequestId())
                    .last("limit 1"));
            if (exists != null) {
                return AgentRunQueueResponse.builder()
                        .runId(exists.getId())
                        .status(exists.getStatus())
                        .build();
            }
        }

        AiAgentRun activeRun = aiAgentRunMapper.selectOne(new LambdaQueryWrapper<AiAgentRun>()
                .eq(AiAgentRun::getUserId, userId)
                .eq(AiAgentRun::getSessionId, sessionId)
                .eq(AiAgentRun::getSceneCode, request.getSceneCode())
                .eq(AiAgentRun::getTargetSectionId, request.getTargetSectionId())
                .eq(AiAgentRun::getActiveFlag, 1)
                .last("limit 1"));
        if (activeRun != null) {
            return AgentRunQueueResponse.builder()
                    .runId(activeRun.getId())
                    .status(activeRun.getStatus())
                    .build();
        }

        // Java 先落 run，再把任务交给 MQ；Python worker 后续按 runId 拉取权威上下文。
        AiAgentRun run = AiAgentRun.builder()
                .userId(userId)
                .sessionId(sessionId)
                .resumeId(request.getResumeId())
                .sceneCode(request.getSceneCode())
                .status(AgentRunStatus.QUEUED.getCode())
                .currentStage(AgentRunStage.BOOTSTRAP.getCode())
                .userInput(request.getUserInput())
                .jobDescription(session.getJobDescription())
                .selectedSectionIdsJson(writeJson(List.of(request.getTargetSectionId())))
                .targetSectionId(request.getTargetSectionId())
                .activeFlag(1)
                .clientRequestId(request.getClientRequestId())
                .build();
        aiAgentRunMapper.insert(run);
        saveMessage(sessionId, "USER", request.getUserInput());

        // 这里只发最小任务消息；简历快照、schema、历史消息由 Python bootstrap 再回查 Java。
        publishAfterCommit(AgentRunJobMessage.builder()
                .jobType("START")
                .runId(run.getId())
                .sessionId(sessionId)
                .resumeId(request.getResumeId())
                .sceneCode(request.getSceneCode())
                .targetSectionId(request.getTargetSectionId())
                .userInput(request.getUserInput() == null ? "" : request.getUserInput())
                .jobDescription(session.getJobDescription() == null ? "" : session.getJobDescription())
                .build());
        return AgentRunQueueResponse.builder()
                .runId(run.getId())
                .status(run.getStatus())
                .build();
    }


    // TODO 或许可以考虑新建个service去写这个事件
    @Override
    public SseEmitter startRunStream(Long sessionId, AgentRunStartRequest request) {
        AgentRunQueueResponse response = transactionTemplate.execute(
                status -> startRun(sessionId, request)
        );
        if (response == null) {
            throw BusinessException.business("启动run失败");
        }
        return streamRunEvents(response.getRunId(), 0);
    }


    @Override
    @Transactional
    public AgentRunQueueResponse continueRun(Long runId, AgentContinueRequest request) {
        // continue 只允许从 WAITING_USER 恢复。
        // WAITING_CONFIRM 应该走 approveRun，RUNNING/QUEUED 说明当前还不能重复入队。
        Long userId = UserContext.verifyGetUserId();
        AiAgentRun run = getOwnedRun(userId, runId);
        if (!AgentRunStatus.WAITING_USER.getCode().equals(run.getStatus())) {
            throw BusinessException.badRequest("当前 run 不需要补充信息，不能继续执行");
        }
        // continue 只恢复同一个 run，不创建新 run；Python 侧通过 checkpoint 从暂停点继续。
        run.setStatus(AgentRunStatus.QUEUED.getCode());
        aiAgentRunMapper.updateById(run);
        Long targetSectionId = firstSelectedSectionId(run);
        if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            // 用户追问答案也保存到会话历史，后续 run 的记忆压缩可以复用。
            saveMessage(run.getSessionId(), "USER", writeJson(request.getAnswers()));
        }
        // CONTINUE 仍然投递同一个 runId，Python 侧用 LangGraph checkpoint 从暂停点恢复。
        publishAfterCommit(AgentRunJobMessage.builder()
                .jobType("CONTINUE")
                .runId(run.getId())
                .sessionId(run.getSessionId())
                .resumeId(run.getResumeId())
                .sceneCode(run.getSceneCode())
                .targetSectionId(targetSectionId)
                .userInput(run.getUserInput() == null ? "" : run.getUserInput())
                .jobDescription(run.getJobDescription() == null ? "" : run.getJobDescription())
                .answers(request.getAnswers() == null ? new ArrayList<>() : request.getAnswers())
                .build());
        return AgentRunQueueResponse.builder()
                .runId(run.getId())
                .status(run.getStatus())
                .build();
    }

    @Override
    public SseEmitter continueRunStream(Long runId, AgentContinueRequest request) {
        // 兼容旧前端入口：内部仍然走 MQ，再返回同一个 run 的事件流。
        AgentRunQueueResponse response = transactionTemplate.execute(
                status -> continueRun(runId, request)
        );
        if (response == null) {
            throw BusinessException.business("启动run失败");
        }
        return streamRunEvents(response.getRunId(), 0);
    }

    @Override
    @Transactional
    public AgentApproveResponse approveRun(Long runId, AgentApproveRequest request) {
        Long userId = UserContext.verifyGetUserId();
        AiAgentRun run = getOwnedRun(userId, runId);
        if (run.getApprovalPayload() == null || run.getApprovalPayload().isBlank()) {
            throw BusinessException.badRequest("当前 run 没有可应用的审批 payload");
        }

        // 审批以 Java run 表里的 approvalPayload 为准，不依赖 Python Redis checkpoint。
        Map<String, Object> approvalPayload = readMap(run.getApprovalPayload());
        List<ResumeSectionPatchRequest> patches = readPatches(approvalPayload.get("patches"));
        if (request.getApprovedPatchIds() != null && !request.getApprovedPatchIds().isEmpty()) {
            patches = patches.stream().filter(patch -> request.getApprovedPatchIds().contains(patch.getPatchId())).toList();
        }
        if (patches.isEmpty()) {
            throw BusinessException.badRequest("没有选中可应用的 patch");
        }

        // 写库前由 ResumePatchService 做 section 归属和 schema 校验。
        ResumePatchApplyResponse applyResponse = resumeClient.applyPatch(run.getResumeId(), new ResumePatchApplyRequest(runId, patches));
        run.setStatus(AgentRunStatus.SUCCESS.getCode());
        run.setActiveFlag(null);
        run.setCompletedAt(LocalDateTime.now());
        run.setResultSummary(String.valueOf(approvalPayload.getOrDefault("summary", "已应用当前模块修改")));
        aiAgentRunMapper.updateById(run);
        saveMessage(run.getSessionId(), "ASSISTANT", run.getResultSummary());

        return AgentApproveResponse.builder()
                .runId(runId)
                .status(run.getStatus())
                .resumeId(run.getResumeId())
                .appliedPatchCount(applyResponse.getAppliedPatchCount())
                .build();
    }

    @Override
    @Transactional
    public void cancelRun(Long runId, AgentCancelRequest request) {
        Long userId = UserContext.verifyGetUserId();
        AiAgentRun run = getOwnedRun(userId, runId);
        run.setStatus(AgentRunStatus.CANCELLED.getCode());
        run.setActiveFlag(null);
        run.setCompletedAt(LocalDateTime.now());
        aiAgentRunMapper.updateById(run);
    }

    @Override
    public AgentRunDetailResponse getRunDetail(Long runId) {
        Long userId = UserContext.verifyGetUserId();
        AiAgentRun run = getOwnedRun(userId, runId);
        return AgentRunDetailResponse.builder()
                .runId(run.getId())
                .sessionId(run.getSessionId())
                .resumeId(run.getResumeId())
                .sceneCode(run.getSceneCode())
                .status(run.getStatus())
                .currentStage(run.getCurrentStage())
                .resultSummary(run.getResultSummary())
                .clarificationPayload(readMap(run.getClarificationPayload()))
                .approvalPayload(readMap(run.getApprovalPayload()))
                .errorMessage(run.getErrorMessage())
                .createdAt(run.getCreatedAt())
                .updatedAt(run.getUpdatedAt())
                .completedAt(run.getCompletedAt())
                .build();
    }

    @Override
    public List<AgentEventResponse> listRunEvents(Long runId, Integer page, Integer pageSize) {
        Long userId = UserContext.verifyGetUserId();
        getOwnedRun(userId, runId);
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = pageSize == null || pageSize < 1 ? 100 : Math.min(pageSize, 200);
        return aiRunEventMapper.selectList(new LambdaQueryWrapper<AiRunEvent>()
                        .eq(AiRunEvent::getRunId, runId)
                        .orderByAsc(AiRunEvent::getEventSeq)
                        .last("limit " + ((safePage - 1) * safeSize) + "," + safeSize))
                .stream()
                .map(event -> AgentEventResponse.builder()
                        .id(event.getId())
                        .runId(event.getRunId())
                        .eventSeq(event.getEventSeq())
                        .eventType(event.getEventType())
                        .stageCode(event.getStageCode())
                        .message(event.getMessage())
                        .payload(readMap(event.getPayloadJson()))
                        .createdAt(event.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    public SseEmitter streamRunEvents(Long runId, Integer afterSeq) {
        // 这个接口不直接调用 Python。
        // 它只读 ai_run_event，因此前端刷新或断线后可以用 afterSeq 补齐历史事件。
        Long userId = UserContext.verifyGetUserId();
        getOwnedRun(userId, runId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        int startSeq = afterSeq == null || afterSeq < 0 ? 0 : afterSeq;
        try {
            agentStreamTaskExecutor.execute(() -> {
                int lastSeq = startSeq;
                int endStatusIdlePollCount = 0;
                try {
                    while (true) {
                        List<AiRunEvent> newEvents = listEventsAfter(runId, lastSeq);
                        for (AiRunEvent event : newEvents) {
                            AgentEventResponse response = buildEventResponse(event);
                            // SSE id 使用 eventSeq，浏览器/前端可以记录最后收到的序号后重连。
                            emitter.send(SseEmitter.event()
                                    .id(String.valueOf(response.getEventSeq()))
                                    .name(response.getEventType())
                                    .data(writeJson(response)));
                            lastSeq = response.getEventSeq();
                        }
                        AiAgentRun run = aiAgentRunMapper.selectById(runId);
                        if (run == null) {
                            emitter.complete();
                            return;
                        }
                        if (!newEvents.isEmpty()) {
                            endStatusIdlePollCount = 0;
                        } else if (STREAM_END_STATUS.contains(run.getStatus())) {
                            // Python 会先写事件再回写 WAITING/SUCCESS 等状态；这里再等一次轮询，
                            // 避免极端时序下状态已变但最后一条事件还没落库，导致 SSE 提前关闭。
                            endStatusIdlePollCount++;
                            if (endStatusIdlePollCount >= END_STATUS_IDLE_POLL_COUNT) {
                                emitter.complete();
                                return;
                            }
                        }
                        Thread.sleep(EVENT_POLL_INTERVAL_MS);
                    }
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            });
        } catch (TaskRejectedException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    private List<AiRunEvent> listEventsAfter(Long runId, Integer afterSeq) {
        // event_seq 在同一个 run 内递增；这里严格大于 afterSeq，避免重连时重复推送已确认事件。
        return aiRunEventMapper.selectList(new LambdaQueryWrapper<AiRunEvent>()
                .eq(AiRunEvent::getRunId, runId)
                .gt(AiRunEvent::getEventSeq, afterSeq)
                .orderByAsc(AiRunEvent::getEventSeq)
                .last("limit " + EVENT_BATCH_SIZE));
    }

    private AgentEventResponse buildEventResponse(AiRunEvent event) {
        return AgentEventResponse.builder()
                .id(event.getId())
                .runId(event.getRunId())
                .eventSeq(event.getEventSeq())
                .eventType(event.getEventType())
                .stageCode(event.getStageCode())
                .message(event.getMessage())
                .payload(readMap(event.getPayloadJson()))
                .createdAt(event.getCreatedAt())
                .build();
    }

    private void publishAfterCommit(AgentRunJobMessage message) {
        Runnable publishTask = () -> {
            try {
                agentRunJobProducer.publish(message);
            } catch (Exception e) {
                // 入队失败时不能让 run 永远停在 QUEUED；直接标记 FAILED 方便前端展示。
                markFailed(message.getRunId(), "Agent run 入队失败: " + e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 事务提交后再投递 MQ，保证 Python worker 消费到消息时 run 已经可查询。
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishTask.run();
                }
            });
            return;
        }
        publishTask.run();
    }

    private AgentSession getOwnedSession(Long userId, Long sessionId) {
        // 前端所有 run 操作都要校验用户归属，避免跨用户读取会话和简历。
        AgentSession session = agentSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId()) || "DELETED".equals(session.getStatus())) {
            throw BusinessException.notFound("Agent 会话不存在或无权限访问");
        }
        return session;
    }

    private AiAgentRun getOwnedRun(Long userId, Long runId) {
        // run 是后续事件流、审批、取消的统一权限入口。
        AiAgentRun run = aiAgentRunMapper.selectById(runId);
        if (run == null || !userId.equals(run.getUserId())) {
            throw BusinessException.notFound("Agent run 不存在或无权限访问");
        }
        return run;
    }

    private ResumeDetailResponse getOwnedResume(Long userId, Long resumeId) {
        ApiResponse<ResumeDetailResponse> detailedResume = resumeClient.getDetailedResume(resumeId);
        ResumeDetailResponse resume = detailedResume.getData();
        if (resume == null || !userId.equals(resume.getUserId())) {
            throw BusinessException.notFound("简历不存在或无权限访问");
        }
        return resume;
    }

    private void saveMessage(Long sessionId, String role, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        // MySQL 保存完整关键历史，模型调用时再由 Python 按 summary + 最近消息裁剪。
        AgentMessage last = agentMessageMapper.selectOne(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, sessionId)
                .orderByDesc(AgentMessage::getSeqNo)
                .last("limit 1"));
        agentMessageMapper.insert(AgentMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content.trim())
                .contentType("TEXT")
                .seqNo(last == null ? 1 : last.getSeqNo() + 1)
                .status("SUCCESS")
                .build());
        AgentSession session = agentSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setLastMessageAt(LocalDateTime.now());
            agentSessionMapper.updateById(session);
        }
    }

    private void markFailed(Long runId, String message) {
        // 这个方法用于 MQ 投递失败等 Java 本地失败场景。
        // Python 执行失败会通过 InternalAgentSupportServiceImpl.updateRunStatus 回写。
        AiAgentRun run = aiAgentRunMapper.selectById(runId);
        if (run != null) {
            run.setStatus(AgentRunStatus.FAILED.getCode());
            run.setErrorMessage(message);
            run.setActiveFlag(null);
            run.setCompletedAt(LocalDateTime.now());
            aiAgentRunMapper.updateById(run);
        }
    }

    private List<ResumeSectionPatchRequest> readPatches(Object value) {
        return objectMapper.convertValue(value == null ? new ArrayList<>() : value, new TypeReference<List<ResumeSectionPatchRequest>>() {});
    }

    private Long firstSelectedSectionId(AiAgentRun run) {
        if (run.getSelectedSectionIdsJson() == null || run.getSelectedSectionIdsJson().isBlank()) {
            throw BusinessException.badRequest("当前 run 缺少目标模块");
        }
        try {
            List<Long> sectionIds = objectMapper.readValue(run.getSelectedSectionIdsJson(), new TypeReference<List<Long>>() {});
            if (sectionIds.isEmpty()) {
                throw BusinessException.badRequest("当前 run 缺少目标模块");
            }
            return sectionIds.get(0);
        } catch (Exception e) {
            throw BusinessException.badRequest("目标模块解析失败");
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception e) {
            throw BusinessException.badRequest("JSON 序列化失败");
        }
    }
}
