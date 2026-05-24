package com.elias.agent.controller;

import com.elias.common.ApiResponse;
import com.elias.common.dto.response.AgentSessionDetailResponse;
import com.elias.agent.dto.run.request.AgentMessageCreateRequest;
import com.elias.agent.dto.session.request.AgentSessionCreateRequest;
import com.elias.agent.dto.session.request.AgentSessionUpdateRequest;
import com.elias.agent.service.AgentSessionService;
import com.elias.common.dto.response.AgentMessageResponse;
import com.elias.agent.dto.session.response.AgentSessionItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent会话控制器
 * 负责会话创建、查询、更新，以及消息的保存和查询
 */
@RestController
@RequestMapping("/api/agent/sessions")
@RequiredArgsConstructor
public class AgentSessionController {

    private final AgentSessionService agentSessionService;

    /**
     * 创建Agent会话
     *
     * @param request 创建请求
     * @return 新建的会话详情
     */
    @PostMapping
    public ApiResponse<AgentSessionDetailResponse> createSession(
            @Valid @RequestBody AgentSessionCreateRequest request) {
        return ApiResponse.ok(agentSessionService.createSession(request));
    }

    /**
     * 查询当前用户会话列表
     *
     * @param resumeId  关联简历ID，可为空
     * @param sceneCode 会话场景，可为空
     * @return 会话列表
     */
    @GetMapping
    public ApiResponse<List<AgentSessionItemResponse>> listSessions(@RequestParam(required = false) Long resumeId,
            @RequestParam(required = false) String sceneCode) {
        return ApiResponse.ok(agentSessionService.listSessions(resumeId, sceneCode));
    }

    /**
     * 查询会话详情
     *
     * @param sessionId 会话ID
     * @return 会话详情
     */
    @GetMapping("/{sessionId}")
    public ApiResponse<AgentSessionDetailResponse> getSessionDetail(@PathVariable Long sessionId) {
        return ApiResponse.ok(agentSessionService.getSessionDetail(sessionId));
    }

    /**
     * 更新会话标题或状态
     *
     * @param sessionId 会话ID
     * @param request   更新请求
     * @return 空响应
     */
    @PutMapping("/{sessionId}")
    public ApiResponse<Void> updateSession(@PathVariable Long sessionId,
            @Valid @RequestBody AgentSessionUpdateRequest request) {
        agentSessionService.updateSession(sessionId, request);
        return ApiResponse.ok();
    }

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @return 空响应
     */
    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable Long sessionId) {
        agentSessionService.deleteSession(sessionId);
        return ApiResponse.ok();
    }

    /**
     * 查询会话下的消息列表
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @GetMapping("/{sessionId}/messages")
    public ApiResponse<List<AgentMessageResponse>> listMessages(@PathVariable Long sessionId) {
        return ApiResponse.ok(agentSessionService.listMessages(sessionId));
    }

    /**
     * 新增一条会话消息
     *
     * @param sessionId 会话ID
     * @param request   消息请求
     * @return 保存后的消息
     */
    @PostMapping("/{sessionId}/messages")
    public ApiResponse<AgentMessageResponse> createMessage(@PathVariable Long sessionId,
            @Valid @RequestBody AgentMessageCreateRequest request) {
        return ApiResponse.ok(agentSessionService.createMessage(sessionId, request));
    }
}
