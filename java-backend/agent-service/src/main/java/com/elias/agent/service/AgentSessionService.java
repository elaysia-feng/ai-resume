package com.elias.agent.service;

import com.elias.agent.dto.run.request.AgentMessageCreateRequest;
import com.elias.agent.dto.session.request.AgentSessionCreateRequest;
import com.elias.agent.dto.session.request.AgentSessionUpdateRequest;
import com.elias.common.dto.response.AgentMessageResponse;
import com.elias.agent.dto.session.response.AgentSessionItemResponse;
import com.elias.common.dto.response.AgentSessionDetailResponse;

import java.util.List;

/**
 * Agent会话服务接口
 */
public interface AgentSessionService {

    /**
     * 创建Agent会话
     */
    AgentSessionDetailResponse createSession(AgentSessionCreateRequest request);

    /**
     * 查询当前用户会话列表
     */
    List<AgentSessionItemResponse> listSessions(Long resumeId, String sceneCode);

    /**
     * 查询会话详情
     */
    AgentSessionDetailResponse getSessionDetail(Long sessionId);

    /**
     * 更新会话标题或状态
     */
    void updateSession(Long sessionId, AgentSessionUpdateRequest request);

    /**
     * 软删除会话
     */
    void deleteSession(Long sessionId);

    /**
     * 查询会话下的消息列表
     */
    List<AgentMessageResponse> listMessages(Long sessionId);

    /**
     * 新增一条消息
     */
    AgentMessageResponse createMessage(Long sessionId, AgentMessageCreateRequest request);
}
