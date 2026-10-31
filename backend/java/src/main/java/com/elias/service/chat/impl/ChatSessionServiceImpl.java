package com.elias.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.elias.entity.agent.AgentMessage;
import com.elias.entity.agent.AgentSession;
import com.elias.mapper.agent.AgentMessageMapper;
import com.elias.mapper.agent.AgentSessionMapper;
import com.elias.dto.chat.request.ChatMessageRequest;
import com.elias.dto.chat.request.ChatSessionCreateRequest;
import com.elias.dto.chat.response.ChatMessageResponse;
import com.elias.dto.chat.response.ChatSessionCreateResponse;
import com.elias.dto.chat.response.ChatSessionItemResponse;
import com.elias.config.agent.PythonAgentProperties;
import com.elias.config.common.InternalAgentProperties;
import com.elias.service.chat.ChatSessionService;
import com.elias.service.chat.WorkspaceService;
import com.elias.common.context.UserContext;
import com.elias.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话会话服务实现。
 *
 * <p>Java 负责会话权限、消息落库和 SSE 代理；模型调用、提示词、Agent 编排和工具执行由 Python 负责。</p>
 *
 * <p>整个过程通过 SSE 推给前端：text_delta / tool_call / tool_result / done / error。</p>
 */
@Slf4j
@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    /**
     * 会话场景
     */
    private static final String SCENE_CODE = "CHAT";

    /**
     * 会话状态
     */
    private static final String SESSION_STATUS_ACTIVE = "ACTIVE";

    /**
     * 会话已删除状态
     */
    private static final String SESSION_STATUS_DELETED = "DELETED";

    /**
     * 消息角色
     */
    private static final String ROLE_USER = "USER";
    private static final String ROLE_ASSISTANT = "ASSISTANT";
    private static final String ROLE_TOOL = "TOOL";

    /**
     * 消息内容类型
     */
    private static final String CONTENT_TYPE_TEXT = "TEXT";
    private static final String CONTENT_TYPE_JSON = "JSON";

    /**
     * 消息状态
     */
    private static final String MESSAGE_STATUS_SUCCESS = "SUCCESS";
    private static final String MESSAGE_STATUS_FAILED = "FAILED";

    /**
     * 默认会话标题
     */
    private static final String DEFAULT_SESSION_TITLE = "新建对话";

    private final AgentSessionMapper agentSessionMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final WorkspaceService workspaceService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final ThreadPoolTaskExecutor agentStreamTaskExecutor;
    private final RestClient pythonAgentClient;
    private final PythonAgentProperties pythonAgentProperties;
    private final InternalAgentProperties internalAgentProperties;

    /**
     * 注入会话持久化、工作区解析和 Python SSE 代理依赖。
     *
     * @param agentSessionMapper 会话 Mapper
     * @param agentMessageMapper 消息 Mapper
     * @param workspaceService 当前用户工作区解析服务
     * @param objectMapper JSON 序列化器
     * @param transactionTemplate 消息持久化事务模板
     * @param agentStreamTaskExecutor 聊天 SSE 线程池
     * @param restClientBuilder HTTP 客户端构造器
     * @param pythonAgentProperties Python 服务地址配置
     * @param internalAgentProperties Java-Python 内部令牌配置
     */
    public ChatSessionServiceImpl(AgentSessionMapper agentSessionMapper,
            AgentMessageMapper agentMessageMapper,
            WorkspaceService workspaceService,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            ThreadPoolTaskExecutor agentStreamTaskExecutor,
            RestClient.Builder restClientBuilder,
            PythonAgentProperties pythonAgentProperties,
            InternalAgentProperties internalAgentProperties) {
        this.agentSessionMapper = agentSessionMapper;
        this.agentMessageMapper = agentMessageMapper;
        this.workspaceService = workspaceService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.agentStreamTaskExecutor = agentStreamTaskExecutor;
        this.pythonAgentProperties = pythonAgentProperties;
        this.internalAgentProperties = internalAgentProperties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(pythonAgentProperties.getReadTimeoutSeconds()));
        this.pythonAgentClient = restClientBuilder.baseUrl(pythonAgentProperties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 创建当前用户的聊天会话。
     *
     * @param request 会话标题和可选简历 ID
     * @return 新建会话 ID
     */
    @Override
    @Transactional
    public ChatSessionCreateResponse createSession(ChatSessionCreateRequest request) {
        Long userId = UserContext.verifyGetUserId();
        String title = request == null ? null : request.getTitle();
        AgentSession session = AgentSession.builder()
                .userId(userId)
                .resumeId(request == null ? null : request.getResumeId())
                .sceneCode(SCENE_CODE)
                .sessionTitle(title == null || title.isBlank() ? DEFAULT_SESSION_TITLE : title.trim())
                .status(SESSION_STATUS_ACTIVE)
                .build();
        agentSessionMapper.insert(session);
        return ChatSessionCreateResponse.builder().sessionId(session.getId()).build();
    }

    /**
     * 查询当前用户的聊天会话，并批量汇总每个会话的消息数。
     *
     * @return 按最近活动排序的聊天会话列表
     */
    @Override
    public List<ChatSessionItemResponse> listSessions() {
        Long userId = UserContext.verifyGetUserId();
        // 1. 按当前用户和 CHAT 场景读取会话，并保持最近活动顺序。
        List<AgentSession> sessions = agentSessionMapper.selectList(new LambdaQueryWrapper<AgentSession>()
                .eq(AgentSession::getUserId, userId)
                .eq(AgentSession::getSceneCode, SCENE_CODE)
                .ne(AgentSession::getStatus, SESSION_STATUS_DELETED)
                .orderByDesc(AgentSession::getLastMessageAt, AgentSession::getUpdatedAt, AgentSession::getId));
        if (sessions.isEmpty()) {
            return List.of();
        }

        // 2. 收集本次列表的会话 ID，使用一次分组查询统计消息数。
        List<Long> sessionIds = sessions.stream().map(AgentSession::getId).toList();
        List<Map<String, Object>> countRows = agentMessageMapper.selectMaps(
                new QueryWrapper<AgentMessage>()
                        .select("session_id", "COUNT(*) AS message_count")
                        .in("session_id", sessionIds)
                        .groupBy("session_id"));
        Map<Long, Long> messageCounts = new HashMap<>();
        for (Map<String, Object> countRow : countRows) {
            Long sessionId = ((Number) countRow.get("session_id")).longValue();
            Long messageCount = ((Number) countRow.get("message_count")).longValue();
            messageCounts.put(sessionId, messageCount);
        }

        // 3. 将会话与消息数合并为响应；没有消息的会话计数为 0。
        return sessions.stream()
                .map(session -> toSessionItem(session, messageCounts.getOrDefault(session.getId(), 0L)))
                .toList();
    }

    /**
     * 查询指定会话的消息历史。
     *
     * @param sessionId 会话 ID
     * @return 按顺序排列的消息列表
     */
    @Override
    public List<ChatMessageResponse> listMessages(Long sessionId) {
        Long userId = UserContext.verifyGetUserId();
        getOwnedSession(userId, sessionId);
        return agentMessageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getSessionId, sessionId)
                        .orderByAsc(AgentMessage::getSeqNo, AgentMessage::getId))
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    /**
     * 删除当前用户拥有的聊天会话及其消息。
     *
     * @param sessionId 会话 ID
     */
    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        Long userId = UserContext.verifyGetUserId();
        AgentSession session = getOwnedSession(userId, sessionId);
        // ai_message 上有 ON DELETE CASCADE，删会话会连带删消息
        agentSessionMapper.deleteById(session.getId());
    }

    /**
     * 保存用户消息并异步代理到 Python ChatModel 流。
     *
     * @param sessionId 会话 ID
     * @param request 用户消息与工作区路径
     * @return 聊天 SSE 事件流
     */
    @Override
    public SseEmitter sendMessage(Long sessionId, ChatMessageRequest request) {
        // 1. 校验会话归属与消息内容，并解析当前用户有权访问的工作区。
        Long userId = UserContext.verifyGetUserId();
        // 归属校验放在请求线程里，越权直接 404，不会走到 SSE 里
        getOwnedSession(userId, sessionId);

        String content = request == null ? null : request.getContent();
        if (content == null || content.isBlank()) {
            throw BusinessException.badRequest("消息内容不能为空");
        }
        String workspaceRoot = workspaceService.resolveWorkspaceRoot(userId,
                request == null ? null : request.getWorkspacePath());

        // 2. 先持久化用户消息，再构造发送给 Python 的完整历史。
        saveUserMessage(sessionId, content.trim());
        List<Map<String, Object>> messages = buildChatHistory(sessionId);

        // 3. 在线程池里代理 Python SSE，并把错误转换成前端事件。
        SseEmitter emitter = createEmitter();
        try {
            agentStreamTaskExecutor.execute(() -> {
                try {
                    proxyPythonChat(sessionId, workspaceRoot, messages, emitter);
                    emitter.complete();
                } catch (Exception e) {
                    log.warn("对话执行失败, sessionId={}", sessionId, e);
                    sendEvent(emitter, "error", Map.<String, Object>of("message", errorMessage()));
                    emitter.complete();
                }
            });
        } catch (TaskRejectedException e) {
            log.warn("对话线程池已满, sessionId={}", sessionId);
            sendEvent(emitter, "error", Map.<String, Object>of("message", "系统繁忙，请稍后重试"));
            emitter.complete();
        }
        return emitter;
    }

    /**
     * 创建 SSE emitter。
     *
     * <p>单独抽成方法，方便单测替换成能捕获事件的实现。</p>
     */
    SseEmitter createEmitter() {
        return new SseEmitter(pythonAgentProperties.getReadTimeoutSeconds() * 1000L);
    }

    /**
     * 通过 Python 内部接口代理模型 SSE，并让 Java 继续作为消息持久化的唯一入口。
     *
     * @param sessionId 会话 ID
     * @param workspaceRoot 当前用户选择的工作区根目录
     * @param messages 已保存的会话消息
     * @param emitter 返回给前端的 SSE emitter
     */
    private void proxyPythonChat(Long sessionId, String workspaceRoot,
            List<Map<String, Object>> messages, SseEmitter emitter) {
        // 1. 传递工作区和历史消息，并使用内部令牌调用 Python。
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("workspaceRoot", workspaceRoot);
        body.put("messages", messages);

        pythonAgentClient.post()
                .uri("/internal/chat/stream")
                .header("X-Internal-Service-Token", internalAgentProperties.getServiceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(body)
                .exchange((request, response) -> {
                    // 2. 校验内部响应，再交由 SSE 解析器逐事件持久化和转发。
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException("Python AI 服务返回 HTTP " + response.getStatusCode().value());
                    }
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        readPythonEvents(sessionId, reader, emitter);
                    }
                    return null;
                });
    }

    /**
     * 解析 Python 传来的 SSE，并在 Java 边界保存模型消息与工具结果。
     *
     * @param sessionId 会话 ID
     * @param reader Python SSE 响应体
     * @param emitter 返回给前端的 SSE emitter
     * @throws IOException SSE 响应体读取失败时抛出
     */
    private void readPythonEvents(Long sessionId, BufferedReader reader, SseEmitter emitter) throws IOException {
        // 1. 按 SSE 空行边界收集完整事件，再转交事件分发器。
        String eventName = "";
        StringBuilder data = new StringBuilder();
        StringBuilder assistantText = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                dispatchPythonEvent(sessionId, eventName, data.toString(), assistantText, emitter);
                eventName = "";
                data.setLength(0);
            } else if (line.startsWith("event:")) {
                eventName = line.substring("event:".length()).trim();
            } else if (line.startsWith("data:")) {
                if (data.length() > 0) {
                    data.append('\n');
                }
                data.append(line.substring("data:".length()).stripLeading());
            }
        }
        // 2. EOF 前没有空行的末尾事件也要分发。
        dispatchPythonEvent(sessionId, eventName, data.toString(), assistantText, emitter);
    }

    /**
     * 按 SSE 事件类型执行对应的消息落库或前端转发。
     *
     * @param sessionId 会话 ID
     * @param eventName Python 事件名称
     * @param rawData Python 事件 JSON
     * @param assistantText 当前轮助手文本缓存
     * @param emitter 返回给前端的 SSE emitter
     * @throws JsonProcessingException 事件 JSON 无法解析时抛出
     */
    private void dispatchPythonEvent(Long sessionId, String eventName, String rawData,
            StringBuilder assistantText, SseEmitter emitter) throws JsonProcessingException {
        if (eventName == null || eventName.isBlank() || rawData == null || rawData.isBlank()) {
            return;
        }
        // 1. 解析事件负载，再按事件类型执行持久化或前端转发。
        Map<String, Object> payload = objectMapper.readValue(rawData, new TypeReference<>() {
        });
        // 2. 文本和工具调用先按前端协议转发；完整工具参数同时写入消息历史。
        if ("text_delta".equals(eventName)) {
            String text = String.valueOf(payload.getOrDefault("text", ""));
            assistantText.append(text);
            sendEvent(emitter, eventName, Map.of("text", text));
        } else if ("tool_call".equals(eventName)) {
            sendEvent(emitter, eventName, payload);
        } else if ("tool_calls_complete".equals(eventName)) {
            String historyContent = String.valueOf(payload.getOrDefault("historyContent", ""));
            if (!historyContent.isBlank()) {
                saveAssistantToolCallMessage(sessionId, historyContent);
            }
            assistantText.setLength(0);
        // 3. 工具结果、最终回答和错误分别更新会话或通知前端。
        } else if ("tool_result".equals(eventName)) {
            saveToolMessage(sessionId, payload);
            Map<String, Object> resultEvent = new LinkedHashMap<>();
            resultEvent.put("id", payload.getOrDefault("id", ""));
            resultEvent.put("name", payload.getOrDefault("name", ""));
            resultEvent.put("ok", Boolean.TRUE.equals(payload.get("ok")));
            resultEvent.put("summary", payload.getOrDefault("summary", ""));
            sendEvent(emitter, eventName, resultEvent);
        } else if ("done".equals(eventName)) {
            Long messageId = saveAssistantMessage(sessionId, assistantText.toString());
            Map<String, Object> doneEvent = new LinkedHashMap<>();
            doneEvent.put("messageId", messageId == null ? 0L : messageId);
            doneEvent.put("finishReason", payload.getOrDefault("finishReason", "stop"));
            sendEvent(emitter, eventName, doneEvent);
        } else if ("error".equals(eventName)) {
            Map<String, Object> errorEvent = new LinkedHashMap<>();
            errorEvent.put("message", payload.getOrDefault("message", "AI 处理失败，请检查模型配置后重试"));
            sendEvent(emitter, eventName, errorEvent);
        }
    }

    /**
     * 保留会话历史的通用字段；Python 负责把旧工具块还原为 ChatModel 消息。
     *
     * @param sessionId 会话 ID
     * @return 供 Python 运行时转换的有序消息列表
     */
    private List<Map<String, Object>> buildChatHistory(Long sessionId) {
        List<Map<String, Object>> messages = new ArrayList<>();
        List<AgentMessage> history = agentMessageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                .select(AgentMessage::getRole, AgentMessage::getContent, AgentMessage::getContentType,
                        AgentMessage::getToolName)
                .eq(AgentMessage::getSessionId, sessionId)
                .orderByAsc(AgentMessage::getSeqNo, AgentMessage::getId));
        for (AgentMessage message : history) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", message.getRole());
            item.put("content", message.getContent() == null ? "" : message.getContent());
            item.put("contentType", message.getContentType() == null ? CONTENT_TYPE_TEXT : message.getContentType());
            item.put("toolName", message.getToolName());
            messages.add(item);
        }
        return messages;
    }
    /**
     * 保存用户消息。
     *
     * @param sessionId 会话 ID
     * @param content 用户输入文本
     */
    private void saveUserMessage(Long sessionId, String content) {
        transactionTemplate.executeWithoutResult(status -> {
            agentMessageMapper.insert(AgentMessage.builder()
                    .sessionId(sessionId)
                    .role(ROLE_USER)
                    .content(content)
                    .contentType(CONTENT_TYPE_TEXT)
                    .seqNo(nextSeqNo(sessionId))
                    .status(MESSAGE_STATUS_SUCCESS)
                    .build());
            touchSession(sessionId);
        });
    }

    /**
     * 保存助手文本消息。
     *
     * @param sessionId 会话 ID
     * @param text 助手文本
     * @return 新消息 ID
     */
    private Long saveAssistantMessage(Long sessionId, String text) {
        return transactionTemplate.execute(status -> {
            AgentMessage message = AgentMessage.builder()
                    .sessionId(sessionId)
                    .role(ROLE_ASSISTANT)
                    .content(text == null ? "" : text)
                    .contentType(CONTENT_TYPE_TEXT)
                    .seqNo(nextSeqNo(sessionId))
                    .status(MESSAGE_STATUS_SUCCESS)
                    .build();
            agentMessageMapper.insert(message);
            touchSession(sessionId);
            return message.getId();
        });
    }

    /**
     * 保存 Python 序列化的工具调用历史。
     *
     * @param sessionId 会话 ID
     * @param historyContent Python 生成的通用消息 JSON
     */
    private void saveAssistantToolCallMessage(Long sessionId, String historyContent) {
        transactionTemplate.executeWithoutResult(status -> {
            agentMessageMapper.insert(AgentMessage.builder()
                    .sessionId(sessionId)
                    .role(ROLE_ASSISTANT)
                    .content(historyContent)
                    .contentType(CONTENT_TYPE_JSON)
                    .seqNo(nextSeqNo(sessionId))
                    .status(MESSAGE_STATUS_SUCCESS)
                    .build());
            touchSession(sessionId);
        });
    }

    /**
     * 保存 Python 序列化的工具结果；前端只接收简短摘要。
     *
     * @param sessionId 会话 ID
     * @param payload Python 工具结果事件
     */
    private void saveToolMessage(Long sessionId, Map<String, Object> payload) {
        // 1. 原样保留 Python 生成的通用历史内容，只在 Java 持久化状态和工具名。
        String toolName = String.valueOf(payload.getOrDefault("name", ""));
        boolean ok = Boolean.TRUE.equals(payload.get("ok"));
        String historyContent = String.valueOf(payload.getOrDefault("historyContent", ""));
        transactionTemplate.executeWithoutResult(status -> {
            agentMessageMapper.insert(AgentMessage.builder()
                    .sessionId(sessionId)
                    .role(ROLE_TOOL)
                    .content(historyContent)
                    .contentType(CONTENT_TYPE_JSON)
                    .toolName(toolName)
                    .seqNo(nextSeqNo(sessionId))
                    .status(ok ? MESSAGE_STATUS_SUCCESS : MESSAGE_STATUS_FAILED)
                    .build());
            touchSession(sessionId);
        });
    }
    /**
     * 读取会话内下一个消息顺序号。
     *
     * @param sessionId 会话 ID
     * @return 下一个顺序号
     */
    private int nextSeqNo(Long sessionId) {
        AgentMessage last = agentMessageMapper.selectOne(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, sessionId)
                .orderByDesc(AgentMessage::getSeqNo)
                .last("limit 1"));
        if (last == null || last.getSeqNo() == null) {
            return 1;
        }
        return last.getSeqNo() + 1;
    }

    /**
     * 更新会话的最后消息时间。
     *
     * @param sessionId 会话 ID
     */
    private void touchSession(Long sessionId) {
        AgentSession session = agentSessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }
        session.setLastMessageAt(LocalDateTime.now());
        agentSessionMapper.updateById(session);
    }

    /**
     * 校验会话归属，不是自己的会话一律 404。
     *
     * @param userId 当前用户 ID
     * @param sessionId 会话 ID
     * @return 当前用户拥有的会话
     */
    private AgentSession getOwnedSession(Long userId, Long sessionId) {
        AgentSession session = agentSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw BusinessException.notFound("会话不存在或无权限访问");
        }
        if (SESSION_STATUS_DELETED.equals(session.getStatus())) {
            throw BusinessException.notFound("会话不存在或已删除");
        }
        return session;
    }

    /**
     * 推送一个 SSE 事件。
     *
     * <p>数据先自己序列化成 JSON 字符串，避免依赖 SSE 内部的消息转换器。</p>
     *
     * @param emitter 返回给前端的 SSE emitter
     * @param eventName 事件名称
     * @param payload 事件数据
     */
    private void sendEvent(SseEmitter emitter, String eventName, Map<String, Object> payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(writeJson(payload), MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.warn("SSE 推送失败, event={}", eventName, e);
        }
    }

    /**
     * 提供不包含底层凭据或异常细节的前端提示。
     *
     * @return 安全的用户提示
     */
    private String errorMessage() {
        return "AI 服务调用失败，请检查 Python 进程、模型端点与协议配置后重试";
    }

    /**
     * 将值序列化成 JSON。
     *
     * @param value 待序列化内容
     * @return JSON 字符串
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw BusinessException.business("内容序列化失败");
        }
    }

    /**
     * 构造带消息数的聊天会话列表项。
     *
     * @param session      会话记录
     * @param messageCount 会话消息数量
     * @return 聊天会话列表项
     */
    private ChatSessionItemResponse toSessionItem(AgentSession session, Long messageCount) {
        return ChatSessionItemResponse.builder()
                .id(session.getId())
                .title(session.getSessionTitle())
                .lastMessageAt(session.getLastMessageAt())
                .messageCount(messageCount)
                .build();
    }

    /**
     * 将消息实体映射为聊天接口响应。
     *
     * @param message 消息实体
     * @return 聊天消息响应
     */
    private ChatMessageResponse toMessageResponse(AgentMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .contentType(message.getContentType())
                .toolName(message.getToolName())
                .seqNo(message.getSeqNo())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
