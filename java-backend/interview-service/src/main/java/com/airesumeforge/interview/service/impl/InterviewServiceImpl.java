package com.airesumeforge.interview.service.impl;

import com.airesumeforge.common.AgentRunStage;
import com.airesumeforge.common.AgentRunStatus;
import com.airesumeforge.context.UserContext;
import com.airesumeforge.common.dto.interview.internal.request.InterviewAgentRunJobMessage;
import com.airesumeforge.interview.dto.request.CreateInterviewSessionRequest;
import com.airesumeforge.interview.dto.request.StartInterviewRunRequest;
import com.airesumeforge.interview.dto.request.InterviewQuestionRoundAnswerRequest;
import com.airesumeforge.interview.dto.request.InterviewQuestionRoundPageRequest;
import com.airesumeforge.interview.dto.response.InterviewBoardResponse;
import com.airesumeforge.interview.dto.response.InterviewAnswerResponse;
import com.airesumeforge.interview.dto.response.InterviewOptionResponse;
import com.airesumeforge.interview.dto.response.InterviewQuestionResponse;
import com.airesumeforge.interview.dto.response.InterviewQuestionRoundResponse;
import com.airesumeforge.interview.dto.response.InterviewQuestionRoundPageResponse;
import com.airesumeforge.interview.dto.internal.request.InternalInterviewQuestionCreateRequest;
import com.airesumeforge.interview.dto.internal.response.InternalInterviewQuestionCreateResponse;
import com.airesumeforge.entity.AgentSession;
import com.airesumeforge.entity.AiAgentRun;
import com.airesumeforge.interview.entity.AiInterviewRound;
import com.airesumeforge.exception.BusinessException;
import com.airesumeforge.mapper.AgentSessionMapper;
import com.airesumeforge.mapper.AiAgentRunMapper;
import com.airesumeforge.interview.mapper.AiInterviewRoundMapper;
import com.airesumeforge.service.AgentRunJobProducer;
import com.airesumeforge.interview.service.InterviewService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.airesumeforge.common.AgentSceneCode;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {
    private final AiInterviewRoundMapper aiInterviewRoundMapper;
    private final AgentSessionMapper agentSessionMapper;
    private final AiAgentRunMapper aiAgentRunMapper;
    private final AgentRunJobProducer agentRunJobProducer;
    private final ObjectMapper objectMapper;



    @Override
    public Long createInterviewSession(CreateInterviewSessionRequest request) {
        Long userId = UserContext.verifyGetUserId();

        AgentSession session = AgentSession.builder()
                .userId(userId)
                .resumeId(request.getResumeId())
                .jobDescription(request.getJobDescription())
                .sceneCode(request.getSceneCode())
                .status("ACTIVE")
                .build();

        agentSessionMapper.insert(session);
        return session.getId();
    }

    @Override
    @Transactional
    public Long startRun(StartInterviewRunRequest request) {
        Long userId = UserContext.verifyGetUserId();

        AgentSession session = agentSessionMapper.selectById(request.getSessionId());
        if (session == null || !userId.equals(session.getUserId())) {
            throw BusinessException.notFound("面试会话不存在");
        }

        // startRun 里也固定场景校验
        if (!AgentSceneCode.INTERVIEW.equals(session.getSceneCode())) {
            throw BusinessException.badRequest("当前会话不是面试模拟场景");
        }

        AiAgentRun run = AiAgentRun.builder()
                .userId(userId)
                .sessionId(session.getId())
                .resumeId(session.getResumeId())
                .sceneCode(session.getSceneCode())
                .status(AgentRunStatus.QUEUED.getCode())
                .currentStage(AgentRunStage.BOOTSTRAP.getCode())
                .jobDescription(session.getJobDescription())
                .build();

        aiAgentRunMapper.insert(run);

        // 投递给 Python worker / MQ
        sendToMq(InterviewAgentRunJobMessage
                .builder()
                .jobType("START")
                .runId(run.getId())
                .sessionId(session.getId())
                .sceneCode(AgentSceneCode.INTERVIEW)
                .jobDescription(run.getJobDescription())
                .resumeId(run.getResumeId())
                .build());
        return run.getId();
    }

    @Override
    public InterviewBoardResponse questionBoard(Long runId) {
        Long userId = UserContext.verifyGetUserId();

        AiAgentRun run = aiAgentRunMapper.selectById(runId);
        if (run == null || !userId.equals(run.getUserId())) {
            throw BusinessException.notFound("没找到与当前用户匹配的面试任务");
        }

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
                .sessionId(run.getSessionId())
                .status(run.getStatus())
                .currentQuestion(currentQuestion)
                .summary(run.getResultSummary())
                .errorMessage(run.getErrorMessage())
                .build();
    }

    @Override
    public InternalInterviewQuestionCreateResponse createQuestionRound(Long runId,
                                                                       InternalInterviewQuestionCreateRequest request)
            throws JsonProcessingException {
        Long userId = UserContext.verifyGetUserId();

        AiAgentRun run = aiAgentRunMapper.selectById(runId);
        if (run == null || !userId.equals(run.getUserId())) {
            throw BusinessException.notFound("没找到与当前用户匹配的面试任务");
        }

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
                .sessionId(run.getSessionId())
                .resumeId(run.getResumeId())
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
    @Transactional
    public InterviewAnswerResponse submitAnswer(Long roundId, InterviewQuestionRoundAnswerRequest request) {
        Long userId = UserContext.verifyGetUserId();

        AiInterviewRound round = aiInterviewRoundMapper.selectById(roundId);
        if (round == null || !userId.equals(round.getUserId())) {
            throw BusinessException.notFound("没找到与当前用户匹配的面试任务");
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
                .eq(AiInterviewRound::getUserId, userId)
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
        if (round == null || !round.getUserId().equals(UserContext.verifyGetUserId())) {
            throw BusinessException.notFound("没找到与当前用户匹配的面试任务");
        }
        round.setStatus(AgentRunStatus.SUCCESS.getCode());

        aiInterviewRoundMapper.updateById(round);
    }


    private void sendToMq(InterviewAgentRunJobMessage message) {
        Runnable publishTask = () -> {
            try{
                agentRunJobProducer.publish(message);
            }
            catch (Exception e){
                markFailed(message.getRunId(), "Agent run 入队失败: " + e.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 事务提交之后再传递mq, 保证 python段的消费到信息时run可以查询
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

    private void markFailed(Long runId, String message) {
        // 这个方法用于 MQ 投递失败等 Java 本地失败场景。
        // Python 执行失败会通过 InternalAgentSupportServiceImpl.updateRunStatus 回写。
        AiAgentRun run = aiAgentRunMapper.selectById(runId);
        if (run != null) {
            run.setStatus(AgentRunStatus.FAILED.getCode());
            run.setErrorMessage(message);
            run.setCompletedAt(LocalDateTime.now());
            aiAgentRunMapper.updateById(run);
        }
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
