package com.elias.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.elias.agent.entity.AgentMessage;
import com.elias.agent.entity.AgentSession;
import com.elias.agent.mapper.AgentMessageMapper;
import com.elias.agent.mapper.AgentSessionMapper;
import com.elias.chat.dto.request.ChatMessageRequest;
import com.elias.chat.dto.request.ChatSessionCreateRequest;
import com.elias.chat.dto.response.ChatMessageResponse;
import com.elias.chat.dto.response.ChatSessionCreateResponse;
import com.elias.chat.dto.response.ChatSessionItemResponse;
import com.elias.chat.service.ChatService;
import com.elias.chat.service.WorkspaceService;
import com.elias.chat.tool.ChatToolExecutor;
import com.elias.chat.tool.ToolRegistry;
import com.elias.chat.tool.ToolResult;
import com.elias.common.context.UserContext;
import com.elias.common.exception.BusinessException;
import com.elias.common.minimax.MiniMaxClient;
import com.elias.common.minimax.MiniMaxContentBlock;
import com.elias.common.minimax.MiniMaxMessage;
import com.elias.common.minimax.MiniMaxStreamListener;
import com.elias.common.minimax.MiniMaxTool;
import com.elias.common.minimax.MiniMaxToolUse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话服务实现。
 *
 * <p>核心是对话循环：用户消息落库 -> 组装历史 -> 调 MiniMax ->
 * 模型要调工具就执行工具、把结果回填后再次调模型，直到没有工具调用。</p>
 *
 * <p>整个过程通过 SSE 推给前端：text_delta / tool_call / tool_result / done / error。</p>
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

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

    /**
     * 对话循环最大轮次，防止模型无限调工具。
     *
     * <p>实测在十万级文件的仓库里，模型定位一个类平均要 8~10 轮工具调用
     * （搜 → 列目录 → 读 → 再搜），8 轮会经常在找到答案前就中止。</p>
     */
    private static final int MAX_TOOL_ROUNDS = 12;

    /**
     * 单次 SSE 连接最长保留时间
     */
    private static final long SSE_TIMEOUT = 300_000L;

    private final AgentSessionMapper agentSessionMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final MiniMaxClient miniMaxClient;
    private final ToolRegistry toolRegistry;
    private final ChatToolExecutor chatToolExecutor;
    private final WorkspaceService workspaceService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final ThreadPoolTaskExecutor agentStreamTaskExecutor;

    public ChatServiceImpl(AgentSessionMapper agentSessionMapper,
            AgentMessageMapper agentMessageMapper,
            MiniMaxClient miniMaxClient,
            ToolRegistry toolRegistry,
            ChatToolExecutor chatToolExecutor,
            WorkspaceService workspaceService,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            ThreadPoolTaskExecutor agentStreamTaskExecutor) {
        this.agentSessionMapper = agentSessionMapper;
        this.agentMessageMapper = agentMessageMapper;
        this.miniMaxClient = miniMaxClient;
        this.toolRegistry = toolRegistry;
        this.chatToolExecutor = chatToolExecutor;
        this.workspaceService = workspaceService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.agentStreamTaskExecutor = agentStreamTaskExecutor;
    }

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

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        Long userId = UserContext.verifyGetUserId();
        AgentSession session = getOwnedSession(userId, sessionId);
        // ai_message 上有 ON DELETE CASCADE，删会话会连带删消息
        agentSessionMapper.deleteById(session.getId());
    }

    @Override
    public SseEmitter sendMessage(Long sessionId, ChatMessageRequest request) {
        Long userId = UserContext.verifyGetUserId();
        // 归属校验放在请求线程里，越权直接 404，不会走到 SSE 里
        getOwnedSession(userId, sessionId);

        String content = request == null ? null : request.getContent();
        if (content == null || content.isBlank()) {
            throw BusinessException.badRequest("消息内容不能为空");
        }
        String workspaceRoot = workspaceService.resolveWorkspaceRoot(userId,
                request == null ? null : request.getWorkspacePath());

        saveUserMessage(sessionId, content.trim());
        List<MiniMaxMessage> messages = buildModelMessages(sessionId);

        SseEmitter emitter = createEmitter();
        try {
            agentStreamTaskExecutor.execute(() -> {
                try {
                    runConversation(userId, sessionId, workspaceRoot, messages, emitter);
                    emitter.complete();
                } catch (Exception e) {
                    log.warn("对话执行失败, sessionId={}", sessionId, e);
                    sendEvent(emitter, "error", Map.<String, Object>of("message", errorMessage(e)));
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
        return new SseEmitter(SSE_TIMEOUT);
    }

    /**
     * 对话循环：模型要调工具就执行并回填，最多 MAX_TOOL_ROUNDS 轮。
     */
    private void runConversation(Long userId, Long sessionId, String workspaceRoot,
            List<MiniMaxMessage> messages, SseEmitter emitter) {
        List<MiniMaxTool> tools = toolRegistry.toAnthropicTools();
        String systemPrompt = buildSystemPrompt(workspaceRoot);

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            List<MiniMaxContentBlock> blocks = new ArrayList<>();
            StringBuilder text = new StringBuilder();
            String[] stopReasonHolder = new String[1];

            miniMaxClient.chatStream(messages, tools, systemPrompt, new MiniMaxStreamListener() {
                @Override
                public void onTextDelta(String delta) {
                    text.append(delta);
                    sendEvent(emitter, "text_delta", Map.<String, Object>of("text", delta));
                }

                @Override
                public void onToolUse(MiniMaxToolUse toolUse) {
                    blocks.add(MiniMaxContentBlock.ofToolUse(toolUse.getId(), toolUse.getName(),
                            toolUse.getInput()));
                    Map<String, Object> input = toolUse.getInput() == null
                            ? Map.of()
                            : toolUse.getInput();
                    sendEvent(emitter, "tool_call", Map.<String, Object>of(
                            "id", toolUse.getId(),
                            "name", toolUse.getName(),
                            "input", input));
                }

                @Override
                public void onStopReason(String stopReason) {
                    stopReasonHolder[0] = stopReason;
                }
            });

            List<MiniMaxToolUse> toolUses = blocks.stream()
                    .filter(MiniMaxContentBlock::isToolUse)
                    .map(block -> new MiniMaxToolUse(block.getId(), block.getName(), block.getInput()))
                    .toList();

            if (toolUses.isEmpty()) {
                Long messageId = saveAssistantMessage(sessionId, text.toString());
                sendEvent(emitter, "done", Map.<String, Object>of(
                        "messageId", messageId == null ? 0L : messageId,
                        "finishReason", stopReasonHolder[0] == null ? "end_turn" : stopReasonHolder[0]));
                return;
            }

            // 模型这一轮要调工具：先把工具调用本身作为助手消息落库，再回填工具结果
            saveAssistantToolCallMessage(sessionId, blocks);
            List<Map<String, Object>> resultBlocks = new ArrayList<>();
            for (MiniMaxToolUse toolUse : toolUses) {
                ToolResult result = chatToolExecutor.execute(toolUse.getName(), toolUse.getInput(),
                        userId, workspaceRoot);
                sendEvent(emitter, "tool_result", Map.<String, Object>of(
                        "id", toolUse.getId(),
                        "name", toolUse.getName(),
                        "ok", result.isOk(),
                        "summary", result.getSummary() == null ? "" : result.getSummary()));
                saveToolMessage(sessionId, toolUse, result);
                resultBlocks.add(toToolResultBlock(toolUse.getId(), result));
            }
            messages.add(new MiniMaxMessage("assistant", blocks));
            messages.add(new MiniMaxMessage("user", resultBlocks));
        }

        throw new BusinessException(400, "工具调用轮次超过上限（" + MAX_TOOL_ROUNDS + " 轮），本轮对话已中止");
    }

    /**
     * 组装发给模型的消息：保留完整会话历史，只读取协议转换需要的字段。
     */
    private List<MiniMaxMessage> buildModelMessages(Long sessionId) {
        List<MiniMaxMessage> messages = new ArrayList<>();
        List<AgentMessage> history = agentMessageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                .select(AgentMessage::getRole, AgentMessage::getContent, AgentMessage::getContentType)
                .eq(AgentMessage::getSessionId, sessionId)
                .orderByAsc(AgentMessage::getSeqNo, AgentMessage::getId));
        for (AgentMessage message : history) {
            MiniMaxMessage converted = toModelMessage(message);
            if (converted != null) {
                messages.add(converted);
            }
        }
        return messages;
    }

    /**
     * 把库里的消息还原成模型协议的消息
     */
    private MiniMaxMessage toModelMessage(AgentMessage message) {
        if (ROLE_USER.equals(message.getRole())) {
            return new MiniMaxMessage("user", message.getContent());
        }
        if (ROLE_ASSISTANT.equals(message.getRole())) {
            if (CONTENT_TYPE_JSON.equals(message.getContentType())) {
                return new MiniMaxMessage("assistant", readContentBlocks(message.getContent()));
            }
            return new MiniMaxMessage("assistant", message.getContent());
        }
        if (ROLE_TOOL.equals(message.getRole())) {
            // Anthropic 兼容协议里 tool_result 走 user 角色
            return new MiniMaxMessage("user", readToolResultBlocks(message.getContent()));
        }
        return null;
    }

    /**
     * 保存用户消息
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
     * 保存助手文本消息，返回消息ID
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
     * 保存助手的工具调用消息，内容是协议层的内容块数组
     */
    private void saveAssistantToolCallMessage(Long sessionId, List<MiniMaxContentBlock> blocks) {
        transactionTemplate.executeWithoutResult(status -> {
            agentMessageMapper.insert(AgentMessage.builder()
                    .sessionId(sessionId)
                    .role(ROLE_ASSISTANT)
                    .content(writeJson(blocks))
                    .contentType(CONTENT_TYPE_JSON)
                    .seqNo(nextSeqNo(sessionId))
                    .status(MESSAGE_STATUS_SUCCESS)
                    .build());
            touchSession(sessionId);
        });
    }

    /**
     * 保存工具执行结果
     */
    private void saveToolMessage(Long sessionId, MiniMaxToolUse toolUse, ToolResult result) {
        transactionTemplate.executeWithoutResult(status -> {
            agentMessageMapper.insert(AgentMessage.builder()
                    .sessionId(sessionId)
                    .role(ROLE_TOOL)
                    .content(writeJson(List.of(toToolResultBlock(toolUse.getId(), result))))
                    .contentType(CONTENT_TYPE_JSON)
                    .toolName(toolUse.getName())
                    .seqNo(nextSeqNo(sessionId))
                    .status(result.isOk() ? MESSAGE_STATUS_SUCCESS : MESSAGE_STATUS_FAILED)
                    .build());
            touchSession(sessionId);
        });
    }

    /**
     * 构造 Anthropic 协议的 tool_result 块
     */
    private Map<String, Object> toToolResultBlock(String toolUseId, ToolResult result) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("type", "tool_result");
        block.put("tool_use_id", toolUseId);
        block.put("content", result.getContent() == null ? "" : result.getContent());
        block.put("is_error", !result.isOk());
        return block;
    }

    /**
     * 会话内下一个顺序号
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
     * 更新会话的最后消息时间
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
     * 校验会话归属，不是自己的会话一律 404
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
     * 系统提示词
     */
    private String buildSystemPrompt(String workspaceRoot) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是 AI Resume Forge 的桌面助手，运行在用户本机，可以帮用户阅读和分析工作区里的文件。\n");
        if (workspaceRoot == null || workspaceRoot.isBlank()) {
            prompt.append("用户还没有选择工作区目录，文件类工具会返回「尚未选择工作区目录」，"
                    + "此时直接引导用户先在桌面端选择工作区目录。\n");
        } else {
            prompt.append("工作区根目录：").append(workspaceRoot).append("\n");
            prompt.append("所有文件工具只能访问该目录内的路径，越界会被拒绝。\n");
        }
        prompt.append("需要文件内容时优先调用工具读取，不要凭空猜测。");
        return prompt.toString();
    }

    /**
     * 推送一个 SSE 事件。
     *
     * <p>数据先自己序列化成 JSON 字符串，避免依赖 SSE 内部的消息转换器。</p>
     */
    private void sendEvent(SseEmitter emitter, String eventName, Map<String, Object> payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(writeJson(payload), MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.warn("SSE 推送失败, event={}", eventName, e);
        }
    }

    /**
     * 异常信息兜底，保证推给前端的消息不为空
     */
    private String errorMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? "对话执行失败，请稍后重试" : message;
    }

    /**
     * 解析助手消息里的内容块数组
     */
    private List<MiniMaxContentBlock> readContentBlocks(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<MiniMaxContentBlock>>() {
            });
        } catch (JsonProcessingException e) {
            throw BusinessException.business("会话历史解析失败");
        }
    }

    /**
     * 解析工具结果块数组
     */
    private List<Map<String, Object>> readToolResultBlocks(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (JsonProcessingException e) {
            throw BusinessException.business("工具结果解析失败");
        }
    }

    /**
     * 序列化成 JSON
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
     * 消息响应
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
