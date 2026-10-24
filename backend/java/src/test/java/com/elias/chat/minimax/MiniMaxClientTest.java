package com.elias.chat.minimax;

import com.elias.common.minimax.MiniMaxClient;
import com.elias.common.minimax.MiniMaxException;
import com.elias.common.minimax.MiniMaxMessage;
import com.elias.common.minimax.MiniMaxProperties;
import com.elias.common.minimax.MiniMaxResponse;
import com.elias.common.minimax.MiniMaxStreamListener;
import com.elias.common.minimax.MiniMaxTool;
import com.elias.common.minimax.MiniMaxToolUse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * MiniMax 客户端的协议解析。
 *
 * <p>重点验证两件事：tool_use 块能不能解析出来，stop_reason 能不能正确落到模型上。</p>
 */
class MiniMaxClientTest {

    private static final String MESSAGES_URL = "https://api.minimaxi.com/anthropic/v1/messages";

    private MiniMaxProperties properties;
    private MockRestServiceServer server;
    private MiniMaxClient client;

    @BeforeEach
    void setUp() {
        properties = new MiniMaxProperties();
        properties.setAnthropicApiKey("test-token");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MiniMaxClient(properties, new ObjectMapper(), builder);
    }

    @Test
    @DisplayName("非流式响应能解析出 tool_use 块和 stop_reason")
    void parsesToolUseBlockAndStopReason() {
        String body = """
                {
                  "id": "msg_1",
                  "model": "MiniMax-M3.1-Flash",
                  "role": "assistant",
                  "content": [
                    {"type": "text", "text": "我先看看文件"},
                    {"type": "tool_use", "id": "toolu_01", "name": "read_local_file",
                     "input": {"path": "a.txt"}}
                  ],
                  "stop_reason": "tool_use",
                  "usage": {"input_tokens": 10, "output_tokens": 20}
                }
                """;
        server.expect(requestTo(MESSAGES_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-token"))
                .andExpect(header("authorization", "Bearer test-token"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("read_local_file")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        MiniMaxResponse response = client.chat(
                List.of(new MiniMaxMessage("user", "读一下 a.txt")),
                List.of(new MiniMaxTool("read_local_file", "读文件", Map.of("type", "object"))));

        assertEquals("tool_use", response.getStopReason());
        assertEquals("MiniMax-M3.1-Flash", response.getModel());
        assertTrue(response.text().contains("我先看看文件"));
        List<MiniMaxToolUse> toolUses = response.toolUses();
        assertEquals(1, toolUses.size());
        assertEquals("toolu_01", toolUses.get(0).getId());
        assertEquals("read_local_file", toolUses.get(0).getName());
        assertEquals("a.txt", toolUses.get(0).getInput().get("path"));
        server.verify();
    }

    @Test
    @DisplayName("流式响应能把分片的工具入参拼完整")
    void parsesStreamingToolUse() {
        String sse = """
                event: message_start
                data: {"type":"message_start","message":{"id":"msg_2","model":"MiniMax-M3.1-Flash"}}

                event: content_block_start
                data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"你好"}}

                event: content_block_stop
                data: {"type":"content_block_stop","index":0}

                event: content_block_start
                data: {"type":"content_block_start","index":1,"content_block":{"type":"tool_use","id":"toolu_02","name":"list_dir","input":{}}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":1,"delta":{"type":"input_json_delta","partial_json":"{\\"path\\":\\"src\\"}"}}

                event: content_block_stop
                data: {"type":"content_block_stop","index":1}

                event: message_delta
                data: {"type":"message_delta","delta":{"stop_reason":"tool_use"},"usage":{"output_tokens":15}}

                event: message_stop
                data: {"type":"message_stop"}
                """;
        server.expect(requestTo(MESSAGES_URL))
                .andRespond(withSuccess(sse, MediaType.TEXT_EVENT_STREAM));

        StringBuilder text = new StringBuilder();
        List<MiniMaxToolUse> toolUses = new ArrayList<>();
        String[] stopReason = new String[1];

        client.chatStream(List.of(new MiniMaxMessage("user", "列一下目录")), List.of(),
                new MiniMaxStreamListener() {
                    @Override
                    public void onTextDelta(String delta) {
                        text.append(delta);
                    }

                    @Override
                    public void onToolUse(MiniMaxToolUse toolUse) {
                        toolUses.add(toolUse);
                    }

                    @Override
                    public void onStopReason(String reason) {
                        stopReason[0] = reason;
                    }
                });

        assertEquals("你好", text.toString());
        assertEquals("tool_use", stopReason[0]);
        assertEquals(1, toolUses.size());
        assertEquals("toolu_02", toolUses.get(0).getId());
        assertEquals("list_dir", toolUses.get(0).getName());
        assertEquals("src", toolUses.get(0).getInput().get("path"));
        server.verify();
    }

    @Test
    @DisplayName("没有配置 api-key 时调用才报错，且给出中文提示")
    void failsWithClearMessageWhenApiKeyMissing() {
        properties.setAnthropicApiKey("  ");

        MiniMaxException exception = assertThrows(MiniMaxException.class,
                () -> client.chat(List.of(new MiniMaxMessage("user", "你好")), List.of()));

        assertTrue(exception.getMessage().contains("API Key"));
        assertTrue(exception.getMessage().contains("MINIMAX_ANTHROPIC_API_KEY"));
    }
}
