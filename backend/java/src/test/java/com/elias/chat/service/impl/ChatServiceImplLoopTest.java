package com.elias.chat.service.impl;

import com.elias.agent.entity.AgentMessage;
import com.elias.agent.entity.AgentSession;
import com.elias.agent.mapper.AgentMessageMapper;
import com.elias.agent.mapper.AgentSessionMapper;
import com.elias.chat.tool.ChatToolExecutor;
import com.elias.chat.tool.ToolRegistry;
import com.elias.chat.tool.ToolResult;
import com.elias.chat.service.WorkspaceService;
import com.elias.common.context.UserContext;
import com.elias.common.exception.BusinessException;
import com.elias.common.minimax.MiniMaxClient;
import com.elias.common.minimax.MiniMaxContentBlock;
import com.elias.common.minimax.MiniMaxMessage;
import com.elias.common.minimax.MiniMaxStreamListener;
import com.elias.common.minimax.MiniMaxTool;
import com.elias.common.minimax.MiniMaxToolUse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 对话循环与会话归属。
 *
 * <p>核心链路：模型返回 tool_use -> 执行工具 -> 结果回填 -> 再次调用模型 -> 输出最终回答。</p>
 */
class ChatServiceImplLoopTest {

    private static final Long SESSION_ID = 9L;
    private static final Long USER_ID = 1L;

    private AgentSessionMapper sessionMapper;
    private AgentMessageMapper messageMapper;
    private MiniMaxClient miniMaxClient;
    private ToolRegistry toolRegistry;
    private ChatToolExecutor toolExecutor;
    private WorkspaceService workspaceService;

    /**
     * 模拟落库的 ai_message
     */
    private final List<AgentMessage> stored = new ArrayList<>();

    private RecordingSseEmitter recorder;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(String.valueOf(USER_ID));

        sessionMapper = mock(AgentSessionMapper.class);
        messageMapper = mock(AgentMessageMapper.class);
        miniMaxClient = mock(MiniMaxClient.class);
        toolRegistry = mock(ToolRegistry.class);
        toolExecutor = mock(ChatToolExecutor.class);
        workspaceService = mock(WorkspaceService.class);

        when(sessionMapper.selectById(SESSION_ID)).thenReturn(AgentSession.builder()
                .id(SESSION_ID)
                .userId(USER_ID)
                .sceneCode("CHAT")
                .status("ACTIVE")
                .build());
        when(toolRegistry.toAnthropicTools())
                .thenReturn(List.of(new MiniMaxTool("read_local_file", "读文件", Map.of("type", "object"))));
        when(workspaceService.resolveWorkspaceRoot(eq(USER_ID), any())).thenReturn("D:\\workspace");
        when(toolExecutor.execute(any(), any(), any(), any()))
                .thenReturn(ToolResult.ok("读取了 3 字节", "文件内容是"));

        when(messageMapper.insert(any(AgentMessage.class))).thenAnswer(invocation -> {
            AgentMessage message = invocation.getArgument(0);
            message.setId((long) stored.size() + 1);
            stored.add(message);
            return 1;
        });
        when(messageMapper.selectOne(any())).thenAnswer(invocation -> {
            if (stored.isEmpty()) {
                return null;
            }
            return stored.get(stored.size() - 1);
        });
        when(messageMapper.selectList(any())).thenAnswer(invocation -> new ArrayList<>(stored));

        recorder = new RecordingSseEmitter();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private ChatServiceImpl buildService() {
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any(TransactionDefinition.class))).thenReturn(mock(TransactionStatus.class));
        TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        return new ChatServiceImpl(sessionMapper, messageMapper, miniMaxClient, toolRegistry, toolExecutor,
                workspaceService, new ObjectMapper(), transactionTemplate, executor) {
            @Override
            SseEmitter createEmitter() {
                return recorder;
            }
        };
    }

    @Test
    @DisplayName("模型返回 tool_use 后执行工具、回填结果并再次调用模型")
    void loopsWhenModelReturnsToolUse() {
        boolean[] firstCall = {true};
        doAnswer(invocation -> {
            MiniMaxStreamListener listener = invocation.getArgument(3);
            if (firstCall[0]) {
                firstCall[0] = false;
                listener.onToolUse(new MiniMaxToolUse("toolu_1", "read_local_file", Map.of("path", "a.txt")));
                listener.onStopReason("tool_use");
            } else {
                listener.onTextDelta("文件内容是");
                listener.onTextDelta("你好");
                listener.onStopReason("end_turn");
            }
            return null;
        }).when(miniMaxClient).chatStream(any(), any(), any(), any());

        ChatServiceImpl service = buildService();
        service.sendMessage(SESSION_ID, buildRequest("读一下 a.txt"));

        // 模型被调用了两轮
        verify(miniMaxClient, times(2)).chatStream(any(), any(), any(), any());

        // 工具被执行了一次，入参透传
        verify(toolExecutor).execute(eq("read_local_file"), eq(Map.of("path", "a.txt")), eq(USER_ID), any());

        // 第二轮的消息数组里带上��工具调用和工具结果
        ArgumentCaptor<List<MiniMaxMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(miniMaxClient, times(2)).chatStream(captor.capture(), any(), any(), any());
        List<MiniMaxMessage> secondRound = captor.getAllValues().get(1);

        assertEquals(3, secondRound.size());
        assertEquals("assistant", secondRound.get(1).getRole());
        assertEquals("user", secondRound.get(2).getRole());
        MiniMaxContentBlock toolUse = ((List<MiniMaxContentBlock>) secondRound.get(1).getContent()).get(0);
        assertEquals("tool_use", toolUse.getType());
        assertEquals("read_local_file", toolUse.getName());
        List<Map<String, Object>> toolResults = (List<Map<String, Object>>) secondRound.get(2).getContent();
        assertEquals("tool_result", toolResults.get(0).get("type"));
        assertEquals("toolu_1", toolResults.get(0).get("tool_use_id"));

        // 落库顺序：用户消息 -> 助手工具调用 -> 工具结果 -> 助手回答
        assertEquals(4, stored.size());
        assertEquals("USER", stored.get(0).getRole());
        assertEquals("ASSISTANT", stored.get(1).getRole());
        assertEquals("JSON", stored.get(1).getContentType());
        assertEquals("TOOL", stored.get(2).getRole());
        assertEquals("read_local_file", stored.get(2).getToolName());
        assertEquals("JSON", stored.get(2).getContentType());
        assertEquals("ASSISTANT", stored.get(3).getRole());
        assertEquals("文件内容是你好", stored.get(3).getContent());
        assertEquals(List.of(1, 2, 3, 4), stored.stream().map(AgentMessage::getSeqNo).toList());

        // SSE 事件齐全
        String all = String.join("\n", recorder.events);
        assertTrue(all.contains("tool_call"), all);
        assertTrue(all.contains("read_local_file"), all);
        assertTrue(all.contains("tool_result"), all);
        assertTrue(all.contains("text_delta"), all);
        assertTrue(all.contains("文件内容是"), all);
        assertTrue(all.contains("done"), all);
        assertTrue(recorder.completed);
    }

    @Test
    @DisplayName("别的用户的会话一律按不存在处理")
    void rejectsSessionOwnedByAnotherUser() {
        when(sessionMapper.selectById(99L)).thenReturn(AgentSession.builder()
                .id(99L)
                .userId(2L)
                .sceneCode("CHAT")
                .status("ACTIVE")
                .build());

        ChatServiceImpl service = buildService();

        BusinessException listException = assertThrows(BusinessException.class,
                () -> service.listMessages(99L));
        assertEquals(404, listException.getCode());

        BusinessException sendException = assertThrows(BusinessException.class,
                () -> service.sendMessage(99L, buildRequest("你好")));
        assertEquals(404, sendException.getCode());

        verify(miniMaxClient, times(0)).chatStream(any(), any(), any(), any());
    }

    @Test
    @DisplayName("消息内容为空直接报错，不会调模型")
    void rejectsEmptyContent() {
        ChatServiceImpl service = buildService();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.sendMessage(SESSION_ID, buildRequest("   ")));

        assertEquals(400, exception.getCode());
        verify(miniMaxClient, times(0)).chatStream(any(), any(), any(), any());
    }

    private com.elias.chat.dto.request.ChatMessageRequest buildRequest(String content) {
        com.elias.chat.dto.request.ChatMessageRequest request = new com.elias.chat.dto.request.ChatMessageRequest();
        request.setContent(content);
        return request;
    }

    /**
     * 能捕获 SSE 事件的 emitter
     */
    static class RecordingSseEmitter extends SseEmitter {

        final List<String> events = new ArrayList<>();
        boolean completed;

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            for (ResponseBodyEmitter.DataWithMediaType entry : builder.build()) {
                events.add(String.valueOf(entry.getData()));
            }
        }

        @Override
        public void send(Object object, MediaType mediaType) throws IOException {
            events.add(String.valueOf(object));
        }

        @Override
        public synchronized void complete() {
            completed = true;
        }
    }
}
