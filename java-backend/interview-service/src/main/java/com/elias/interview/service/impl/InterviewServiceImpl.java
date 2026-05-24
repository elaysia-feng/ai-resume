package com.elias.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.elias.common.AgentRunStage;
import com.elias.common.AgentRunStatus;
import com.elias.common.AgentSceneCode;
import com.elias.common.ApiResponse;
import com.elias.common.client.AgentClient;
import com.elias.common.context.UserContext;
import com.elias.common.dto.agent.internal.request.InternalAgentRunCreateRequest;
import com.elias.common.dto.agent.internal.request.RunStatusUpdateRequest;
import com.elias.common.dto.agent.internal.response.InternalAgentRunCreateResponse;
import com.elias.common.dto.agent.internal.response.InternalAgentRunDetailResponse;
import com.elias.common.dto.interview.internal.request.InternalInterviewQuestionAnalysisRequest;
import com.elias.common.dto.interview.internal.request.InternalInterviewQuestionCreateRequest;
import com.elias.common.dto.interview.internal.request.InterviewAgentRunJobMessage;
import com.elias.common.dto.interview.internal.response.InternalInterviewQuestionCreateResponse;
import com.elias.common.dto.interview.internal.response.InternalInterviewRoundDetailResponse;
import com.elias.common.dto.response.AgentSessionDetailResponse;
import com.elias.common.exception.BusinessException;
import com.elias.interview.config.InterviewAgentRabbitProperties;
import com.elias.interview.dto.request.InterviewQuestionRoundAnswerRequest;
import com.elias.interview.dto.request.InterviewQuestionRoundPageRequest;
import com.elias.interview.dto.request.StartInterviewRunRequest;
import com.elias.interview.dto.response.InterviewAnswerResponse;
import com.elias.interview.dto.response.InterviewBoardResponse;
import com.elias.interview.dto.response.InterviewOptionResponse;
import com.elias.interview.dto.response.InterviewQuestionResponse;
import com.elias.interview.dto.response.InterviewQuestionRoundPageResponse;
import com.elias.interview.dto.response.InterviewQuestionRoundResponse;
import com.elias.interview.entity.AiInterviewRound;
import com.elias.interview.mapper.AiInterviewRoundMapper;
import com.elias.interview.service.InterviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final AgentClient agentClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final InterviewAgentRabbitProperties agentRabbitProperties;
    private final AiInterviewRoundMapper aiInterviewRoundMapper;

    @Override
    public Long startRun(StartInterviewRunRequest request) {
        Long userId = UserContext.verifyGetUserId();
        AgentSessionDetailResponse session = getSession(request.getSessionId());

        ApiResponse<InternalAgentRunCreateResponse> runResponse = agentClient.createInterviewRun(
                InternalAgentRunCreateRequest.builder()
                        .userId(userId)
                        .sessionId(session.getId())
                        .resumeId(session.getResumeId())
                        .sceneCode(AgentSceneCode.INTERVIEW)
                        .status(AgentRunStatus.QUEUED.getCode())
                        .currentStage(AgentRunStage.BOOTSTRAP.getCode())
                        .jobDescription(session.getJobDescription())
                        .build());
        InternalAgentRunCreateResponse run = runResponse == null ? null : runResponse.getData();
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
                        .eq(AiInterviewRound::getUserId, userId)
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
        InternalAgentRunDetailResponse run = getRunDetail(runId);

        AiInterviewRound lastRound = aiInterviewRoundMapper.selectOne(
                new LambdaQueryWrapper<AiInterviewRound>()
                        .eq(AiInterviewRound::getRunId, runId)
                        .orderByDesc(AiInterviewRound::getRoundNo)
                        .last("limit 1"));
        int roundNo = lastRound == null ? 1 : lastRound.getRoundNo() + 1;

        AiInterviewRound round = AiInterviewRound.builder()
                .userId(run.getUserId())
                .runId(runId)
                .sessionId(run.getSessionId())
                .resumeId(run.getResumeId())
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
        if (!userId.equals(round.getUserId())) {
            throw BusinessException.notFound("没找到与当前用户匹配的面试题目");
        }
        getOwnedRun(userId, round.getRunId());
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
        agentClient.updateRunStatus(round.getRunId(), updateRequest);

        sendToMq(InterviewAgentRunJobMessage.builder()
                .jobType("CONTINUE")
                .runId(round.getRunId())
                .sessionId(round.getSessionId())
                .resumeId(round.getResumeId())
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
        Long userId = UserContext.verifyGetUserId();
        getOwnedRun(userId, runId);

        Page<AiInterviewRound> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<AiInterviewRound> roundPage = aiInterviewRoundMapper.selectPage(page,
                new LambdaQueryWrapper<AiInterviewRound>()
                        .eq(AiInterviewRound::getRunId, runId)
                        .eq(AiInterviewRound::getUserId, userId)
                        .eq(AiInterviewRound::getStatus, "ANSWERED")
                        .orderByAsc(AiInterviewRound::getRoundNo));

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
        agentClient.updateRunStatus(runId, updateRequest);
    }

    private AgentSessionDetailResponse getSession(Long sessionId) {
        ApiResponse<AgentSessionDetailResponse> response = agentClient.getSessionStatus(sessionId);
        if (response == null || response.getData() == null) {
            throw BusinessException.badRequest("无法获取会话信息");
        }
        AgentSessionDetailResponse session = response.getData();
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
        ApiResponse<InternalAgentRunDetailResponse> response = agentClient.getInternalRunDetail(runId);
        if (response == null || response.getData() == null) {
            throw BusinessException.notFound("没找到面试任务");
        }
        InternalAgentRunDetailResponse run = response.getData();
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
        Runnable publishTask = () -> {
            try {
                String routingKey = "CONTINUE".equals(message.getJobType())
                        ? agentRabbitProperties.getInterviewContinueRoutingKey()
                        : agentRabbitProperties.getInterviewStartRoutingKey();
                rabbitTemplate.convertAndSend(agentRabbitProperties.getRunExchange(), routingKey, message,
                        new CorrelationData());
            } catch (Exception e) {
                // MQ 投递失败不影响主流程。
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
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

    private List<InterviewOptionResponse> parseOptions(String optionsJson) {
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<InterviewOptionResponse>>() {
            });
        } catch (Exception e) {
            throw BusinessException.business("面试题选项解析失败");
        }
    }

    private List<com.elias.common.dto.interview.response.InterviewOptionResponse> parseCommonOptions(String optionsJson) {
        try {
            return objectMapper.readValue(optionsJson,
                    new TypeReference<List<com.elias.common.dto.interview.response.InterviewOptionResponse>>() {
                    });
        } catch (Exception e) {
            throw BusinessException.business("面试题选项解析失败");
        }
    }
}
