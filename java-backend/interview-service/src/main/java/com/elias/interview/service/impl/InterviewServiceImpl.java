package com.elias.interview.service.impl;

import com.elias.common.client.AgentClient;
import com.elias.common.AgentRunStatus;
import com.elias.common.AgentSceneCode;
import com.elias.common.ApiResponse;
import com.elias.common.dto.interview.internal.request.InterviewAgentRunJobMessage;
import com.elias.common.context.UserContext;
import com.elias.interview.dto.request.CreateInterviewSessionRequest;
import com.elias.interview.dto.request.StartInterviewRunRequest;
import com.elias.interview.dto.request.InterviewQuestionRoundAnswerRequest;
import com.elias.interview.dto.request.InterviewQuestionRoundPageRequest;
import com.elias.interview.dto.response.InterviewBoardResponse;
import com.elias.interview.dto.response.InterviewAnswerResponse;
import com.elias.interview.dto.response.InterviewOptionResponse;
import com.elias.interview.dto.response.InterviewQuestionResponse;
import com.elias.interview.dto.response.InterviewQuestionRoundResponse;
import com.elias.interview.dto.response.InterviewQuestionRoundPageResponse;
import com.elias.interview.dto.internal.request.InternalInterviewQuestionCreateRequest;
import com.elias.interview.dto.internal.response.InternalInterviewQuestionCreateResponse;
import com.elias.interview.entity.AiInterviewRound;
import com.elias.common.exception.BusinessException;
import com.elias.interview.config.InterviewAgentRabbitProperties;
import com.elias.interview.mapper.AiInterviewRoundMapper;
import com.elias.interview.service.InterviewService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {
    private final AiInterviewRoundMapper aiInterviewRoundMapper;
    private final AgentClient agentClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final InterviewAgentRabbitProperties agentRabbitProperties;

    @Override
    public Long createInterviewSession(CreateInterviewSessionRequest request) {
        // 面试会话创建暂不通过 agent-service，通过 interview-service 自己处理
        throw BusinessException.badRequest("请使用 interview-service 的内部接口创建会话");
    }

    @Override
    public Long startRun(StartInterviewRunRequest request) {
        Long userId = UserContext.verifyGetUserId();

        // 通过 AgentClient 获取会话信息
        ApiResponse<String> sessionResponse = agentClient.getRunStatus(request.getSessionId());
        // TODO: 需要先有获取会话信息的接口，现在先用 runId 查询状态

        // 投递给 Python worker / MQ
        sendToMq(InterviewAgentRunJobMessage
                .builder()
                .jobType("START")
                .runId(request.getSessionId())
                .sessionId(request.getSessionId())
                .sceneCode(AgentSceneCode.INTERVIEW)
                .jobDescription(null)
                .resumeId(null)
                .build());
        return request.getSessionId();
    }

    @Override
    public InterviewBoardResponse questionBoard(Long runId) {
        Long userId = UserContext.verifyGetUserId();

        // 通过 AgentClient 查询 run 状态
        String status = agentClient.getRunStatus(runId).getData();

        AiInterviewRound currentRound = aiInterviewRoundMapper.selectOne(
                new LambdaQueryWrapper<AiInterviewRound>()
                        .eq(AiInterviewRound::getRunId, runId)
                        .eq(AiInterviewRound::getStatus, "WAITING_ANSWER")
                        .orderByDesc(AiInterviewRound::getRoundNo)
                        .last("limit 1")
        );

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
                .sessionId(null)
                .status(status)
                .currentQuestion(currentQuestion)
                .summary(null)
                .errorMessage(null)
                .build();
    }

    @Override
    public InternalInterviewQuestionCreateResponse createQuestionRound(Long runId,
                                                                       InternalInterviewQuestionCreateRequest request)
            throws JsonProcessingException {
        Long userId = UserContext.verifyGetUserId();

        String optionsJson = objectMapper.writeValueAsString(request.getOptions());

        AiInterviewRound lastRound = aiInterviewRoundMapper.selectOne(
                new LambdaQueryWrapper<AiInterviewRound>()
                        .eq(AiInterviewRound::getRunId, runId)
                        .orderByDesc(AiInterviewRound::getRoundNo)
                        .last("limit 1")
        );

        int roundNo = lastRound == null ? 1 : lastRound.getRoundNo() + 1;

        AiInterviewRound round = AiInterviewRound.builder()
                .runId(runId)
                .sessionId(null)
                .resumeId(null)
                .roundNo(roundNo)
                .questionText(request.getQuestionText())
                .optionsJson(optionsJson)
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
    public InterviewAnswerResponse submitAnswer(Long roundId, InterviewQuestionRoundAnswerRequest request) {
        Long userId = UserContext.verifyGetUserId();

        AiInterviewRound round = aiInterviewRoundMapper.selectById(roundId);
        if (round == null) {
            throw BusinessException.notFound("没找到与当前用户匹配的面试题目");
        }

        round.setStatus("ANSWERED");
        round.setUserAnswer(request.getUserAnswer());
        aiInterviewRoundMapper.updateById(round);

        // 通知 Python 恢复图执行(就是去执行continue_run)
        sendToMq(InterviewAgentRunJobMessage
                .builder()
                .jobType("CONTINUE")
                .runId(round.getRunId())
                .sessionId(round.getSessionId())
                .sceneCode(AgentSceneCode.INTERVIEW)
                .build());

        return InterviewAnswerResponse.builder()
                .roundId(roundId)
                .status("ANSWERED")
                .build();
    }

    @Override
    public InterviewQuestionRoundPageResponse pageQuestionRounds(Long runId, InterviewQuestionRoundPageRequest request) {
        Long userId = UserContext.verifyGetUserId();

        LambdaQueryWrapper<AiInterviewRound> wrapper = new LambdaQueryWrapper<AiInterviewRound>()
                .eq(AiInterviewRound::getRunId, runId)
                .eq(AiInterviewRound::getStatus, "ANSWERED")
                .orderByAsc(AiInterviewRound::getRoundNo);

        Page<AiInterviewRound> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<AiInterviewRound> aiInterviewRoundPage = aiInterviewRoundMapper.selectPage(page, wrapper);

        List<InterviewQuestionRoundResponse> records = aiInterviewRoundPage.getRecords().stream()
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
                .total(aiInterviewRoundPage.getTotal())
                .build();
    }

    @Override
    public void finishRun(Long runId) {
        AiInterviewRound round = aiInterviewRoundMapper.selectById(runId);
        if (round == null) {
            throw BusinessException.notFound("没找到与当前用户匹配的面试任务");
        }
        round.setStatus(AgentRunStatus.SUCCESS.getCode());

        aiInterviewRoundMapper.updateById(round);
    }


    private void sendToMq(InterviewAgentRunJobMessage message) {
        Runnable publishTask = () -> {
            try{
                String routingKey = "CONTINUE".equals(message.getJobType())
                        ? agentRabbitProperties.getContinueRoutingKey()
                        : agentRabbitProperties.getStartRoutingKey();
                rabbitTemplate.convertAndSend(agentRabbitProperties.getRunExchange(), routingKey, message, new CorrelationData());
            }
            catch (Exception e){
                // MQ 投递失败不影响主流程
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

}
