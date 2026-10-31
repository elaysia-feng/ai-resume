package com.elias.service.interview.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.elias.service.agent.AgentSessionService;
import com.elias.service.agent.InternalAgentSupportService;
import com.elias.common.AgentRunStage;
import com.elias.common.AgentRunStatus;
import com.elias.common.AgentSceneCode;
import com.elias.common.context.UserContext;
import com.elias.dto.common.agent.internal.request.InternalAgentRunCreateRequest;
import com.elias.dto.common.agent.internal.request.RunStatusUpdateRequest;
import com.elias.dto.common.agent.internal.response.InternalAgentRunCreateResponse;
import com.elias.dto.common.agent.internal.response.InternalAgentRunDetailResponse;
import com.elias.dto.common.interview.internal.request.InternalInterviewQuestionAnalysisRequest;
import com.elias.dto.common.interview.internal.request.InternalInterviewQuestionCreateRequest;
import com.elias.dto.common.interview.internal.request.InterviewAgentRunJobMessage;
import com.elias.dto.common.interview.internal.response.InternalInterviewQuestionCreateResponse;
import com.elias.dto.common.interview.internal.response.InternalInterviewRoundDetailResponse;
import com.elias.dto.common.response.AgentSessionDetailResponse;
import com.elias.exception.BusinessException;
import com.elias.common.stream.AgentStreamClient;
import com.elias.common.stream.AgentStreamProperties;
import com.elias.dto.interview.request.InterviewQuestionRoundAnswerRequest;
import com.elias.dto.interview.request.InterviewQuestionRoundPageRequest;
import com.elias.dto.interview.request.StartInterviewRunRequest;
import com.elias.dto.interview.response.InterviewAnswerResponse;
import com.elias.dto.interview.response.InterviewBoardResponse;
import com.elias.dto.interview.response.InterviewOptionResponse;
import com.elias.dto.interview.response.InterviewQuestionResponse;
import com.elias.dto.interview.response.InterviewQuestionRoundPageResponse;
import com.elias.dto.interview.response.InterviewQuestionRoundResponse;
import com.elias.entity.interview.AiInterviewRound;
import com.elias.mapper.interview.AiInterviewRoundMapper;
import com.elias.service.interview.InterviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InternalAgentSupportService internalAgentSupportService;
    private final AgentSessionService agentSessionService;
    private final AgentStreamClient agentStreamClient;
    private final ObjectMapper objectMapper;
    private final AgentStreamProperties agentStreamProperties;
    private final AiInterviewRoundMapper aiInterviewRoundMapper;

    @Override
    @Transactional
    public Long startRun(StartInterviewRunRequest request) {
        Long userId = UserContext.verifyGetUserId();
        AgentSessionDetailResponse session = getSession(request.getSessionId());

        InternalAgentRunCreateResponse run = internalAgentSupportService.createInterviewRun(
                InternalAgentRunCreateRequest.builder()
                        .userId(userId)
                        .sessionId(session.getId())
                        .resumeId(session.getResumeId())
                        .sceneCode(AgentSceneCode.INTERVIEW)
                        .status(AgentRunStatus.QUEUED.getCode())
                        .currentStage(AgentRunStage.BOOTSTRAP.getCode())
                        .jobDescription(session.getJobDescription())
                        .build());
        if (run == null || run.getRunId() == null) {
            throw BusinessException.business("创建面试任务失败");
        }

        if (AgentRunStatus.QUEUED.getCode().equals(run.getStatus())) {
            sendToMq(InterviewAgentRunJobMessage.builder()
                    .jobType("START")
                    .runId(run.getRunId())
                    .sessionId(session.getId())
                    .resumeId(session.getResumeId())
                    .sceneCode(AgentSceneCode.INTERVIEW)
                    .jobDescription(session.getJobDescription())
                    .build());
        }
        return run.getRunId();
    }

    @Override
    public InterviewBoardResponse questionBoard(Long runId) {
        Long userId = UserContext.verifyGetUserId();
        InternalAgentRunDetailResponse run = getOwnedRun(userId, runId);

        AiInterviewRound currentRound = aiInterviewRoundMapper.selectOne(
                new LambdaQueryWrapper<AiInterviewRound>()
                        .eq(AiInterviewRound::getRunId, runId)
                        .eq(AiInterviewRound::getStatus, "WAITING_ANSWER")
                        .orderByDesc(AiInterviewRound::getRoundNo)
                        .last("limit 1"));

        InterviewQuestionResponse currentQuestion = null;
        if (currentRound != null) {
            currentQuestion = InterviewQuestionResponse.builder()
                    .roundId(currentRound.getId())
                    .roundNo(currentRound.getRoundNo())
                    .questionText(currentRound.getQuestionText())
                    .options(parseOptions(currentRound.getOptionsJson()))
                    .userAnswer(currentRound.getUserAnswer())
                    .status(currentRound.getStatus())
                    .build();
        }

        return InterviewBoardResponse.builder()
                .runId(runId)
                .sessionId(run.getSessionId())
                .status(run.getStatus())
                .currentQuestion(currentQuestion)
                .summary(run.getResultSummary())
                .errorMessage(run.getErrorMessage())
                .build();
    }

    @Override
    public InternalInterviewQuestionCreateResponse createQuestionRound(
            Long runId,
            InternalInterviewQuestionCreateRequest request) throws JsonProcessingException {
        getRunDetail(runId);

        AiInterviewRound lastRound = aiInterviewRoundMapper.selectOne(
                new LambdaQueryWrapper<AiInterviewRound>()
                        .eq(AiInterviewRound::getRunId, runId)
                        .orderByDesc(AiInterviewRound::getRoundNo)
                        .last("limit 1"));
        int roundNo = lastRound == null ? 1 : lastRound.getRoundNo() + 1;

        AiInterviewRound round = AiInterviewRound.builder()
                .runId(runId)
                .roundNo(roundNo)
                .questionText(request.getQuestionText())
                .optionsJson(objectMapper.writeValueAsString(request.getOptions()))
                .status("WAITING_ANSWER")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        aiInterviewRoundMapper.insert(round);

        return InternalInterviewQuestionCreateResponse.builder()
                .roundId(round.getId())
                .roundNo(roundNo)
                .build();
    }

    @Override
    @Transactional
    public void updateQuestionAnalysis(Long roundId, InternalInterviewQuestionAnalysisRequest request)
            throws JsonProcessingException {
        AiInterviewRound round = getRound(roundId);
        getRunDetail(round.getRunId());

        round.setAnalysisJson(objectMapper.writeValueAsString(request.getAnalysis()));
        if (request.getStatus() != null) {
            round.setStatus(request.getStatus());
        }
        round.setUpdatedAt(LocalDateTime.now());
        aiInterviewRoundMapper.updateById(round);
    }

    @Override
    public InternalInterviewRoundDetailResponse getQuestionAnswer(Long roundId) {
        AiInterviewRound round = getRound(roundId);
        getRunDetail(round.getRunId());

        return InternalInterviewRoundDetailResponse.builder()
                .roundId(round.getId())
                .runId(round.getRunId())
                .roundNo(round.getRoundNo())
                .questionText(round.getQuestionText())
                .options(parseCommonOptions(round.getOptionsJson()))
                .userAnswer(round.getUserAnswer())
                .analysisJson(round.getAnalysisJson())
                .status(round.getStatus())
                .build();
    }

    @Override
    @Transactional
    public InterviewAnswerResponse submitAnswer(Long roundId, InterviewQuestionRoundAnswerRequest request) {
        Long userId = UserContext.verifyGetUserId();
        AiInterviewRound round = getRound(roundId);
        InternalAgentRunDetailResponse run = getRunDetail(round.getRunId());
        if (!userId.equals(run.getUserId())) {
            throw BusinessException.notFound("没找到与当前用户匹配的面试题目");
        }
        if (!"WAITING_ANSWER".equals(round.getStatus())) {
            throw BusinessException.badRequest("当前题目不是待回答状态");
        }

        round.setStatus("ANSWERED");
        round.setUserAnswer(request.getUserAnswer());
        round.setUpdatedAt(LocalDateTime.now());
        aiInterviewRoundMapper.updateById(round);

        RunStatusUpdateRequest updateRequest = new RunStatusUpdateRequest();
        updateRequest.setStatus(AgentRunStatus.QUEUED.getCode());
        updateRequest.setCurrentStage(AgentRunStage.ANSWER.getCode());
        internalAgentSupportService.updateRunStatus(round.getRunId(), updateRequest);

        sendToMq(InterviewAgentRunJobMessage.builder()
                .jobType("CONTINUE")
                .runId(round.getRunId())
                .sessionId(run.getSessionId())
                .resumeId(run.getResumeId())
                .sceneCode(AgentSceneCode.INTERVIEW)
                .build());

        return InterviewAnswerResponse.builder()
                .roundId(roundId)
                .status("ANSWERED")
                .build();
    }

    @Override
    public InterviewQuestionRoundPageResponse pageQuestionRounds(
            Long runId,
            InterviewQuestionRoundPageRequest request) {
        // 1. 先校验 run 所属用户，避免跨用户读取面试历史。
        Long userId = UserContext.verifyGetUserId();
        getOwnedRun(userId, runId);

        // 2. 复合索引覆盖分页过滤和 round_no 升序；uk_run_round 继续负责轮次唯一性。
        Page<AiInterviewRound> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<AiInterviewRound> roundPage = aiInterviewRoundMapper.selectPage(page,
                new LambdaQueryWrapper<AiInterviewRound>()
                        .select(
                                AiInterviewRound::getId,
                                AiInterviewRound::getRoundNo,
                                AiInterviewRound::getQuestionText,
                                AiInterviewRound::getOptionsJson,
                                AiInterviewRound::getUserAnswer,
                                AiInterviewRound::getAnalysisJson,
                                AiInterviewRound::getStatus)
                        .eq(AiInterviewRound::getRunId, runId)
                        .eq(AiInterviewRound::getStatus, "ANSWERED")
                        .orderByAsc(AiInterviewRound::getRoundNo));

        // 3. 转换本页记录并保留 MyBatis-Plus 统计出的总数。
        List<InterviewQuestionRoundResponse> records = roundPage.getRecords().stream()
                .map(round -> InterviewQuestionRoundResponse.builder()
                        .roundId(round.getId())
                        .roundNo(round.getRoundNo())
                        .questionText(round.getQuestionText())
                        .options(parseOptions(round.getOptionsJson()))
                        .userAnswer(round.getUserAnswer())
                        .analysis(round.getAnalysisJson())
                        .status(round.getStatus())
                        .build())
                .toList();

        return InterviewQuestionRoundPageResponse.builder()
                .records(records)
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .total(roundPage.getTotal())
                .build();
    }

    @Override
    public void finishRun(Long runId) {
        Long userId = UserContext.verifyGetUserId();
        getOwnedRun(userId, runId);

        RunStatusUpdateRequest updateRequest = new RunStatusUpdateRequest();
        updateRequest.setStatus(AgentRunStatus.SUCCESS.getCode());
        internalAgentSupportService.updateRunStatus(runId, updateRequest);
    }

    private AgentSessionDetailResponse getSession(Long sessionId) {
        AgentSessionDetailResponse session = agentSessionService.getSessionDetail(sessionId);
        if (session == null) {
            throw BusinessException.badRequest("无法获取会话信息");
        }
        if (!AgentSceneCode.INTERVIEW.equals(session.getSceneCode())) {
            throw BusinessException.badRequest("当前会话不是面试场景");
        }
        if (!"ACTIVE".equals(session.getStatus())) {
            throw BusinessException.badRequest("当前会话不可用");
        }
        if (session.getResumeId() == null) {
            throw BusinessException.notFound("当前会话没有关联简历");
        }
        return session;
    }

    private InternalAgentRunDetailResponse getOwnedRun(Long userId, Long runId) {
        InternalAgentRunDetailResponse run = getRunDetail(runId);
        if (!userId.equals(run.getUserId())) {
            throw BusinessException.notFound("没找到与当前用户匹配的面试任务");
        }
        return run;
    }

    private InternalAgentRunDetailResponse getRunDetail(Long runId) {
        InternalAgentRunDetailResponse run = internalAgentSupportService.getInternalRunDetail(runId);
        if (run == null) {
            throw BusinessException.notFound("没找到面试任务");
        }
        if (!AgentSceneCode.INTERVIEW.equals(run.getSceneCode())) {
            throw BusinessException.badRequest("当前任务不是面试场景");
        }
        return run;
    }

    private AiInterviewRound getRound(Long roundId) {
        AiInterviewRound round = aiInterviewRoundMapper.selectById(roundId);
        if (round == null) {
            throw BusinessException.notFound("面试题目不存在");
        }
        return round;
    }

    private void sendToMq(InterviewAgentRunJobMessage message) {
        // run 状态、面试轮次和 agent_task 共用 SQLite 事务，入队失败时整体回滚。
        agentStreamClient.publish(agentStreamProperties.getInterviewRunStream(), message);
    }

    private List<InterviewOptionResponse> parseOptions(String optionsJson) {
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<InterviewOptionResponse>>() {
            });
        } catch (Exception e) {
            throw BusinessException.business("面试题选项解析失败");
        }
    }

    private List<com.elias.dto.common.interview.response.InterviewOptionResponse> parseCommonOptions(String optionsJson) {
        try {
            return objectMapper.readValue(optionsJson,
                    new TypeReference<List<com.elias.dto.common.interview.response.InterviewOptionResponse>>() {
                    });
        } catch (Exception e) {
            throw BusinessException.business("面试题选项解析失败");
        }
    }
}
