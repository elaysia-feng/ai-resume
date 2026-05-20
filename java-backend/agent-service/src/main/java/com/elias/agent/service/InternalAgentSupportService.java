package com.elias.agent.service;

import com.elias.common.dto.agent.internal.request.InternalBootstrapRequest;
import com.elias.common.dto.agent.internal.request.RunEventBatchRequest;
import com.elias.common.dto.agent.internal.request.RunStatusUpdateRequest;
import com.elias.common.dto.agent.internal.response.InternalBootstrapResponse;
import com.elias.common.dto.interview.internal.request.InternalInterviewBootstrapRequest;
import com.elias.common.dto.interview.internal.request.InternalInterviewQuestionAnalysisRequest;
import com.elias.common.dto.interview.internal.response.InternalInterviewBootstrapResponse;
import com.elias.common.dto.interview.internal.response.InternalInterviewRoundDetailResponse;
import jakarta.validation.Valid;

/**
 * Python Agent 内部支持接口
 * ServiceImpl 负责 bootstrap 数据组装、事件落库和状态回写
 */
public interface InternalAgentSupportService {

    /**
     * 构造 Python Agent 启动上下文
     *
     * @param request bootstrap 请求
     * @return 简历、schema、历史消息和约束
     */
    InternalBootstrapResponse bootstrap(InternalBootstrapRequest request);

    /**
     * 批量保存 Python 上报的 run 事件
     *
     * @param runId   run ID
     * @param request 事件批量请求
     */
    void saveRunEvents(Long runId, RunEventBatchRequest request);

    /**
     * 更新 run 状态
     *
     * @param runId   run ID
     * @param request 状态更新请求
     */
    void updateRunStatus(Long runId, RunStatusUpdateRequest request);

    /**
     * 构造 Python Agent 启动上下文, 但是不含修改的目标指定和约束
     *
     * @param request bootstrap 请求
     * @return 简历、schema、历史消息和约束
     */
    InternalInterviewBootstrapResponse interviewBootstrap(InternalInterviewBootstrapRequest request);

    void updateQuestionAnalysis(Long roundId, @Valid InternalInterviewQuestionAnalysisRequest request);

    InternalInterviewRoundDetailResponse getQuestionAnswer(Long roundId);

    /**
     * 获取 run 状态
     */
    String getRunStatus(Long runId);
}
