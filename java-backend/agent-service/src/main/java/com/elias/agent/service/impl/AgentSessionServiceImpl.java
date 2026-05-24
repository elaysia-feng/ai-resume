package com.elias.agent.service.impl;

import com.elias.common.client.ResumeClient;
import com.elias.common.ApiResponse;
import com.elias.common.dto.response.AgentSessionDetailResponse;
import com.elias.common.dto.response.ResumeDetailResponse;
import com.elias.common.context.UserContext;
import com.elias.agent.dto.run.request.AgentMessageCreateRequest;
import com.elias.agent.dto.session.request.AgentSessionCreateRequest;
import com.elias.agent.dto.session.request.AgentSessionUpdateRequest;
import com.elias.agent.entity.AgentMessage;
import com.elias.agent.entity.AgentSession;
import com.elias.agent.entity.AiAgentRun;
import com.elias.common.exception.BusinessException;
import com.elias.agent.mapper.AgentMessageMapper;
import com.elias.agent.mapper.AgentSessionMapper;
import com.elias.agent.mapper.AiAgentRunMapper;
import com.elias.agent.service.AgentSessionService;
import com.elias.common.dto.response.AgentMessageResponse;
import com.elias.agent.dto.session.response.AgentSessionItemResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent会话服务实现
 */
@Service
@RequiredArgsConstructor
public class AgentSessionServiceImpl implements AgentSessionService {

    private final AgentSessionMapper agentSessionMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final AiAgentRunMapper aiAgentRunMapper;
    private final ResumeClient resumeClient;

    /**
     * 创建Agent会话
     *
     * @param request 创建请求
     * @return 会话详情
     */
    @Override
    @Transactional
    public AgentSessionDetailResponse createSession(AgentSessionCreateRequest request) {
        Long userId = UserContext.verifyGetUserId();
        validateResumeOwnership(userId, request.getResumeId());
        AgentSession parentSession = null;
        if (request.getCopyFromSessionId() != null) {
            parentSession = getOwnedSession(request.getCopyFromSessionId());
        }

        AgentSession agentSession = AgentSession.builder()
                .userId(userId)
                .resumeId(request.getResumeId() != null ? request.getResumeId()
                        : parentSession == null ? null : parentSession.getResumeId())
                .sceneCode(normalizeSceneCode(request.getSceneCode()))
                .sessionTitle(resolveSessionTitle(request))
                .jobDescription(resolveJobDescription(request, parentSession))
                .parentSessionId(parentSession == null ? null : parentSession.getId())
                .status("ACTIVE")
                .build();
        agentSessionMapper.insert(agentSession);

        return buildSessionDetailResponse(agentSession, List.of());
    }

    /**
     * 查询当前用户会话列表
     *
     * @param resumeId  关联简历ID，可为空
     * @param sceneCode 会话场景，可为空
     * @return 会话列表
     */
    @Override
    public List<AgentSessionItemResponse> listSessions(Long resumeId, String sceneCode) {
        Long userId = UserContext.verifyGetUserId();
        validateResumeOwnership(userId, resumeId);

        LambdaQueryWrapper<AgentSession> queryWrapper = new LambdaQueryWrapper<AgentSession>()
                .eq(AgentSession::getUserId, userId)
                .ne(AgentSession::getStatus, "DELETED")
                .orderByDesc(AgentSession::getLastMessageAt, AgentSession::getUpdatedAt, AgentSession::getId);

        if (resumeId != null) {
            queryWrapper.eq(AgentSession::getResumeId, resumeId);
        }
        if (sceneCode != null && !sceneCode.isBlank()) {
            queryWrapper.eq(AgentSession::getSceneCode, normalizeSceneCode(sceneCode));
        }

        return agentSessionMapper.selectList(queryWrapper).stream()
                .map(this::buildSessionItemResponse)
                .toList();
    }

    /**
     * 查询会话详情
     *
     * @param sessionId 会话ID
     * @return 会话详情
     */
    @Override
    public AgentSessionDetailResponse getSessionDetail(Long sessionId) {
        AgentSession agentSession = getOwnedSession(sessionId);
        List<AgentMessageResponse> messages = listMessages(sessionId);
        return buildSessionDetailResponse(agentSession, messages);
    }

    /**
     * 更新会话标题或状态
     *
     * @param sessionId 会话ID
     * @param request   更新请求
     */
    @Override
    @Transactional
    public void updateSession(Long sessionId, AgentSessionUpdateRequest request) {
        AgentSession agentSession = getOwnedSession(sessionId);

        if ((request.getSessionTitle() == null || request.getSessionTitle().isBlank())
                && request.getJobDescription() == null
                && request.getSummary() == null
                && (request.getStatus() == null || request.getStatus().isBlank())) {
            throw BusinessException.badRequest("sessionTitle、jobDescription、summary 和 status 不能同时为空");
        }

        if (request.getSessionTitle() != null && !request.getSessionTitle().isBlank()) {
            agentSession.setSessionTitle(request.getSessionTitle().trim());
        }
        if (request.getJobDescription() != null) {
            agentSession.setJobDescription(
                    request.getJobDescription().isBlank() ? null : request.getJobDescription().trim());
        }
        if (request.getSummary() != null) {
            agentSession.setSummary(request.getSummary().isBlank() ? null : request.getSummary().trim());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            agentSession.setStatus(normalizeSessionStatus(request.getStatus()));
        }

        agentSessionMapper.updateById(agentSession);
    }

    /**
     * 软删除会话
     *
     * @param sessionId 会话ID
     */
    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        AgentSession agentSession = getOwnedSession(sessionId);
        agentSession.setStatus("DELETED");
        agentSessionMapper.updateById(agentSession);
    }

    /**
     * 查询会话下的消息列表
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @Override
    public List<AgentMessageResponse> listMessages(Long sessionId) {
        getOwnedSession(sessionId);
        return agentMessageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, sessionId)
                .orderByAsc(AgentMessage::getSeqNo, AgentMessage::getId))
                .stream()
                .map(this::buildMessageResponse)
                .toList();
    }

    /**
     * 新增一条消息
     *
     * @param sessionId 会话ID
     * @param request   消息请求
     * @return 保存后的消息
     */
    @Override
    @Transactional
    public AgentMessageResponse createMessage(Long sessionId, AgentMessageCreateRequest request) {
        AgentSession agentSession = getOwnedSession(sessionId);

        AgentMessage lastMessage = agentMessageMapper.selectOne(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, sessionId)
                .orderByDesc(AgentMessage::getSeqNo)
                .last("limit 1"));
        int nextSeqNo = lastMessage == null ? 1 : lastMessage.getSeqNo() + 1;

        AgentMessage agentMessage = AgentMessage.builder()
                .sessionId(sessionId)
                .role(normalizeRole(request.getRole()))
                .content(request.getContent().trim())
                .contentType(normalizeContentType(request.getContentType()))
                .seqNo(nextSeqNo)
                .status("SUCCESS")
                .build();
        agentMessageMapper.insert(agentMessage);

        agentSession.setLastMessageAt(LocalDateTime.now());
        if (agentSession.getSessionTitle() == null || agentSession.getSessionTitle().isBlank()) {
            agentSession
                    .setSessionTitle(buildDefaultSessionTitle(agentSession.getSceneCode(), agentMessage.getContent()));
        }
        agentSessionMapper.updateById(agentSession);

        return buildMessageResponse(agentMessage);
    }

    /**
     * 查询当前用户拥有的会话
     *
     * @param sessionId 会话ID
     * @return 会话实体
     */
    private AgentSession getOwnedSession(Long sessionId) {
        Long userId = UserContext.verifyGetUserId();
        AgentSession agentSession = agentSessionMapper.selectById(sessionId);
        if (agentSession == null || !userId.equals(agentSession.getUserId())) {
            throw BusinessException.notFound("Agent 会话不存在或无权限访问");
        }
        if ("DELETED".equals(agentSession.getStatus())) {
            throw BusinessException.notFound("Agent 会话不存在或已删除");
        }
        return agentSession;
    }

    /**
     * 校验简历归属
     *
     * @param userId   用户ID
     * @param resumeId 简历ID
     */
    private void validateResumeOwnership(Long userId, Long resumeId) {
        if (resumeId == null) {
            return;
        }

        ApiResponse<ResumeDetailResponse> detailedResume = resumeClient.getDetailedResume(resumeId);
        ResumeDetailResponse resume = detailedResume.getData();
        if (resume == null || !userId.equals(resume.getUserId())) {
            throw BusinessException.notFound("简历不存在或无权限访问");
        }
    }

    /**
     * 构建会话列表响应
     *
     * @param agentSession 会话实体
     * @return 列表项响应
     */
    private AgentSessionItemResponse buildSessionItemResponse(AgentSession agentSession) {
        AiAgentRun activeRun = aiAgentRunMapper.selectOne(new LambdaQueryWrapper<AiAgentRun>()
                .eq(AiAgentRun::getSessionId, agentSession.getId())
                .eq(AiAgentRun::getSceneCode, agentSession.getSceneCode())
                .eq(AiAgentRun::getActiveFlag, 1)
                .orderByDesc(AiAgentRun::getUpdatedAt, AiAgentRun::getId)
                .last("limit 1"));
        return AgentSessionItemResponse.builder()
                .id(agentSession.getId())
                .resumeId(agentSession.getResumeId())
                .sceneCode(agentSession.getSceneCode())
                .sessionTitle(agentSession.getSessionTitle())
                .jobDescription(agentSession.getJobDescription())
                .parentSessionId(agentSession.getParentSessionId())
                .status(agentSession.getStatus())
                .activeRunId(activeRun == null ? null : activeRun.getId())
                .activeRunStatus(activeRun == null ? null : activeRun.getStatus())
                .lastMessageAt(agentSession.getLastMessageAt())
                .createdAt(agentSession.getCreatedAt())
                .updatedAt(agentSession.getUpdatedAt())
                .build();
    }

    /**
     * 构建会话详情响应
     *
     * @param agentSession 会话实体
     * @param messages     消息列表
     * @return 会话详情响应
     */
    private AgentSessionDetailResponse buildSessionDetailResponse(AgentSession agentSession,
            List<AgentMessageResponse> messages) {
        return AgentSessionDetailResponse.builder()
                .id(agentSession.getId())
                .resumeId(agentSession.getResumeId())
                .sceneCode(agentSession.getSceneCode())
                .sessionTitle(agentSession.getSessionTitle())
                .jobDescription(agentSession.getJobDescription())
                .summary(agentSession.getSummary())
                .parentSessionId(agentSession.getParentSessionId())
                .status(agentSession.getStatus())
                .lastMessageAt(agentSession.getLastMessageAt())
                .createdAt(agentSession.getCreatedAt())
                .updatedAt(agentSession.getUpdatedAt())
                .messages(messages)
                .build();
    }

    /**
     * 构建消息响应
     *
     * @param agentMessage 消息实体
     * @return 消息响应
     */
    private AgentMessageResponse buildMessageResponse(AgentMessage agentMessage) {
        return AgentMessageResponse.builder()
                .id(agentMessage.getId())
                .sessionId(agentMessage.getSessionId())
                .role(agentMessage.getRole())
                .content(agentMessage.getContent())
                .contentType(agentMessage.getContentType())
                .seqNo(agentMessage.getSeqNo())
                .status(agentMessage.getStatus())
                .toolName(agentMessage.getToolName())
                .extraJson(agentMessage.getExtraJson())
                .createdAt(agentMessage.getCreatedAt())
                .build();
    }

    /**
     * 规范化会话场景
     *
     * @param sceneCode 场景值
     * @return 标准化后的场景值
     */
    private String normalizeSceneCode(String sceneCode) {
        return sceneCode == null ? "CHAT" : sceneCode.trim().toUpperCase();
    }

    /**
     * 规范化会话状态
     *
     * @param status 状态值
     * @return 标准化后的状态值
     */
    private String normalizeSessionStatus(String status) {
        String normalizedStatus = status.trim().toUpperCase();
        List<String> supportedStatus = List.of("ACTIVE", "ARCHIVED", "DELETED");
        if (!supportedStatus.contains(normalizedStatus)) {
            throw BusinessException.badRequest("status 仅支持 ACTIVE / ARCHIVED / DELETED");
        }
        return normalizedStatus;
    }

    /**
     * 规范化消息角色
     *
     * @param role 角色值
     * @return 标准化后的角色值
     */
    private String normalizeRole(String role) {
        String normalizedRole = role == null ? "USER" : role.trim().toUpperCase();
        List<String> supportedRole = List.of("USER", "ASSISTANT", "SYSTEM", "TOOL");
        if (!supportedRole.contains(normalizedRole)) {
            throw BusinessException.badRequest("role 仅支持 USER / ASSISTANT / SYSTEM / TOOL");
        }
        return normalizedRole;
    }

    /**
     * 规范化内容类型
     *
     * @param contentType 内容类型
     * @return 标准化后的内容类型
     */
    private String normalizeContentType(String contentType) {
        String normalizedContentType = contentType == null ? "TEXT" : contentType.trim().toUpperCase();
        List<String> supportedContentType = List.of("TEXT", "JSON");
        if (!supportedContentType.contains(normalizedContentType)) {
            throw BusinessException.badRequest("contentType 仅支持 TEXT / JSON");
        }
        return normalizedContentType;
    }

    /**
     * 生成会话标题
     *
     * @param request 创建请求
     * @return 会话标题
     */
    private String resolveSessionTitle(AgentSessionCreateRequest request) {
        if (request.getSessionTitle() != null && !request.getSessionTitle().isBlank()) {
            return request.getSessionTitle().trim();
        }
        return buildDefaultSessionTitle(normalizeSceneCode(request.getSceneCode()), null);
    }

    /**
     * 解析新会话的JD
     *
     * @param request       创建请求
     * @param parentSession 来源会话
     * @return 会话JD
     */
    private String resolveJobDescription(AgentSessionCreateRequest request, AgentSession parentSession) {
        if (request.getJobDescription() != null && !request.getJobDescription().isBlank()) {
            return request.getJobDescription().trim();
        }
        if (Boolean.TRUE.equals(request.getCopyJobDescription()) && parentSession != null) {
            return parentSession.getJobDescription();
        }
        return null;
    }

    /**
     * 根据场景和消息内容生成默认会话标题
     *
     * @param sceneCode 会话场景
     * @param content   首条消息内容
     * @return 默认标题
     */
    private String buildDefaultSessionTitle(String sceneCode, String content) {
        if (content != null && !content.isBlank()) {
            String normalizedContent = content.trim().replaceAll("\\s+", " ");
            return normalizedContent.length() > 20 ? normalizedContent.substring(0, 20) : normalizedContent;
        }

        return switch (sceneCode) {
            case "OPTIMIZE" -> "优化对话";
            case "MATCH" -> "匹配对话";
            case "SUMMARY" -> "摘要对话";
            default -> "新建对话";
        };
    }
}
