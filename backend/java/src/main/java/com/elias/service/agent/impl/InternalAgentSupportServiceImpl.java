package com.elias.service.agent.impl;

import com.elias.mapper.agent.AgentMessageMapper;
import com.elias.mapper.agent.AgentSessionMapper;
import com.elias.mapper.agent.AiAgentRunMapper;
import com.elias.mapper.agent.AiRunEventMapper;
import com.elias.common.AgentRunStage;
import com.elias.common.AgentRunStatus;
import com.elias.common.AgentSceneCode;
import com.elias.common.SectionSchema;
import com.elias.dto.common.agent.internal.request.InternalAgentRunCreateRequest;
import com.elias.dto.common.agent.internal.request.InternalBootstrapRequest;
import com.elias.dto.common.agent.internal.request.RunEventBatchRequest;
import com.elias.dto.common.agent.internal.request.RunStatusUpdateRequest;
import com.elias.dto.common.agent.internal.response.InternalAgentRunCreateResponse;
import com.elias.dto.common.agent.internal.response.InternalAgentRunDetailResponse;
import com.elias.dto.common.agent.internal.response.BootstrapConstraintsResponse;
import com.elias.dto.common.agent.internal.response.HistoryMessageResponse;
import com.elias.dto.common.agent.internal.response.InternalBootstrapResponse;
import com.elias.dto.common.interview.internal.request.InternalInterviewBootstrapRequest;
import com.elias.dto.common.interview.internal.request.InternalInterviewQuestionAnalysisRequest;
import com.elias.dto.common.interview.internal.response.InternalInterviewBootstrapResponse;
import com.elias.dto.common.interview.internal.response.InternalInterviewRoundDetailResponse;
import com.elias.dto.common.interview.response.InterviewOptionResponse;

import com.elias.dto.common.response.ResumeSnapshotResponse;
import com.elias.dto.common.response.SectionResponse;
import com.elias.entity.interview.AiInterviewRound;
import com.elias.mapper.interview.AiInterviewRoundMapper;
import com.elias.service.resume.ResumePatchService;

import com.elias.entity.agent.AiAgentRun;
import com.elias.entity.agent.AgentMessage;
import com.elias.entity.agent.AgentSession;
import com.elias.exception.BusinessException;
import com.elias.service.agent.InternalAgentSupportService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Python Agent 内部支持服务实现
 *
 * 这些接口只给 Python worker / graph 调用：
 * 1. bootstrap：Python 用 runId/sessionId/resumeId 换取 Java 权威上下文。
 * 2. saveRunEvents：Python 把每个节点事件落到 ai_run_event，前端 SSE 再从这里读。
 * 3. updateRunStatus：Python 把 RUNNING / WAITING / FAILED 等状态回写到 ai_agent_run。
 */
@Service
@RequiredArgsConstructor
public class InternalAgentSupportServiceImpl implements InternalAgentSupportService {

    /**
     * 每次 bootstrap 给 Python 的最近消息数量。
     * 更早的长期上下文由 session.summary 承担，避免一次性把完整历史塞给模型。
     */
    private static final int RECENT_MESSAGE_LIMIT = 20;

    private final AiAgentRunMapper aiAgentRunMapper;
    private final AiRunEventMapper aiRunEventMapper;
    private final AgentSessionMapper agentSessionMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final ResumePatchService resumePatchService;
    private final AiInterviewRoundMapper aiInterviewRoundMapper;
    private final ObjectMapper objectMapper;

    @Override
    public InternalBootstrapResponse bootstrap(InternalBootstrapRequest request) {
        // Python 启动 graph 时只带 ID；完整简历、schema、会话记忆都从 Java 权威库加载。
        AiAgentRun run = aiAgentRunMapper.selectById(request.getRunId());
        AgentSession session = agentSessionMapper.selectById(request.getSessionId());
        ResumeSnapshotResponse resume = resumePatchService.getSnapshot(request.getResumeId());
        if (run == null || session == null || resume == null) {
            throw BusinessException.notFound("Agent bootstrap 上下文不存在");
        }

        return InternalBootstrapResponse.builder()
                .runId(run.getId())
                .sessionId(session.getId())
                .resume(resume)
                .jobDescription(session.getJobDescription() != null ? session.getJobDescription() : run.getJobDescription())
                .summary(session.getSummary())
                .schemas(buildSchemas(resume.getSections()))
                .messages(listRecentMessages(session.getId()))
                // v1 仍然只允许单模块优化，入口字段继续用 targetSectionId。
                .editableSectionIds(List.of(request.getTargetSectionId()))
                // TODO(to be confirmed): 按业务场景提供合理约束，替换当前硬编码值。
                .constraints(BootstrapConstraintsResponse.builder()
                        .allowCreateNewSection(false)
                        .allowDeleteSection(false)
                        .allowedPatchOperation(List.of("REPLACE_SECTION_CONTENT"))
                        .build())
                .build();
    }

    @Override
    @Transactional
    public void saveRunEvents(Long runId, RunEventBatchRequest request) {
        // 事件表是前端断线重连的基础，所以这里按 run_id + event_seq 做幂等。
        // 即使 Python 重试批量上报，也不会生成重复的前端事件。
        for (com.elias.dto.common.agent.internal.request.RunEventRequest event : request.getEvents()) {
            // Python 流式重试时可能重复上报同一 eventSeq，这里做幂等落库。
            Long exists = aiRunEventMapper.selectCount(new LambdaQueryWrapper<com.elias.entity.agent.AiRunEvent>()
                    .eq(com.elias.entity.agent.AiRunEvent::getRunId, runId)
                    .eq(com.elias.entity.agent.AiRunEvent::getEventSeq, event.getEventSeq()));
            if (exists > 0) {
                continue;
            }
            aiRunEventMapper.insert(com.elias.entity.agent.AiRunEvent.builder()
                    .runId(runId)
                    .eventSeq(event.getEventSeq())
                    .eventType(event.getEventType())
                    .stageCode(event.getStageCode())
                    .message(event.getMessage())
                    .payloadJson(writeJson(event.getPayload()))
                    .build());
        }
    }

    @Override
    @Transactional
    public void updateRunStatus(Long runId, RunStatusUpdateRequest request) {
        // Java 表是前端展示的权威状态；Python 不直接改业务库，只通过这个内部接口回写。
        AiAgentRun run = aiAgentRunMapper.selectById(runId);
        if (run == null) {
            throw BusinessException.notFound("Agent run 不存在");
        }
        run.setStatus(request.getStatus());
        if (request.getCurrentStage() != null) {
            run.setCurrentStage(request.getCurrentStage());
        }
        if (request.getResultSummary() != null) {
            run.setResultSummary(request.getResultSummary());
        }
        if (request.getClarificationPayload() != null) {
            run.setClarificationPayload(writeJson(request.getClarificationPayload()));
        }
        if (request.getApprovalPayload() != null) {
            // WAITING_CONFIRM 时保存审批包，后续 approveRun 直接读取这里并应用 patch。
            run.setApprovalPayload(writeJson(request.getApprovalPayload()));
        }
        if (request.getErrorMessage() != null) {
            run.setErrorMessage(request.getErrorMessage());
        }
        if (List.of(AgentRunStatus.SUCCESS.getCode(), AgentRunStatus.FAILED.getCode(), AgentRunStatus.CANCELLED.getCode()).contains(request.getStatus())) {
            // WAITING_USER / WAITING_CONFIRM 不是最终完成，只是等待用户下一步操作。
            run.setCompletedAt(LocalDateTime.now());
            run.setActiveFlag(0);
        }
        aiAgentRunMapper.updateById(run);

        if (request.getResultSummary() != null && !request.getResultSummary().isBlank()) {
            // summary 是 session 级长期记忆，Python 压缩后回写到 Java，后续 run 继续复用。
            AgentSession session = agentSessionMapper.selectById(run.getSessionId());
            if (session != null) {
                session.setSummary(request.getResultSummary());
                agentSessionMapper.updateById(session);
            }
        }
    }

    @Override
    @Transactional
    public boolean claimRun(Long runId) {
        return aiAgentRunMapper.update(null, new LambdaUpdateWrapper<AiAgentRun>()
                .eq(AiAgentRun::getId, runId)
                .eq(AiAgentRun::getStatus, AgentRunStatus.QUEUED.getCode())
                .set(AiAgentRun::getStatus, AgentRunStatus.RUNNING.getCode())
                .set(AiAgentRun::getActiveFlag, 1)
                .set(AiAgentRun::getUpdatedAt, LocalDateTime.now())) == 1;
    }

    @Override
    public InternalInterviewBootstrapResponse interviewBootstrap(InternalInterviewBootstrapRequest request) {
        AiAgentRun run = aiAgentRunMapper.selectById(request.getRunId());
        AgentSession session = agentSessionMapper.selectById(request.getSessionId());
        ResumeSnapshotResponse resume = resumePatchService.getSnapshot(request.getResumeId());
        if (run == null || session == null || resume == null) {
            throw BusinessException.notFound("上下文不存在");
        }

        return InternalInterviewBootstrapResponse.builder()
                .runId(request.getRunId())
                .sessionId(request.getSessionId())
                .jobDescription(session.getJobDescription() != null ? session.getJobDescription() : run.getJobDescription())
                .resume(resume)
                .summary(session.getSummary())
                .build();

    }

    @Override
    @Transactional
    public InternalAgentRunCreateResponse createInterviewRun(InternalAgentRunCreateRequest request) {
        AgentSession session = agentSessionMapper.selectById(request.getSessionId());
        if (session == null || !request.getUserId().equals(session.getUserId())) {
            throw BusinessException.notFound("Agent 会话不存在");
        }
        if (!AgentSceneCode.INTERVIEW.equals(session.getSceneCode())) {
            throw BusinessException.badRequest("当前会话不是面试场景");
        }
        if (!request.getResumeId().equals(session.getResumeId())) {
            throw BusinessException.badRequest("run 的 resumeId 必须和 session 关联简历一致");
        }
        Long targetSectionId = request.getTargetSectionId() == null ? 0L : request.getTargetSectionId();

        if (request.getClientRequestId() != null && !request.getClientRequestId().isBlank()) {
            AiAgentRun exists = aiAgentRunMapper.selectOne(new LambdaQueryWrapper<AiAgentRun>()
                    .eq(AiAgentRun::getUserId, request.getUserId())
                    .eq(AiAgentRun::getSessionId, request.getSessionId())
                    .eq(AiAgentRun::getClientRequestId, request.getClientRequestId())
                    .last("limit 1"));
            if (exists != null) {
                return InternalAgentRunCreateResponse.builder()
                        .runId(exists.getId())
                        .status(exists.getStatus())
                        .build();
            }
        }

        AiAgentRun activeRun = aiAgentRunMapper.selectOne(new LambdaQueryWrapper<AiAgentRun>()
                .eq(AiAgentRun::getUserId, request.getUserId())
                .eq(AiAgentRun::getSessionId, request.getSessionId())
                .eq(AiAgentRun::getSceneCode, request.getSceneCode())
                .eq(AiAgentRun::getTargetSectionId, targetSectionId)
                .eq(AiAgentRun::getActiveFlag, 1)
                .last("limit 1"));
        if (activeRun != null) {
            return InternalAgentRunCreateResponse.builder()
                    .runId(activeRun.getId())
                    .status(activeRun.getStatus())
                    .build();
        }

        AiAgentRun run = AiAgentRun.builder()
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .resumeId(request.getResumeId())
                .sceneCode(request.getSceneCode())
                .status(request.getStatus() == null ? AgentRunStatus.QUEUED.getCode() : request.getStatus())
                .currentStage(request.getCurrentStage() == null ? AgentRunStage.BOOTSTRAP.getCode() : request.getCurrentStage())
                .userInput(request.getUserInput())
                .jobDescription(request.getJobDescription())
                .targetSectionId(targetSectionId)
                .activeFlag(1)
                .clientRequestId(request.getClientRequestId())
                .build();
        aiAgentRunMapper.insert(run);

        return InternalAgentRunCreateResponse.builder()
                .runId(run.getId())
                .status(run.getStatus())
                .build();
    }

    @Override
    public InternalAgentRunDetailResponse getInternalRunDetail(Long runId) {
        AiAgentRun run = aiAgentRunMapper.selectById(runId);
        if (run == null) {
            throw BusinessException.notFound("Agent run 不存在");
        }
        return InternalAgentRunDetailResponse.builder()
                .runId(run.getId())
                .userId(run.getUserId())
                .sessionId(run.getSessionId())
                .resumeId(run.getResumeId())
                .sceneCode(run.getSceneCode())
                .status(run.getStatus())
                .currentStage(run.getCurrentStage())
                .jobDescription(run.getJobDescription())
                .resultSummary(run.getResultSummary())
                .errorMessage(run.getErrorMessage())
                .build();
    }

    @Override
    public void updateQuestionAnalysis(Long roundId, InternalInterviewQuestionAnalysisRequest request) {
        // 单体内直接读写面试轮次表，不再经过 interview 域的 HTTP 接口。
        AiInterviewRound round = aiInterviewRoundMapper.selectById(roundId);
        if (round == null) {
            throw BusinessException.notFound("面试题目不存在");
        }

        AiAgentRun run = aiAgentRunMapper.selectById(round.getRunId());
        if (run == null || !AgentSceneCode.INTERVIEW.equals(run.getSceneCode())) {
            throw BusinessException.badRequest("面试任务状态不合法");
        }

        round.setAnalysisJson(writeJson(request.getAnalysis()));
        round.setStatus(request.getStatus());
        aiInterviewRoundMapper.updateById(round);
    }

    @Override
    public InternalInterviewRoundDetailResponse getQuestionAnswer(Long roundId) {
        // 单体内直接读写面试轮次表，不再经过 interview 域的 HTTP 接口。
        AiInterviewRound round = aiInterviewRoundMapper.selectById(roundId);
        if (round == null) {
            throw BusinessException.notFound("面试题目不存在");
        }

        AiAgentRun run = aiAgentRunMapper.selectById(round.getRunId());
        if (run == null || !AgentSceneCode.INTERVIEW.equals(run.getSceneCode())) {
            throw BusinessException.badRequest("面试任务状态不合法");
        }

        List<InterviewOptionResponse> options;
        try {
            options = objectMapper.readValue(
                    round.getOptionsJson(),
                    new TypeReference<List<InterviewOptionResponse>>() {}
            );
        } catch (Exception e) {
            throw BusinessException.badRequest("面试题目选项解析失败");
        }

        return InternalInterviewRoundDetailResponse.builder()
                .roundId(round.getId())
                .runId(round.getRunId())
                .roundNo(round.getRoundNo())
                .questionText(round.getQuestionText())
                .options(options)
                .userAnswer(round.getUserAnswer())
                .analysisJson(round.getAnalysisJson())
                .status(round.getStatus())
                .build();
    }

    @Override
    public String getRunStatus(Long runId) {
        AiAgentRun run = aiAgentRunMapper.selectById(runId);
        if (run == null) {
            throw BusinessException.notFound("Agent run 不存在");
        }
        return run.getStatus();
    }

    private List<HistoryMessageResponse> listRecentMessages(Long sessionId) {
        // 先按倒序取最近 N 条，再恢复为正序给 Python，保证对话阅读顺序正确。
        return agentMessageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getSessionId, sessionId)
                        .orderByDesc(AgentMessage::getSeqNo)
                        .last("limit " + RECENT_MESSAGE_LIMIT))
                .stream()
                .sorted(Comparator.comparing(AgentMessage::getSeqNo))
                .map(message -> HistoryMessageResponse.builder()
                        .role(message.getRole())
                        .content(message.getContent())
                        .build())
                .toList();
    }

    private Map<String, Map<String, Object>> buildSchemas(List<SectionResponse> sections) {
        // Python 根据 sectionCode 找 schema，用于约束 LLM 只能产出合法 contentJson。
        return sections.stream().collect(java.util.stream.Collectors.toMap(
                // key : sectionCode
                SectionResponse::getSectionCode,
                // value: schema map
                section -> SectionSchema.getSchema(section.getSectionCode()),
                // 这个是规则 -> 就是遇到重复的key的时候, 保留左边的, 舍弃右边的, 也就是只保留第一次出现的
                (left, right) -> left
        ));
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("JSON 序列化失败");
        }
    }
}
