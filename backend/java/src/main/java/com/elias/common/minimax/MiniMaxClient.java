package com.elias.common.minimax;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniMax 客户端（Anthropic 兼容协议）。
 *
 * <p>只负责协议层：拼请求头 / 拼 body / 解析响应。
 * 业务侧的对话循环在 chat 模块里做。</p>
 *
 * <p>缺 api-key 时不在构造或启动阶段报错，只有真正调用时才抛出清晰的中文错误。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MiniMaxClient {

    /**
     * Anthropic 兼容协议版本
     */
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    /**
     * 消息接口路径
     */
    private static final String MESSAGES_PATH = "/v1/messages";

    private final MiniMaxProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    /**
     * 非流式调用。
     *
     * @param messages 历史消息（含本轮用户输入）
     * @param tools   工具声明，可为空
     * @return 模型响应
     */
    public MiniMaxResponse chat(List<MiniMaxMessage> messages, List<MiniMaxTool> tools) {
        return chat(messages, tools, null);
    }

    /**
     * 非流式调用。
     *
     * @param messages 历史消息（含本轮用户输入）
     * @param tools   工具声明，可为空
     * @param system  系统提示词，可为空
     * @return 模型响应
     */
    public MiniMaxResponse chat(List<MiniMaxMessage> messages, List<MiniMaxTool> tools, String system) {
        String raw = post(buildBody(messages, tools, system, false), false);
        return parseResponse(raw);
    }

    /**
     * 流式调用，逐块回调。
     *
     * @param messages 历史消息（含本轮用户输入）
     * @param tools   工具声明，可为空
     * @param listener 回调
     */
    public void chatStream(List<MiniMaxMessage> messages, List<MiniMaxTool> tools,
            MiniMaxStreamListener listener) {
        chatStream(messages, tools, null, listener);
    }

    /**
     * 流式调用，逐块回调。
     *
     * @param messages 历史消息（含本轮用户输入）
     * @param tools   工具声明，可为空
     * @param system  系统提示词，可为空
     * @param listener 回调
     */
    public void chatStream(List<MiniMaxMessage> messages, List<MiniMaxTool> tools, String system,
            MiniMaxStreamListener listener) {
        post(buildBody(messages, tools, system, true), true, listener);
    }

    /**
     * 解析非流式响应文本，工具调用场景下主要用来验证 tool_use 块和 stop_reason 的解析。
     */
    public MiniMaxResponse parseResponse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, MiniMaxResponse.class);
        } catch (JsonProcessingException e) {
            throw new MiniMaxException("MiniMax 返回内容解析失败：" + e.getOriginalMessage(), e);
        }
    }

    /**
     * 拼请求体。
     */
    private Map<String, Object> buildBody(List<MiniMaxMessage> messages, List<MiniMaxTool> tools,
            String system, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.getModel());
        body.put("max_tokens", properties.getMaxTokens());
        body.put("messages", messages == null ? List.of() : messages);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
        }
        if (system != null && !system.isBlank()) {
            // Anthropic 兼容协议里 system 是顶层字段，不能塞进 messages 数组
            body.put("system", system);
        }
        if (stream) {
            body.put("stream", true);
        }
        return body;
    }

    /**
     * 非流式请求。
     */
    private String post(Map<String, Object> body, boolean stream) {
        return post(body, stream, null);
    }

    /**
     * 发起请求。stream=true 时按 SSE 解析，stream=false 时返回原始响应文本。
     */
    private String post(Map<String, Object> body, boolean stream, MiniMaxStreamListener listener) {
        requireApiKey();
        try {
            return restClientBuilder.build()
                    .post()
                    .uri(messagesUrl())
                    .headers(headers -> applyHeaders(headers))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange((request, response) -> handleResponse(response, stream, listener));
        } catch (RestClientResponseException e) {
            throw new MiniMaxException("MiniMax 请求失败，HTTP " + e.getStatusCode().value() + "：" + e.getResponseBodyAsString(), e);
        } catch (MiniMaxException e) {
            throw e;
        } catch (Exception e) {
            throw new MiniMaxException("MiniMax 请求异常：" + e.getMessage(), e);
        }
    }

    /**
     * 处理响应：流式走 SSE 解析，非流式直接返回文本。
     */
    private String handleResponse(ClientHttpResponse response, boolean stream, MiniMaxStreamListener listener)
            throws IOException {
        if (!response.getStatusCode().is2xxSuccessful()) {
            String errorBody = response.getBody() == null ? ""
                    : StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
            throw new MiniMaxException("MiniMax 接口返回 HTTP " + response.getStatusCode().value() + "：" + errorBody);
        }
        if (!stream) {
            return StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        }
        try (InputStream inputStream = response.getBody()) {
            parseSse(inputStream, listener);
        }
        return null;
    }

    /**
     * 解析 Anthropic 兼容的 SSE 流。
     */
    private void parseSse(InputStream inputStream, MiniMaxStreamListener listener) throws IOException {
        StreamParseState state = new StreamParseState();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder eventData = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    dispatchEvent(eventData.toString(), listener, state);
                    eventData.setLength(0);
                    continue;
                }
                if (line.startsWith("data:")) {
                    if (eventData.length() > 0) {
                        eventData.append('\n');
                    }
                    eventData.append(line.substring("data:".length()).trim());
                }
                // event: / id: / retry: 行对本客户端没有意义，直接忽略
            }
            dispatchEvent(eventData.toString(), listener, state);
        }
    }

    /**
     * 处理一个 SSE 事件。
     */
    private void dispatchEvent(String eventData, MiniMaxStreamListener listener, StreamParseState state) {
        if (eventData == null || eventData.isBlank()) {
            return;
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(eventData);
        } catch (JsonProcessingException e) {
            throw new MiniMaxException("MiniMax 流式数据解析失败：" + e.getOriginalMessage(), e);
        }
        String type = node.path("type").asText("");
        switch (type) {
            case "content_block_start" -> {
                JsonNode block = node.path("content_block");
                int index = node.path("index").asInt(0);
                if ("tool_use".equals(block.path("type").asText())) {
                    state.startTool(index, block.path("id").asText(), block.path("name").asText(),
                            block.path("input"));
                }
            }
            case "content_block_delta" -> {
                JsonNode delta = node.path("delta");
                int index = node.path("index").asInt(0);
                String deltaType = delta.path("type").asText("");
                if ("text_delta".equals(deltaType)) {
                    String text = delta.path("text").asText("");
                    if (!text.isEmpty()) {
                        listener.onTextDelta(text);
                    }
                } else if ("input_json_delta".equals(deltaType)) {
                    state.appendToolJson(index, delta.path("partial_json").asText(""));
                }
            }
            case "content_block_stop" -> {
                MiniMaxToolUse toolUse = state.finishTool(node.path("index").asInt(0));
                if (toolUse != null) {
                    listener.onToolUse(toolUse);
                }
            }
            case "message_delta" -> {
                String stopReason = node.path("delta").path("stop_reason").asText("");
                if (!stopReason.isBlank()) {
                    listener.onStopReason(stopReason);
                }
            }
            case "error" -> throw new MiniMaxException("MiniMax 返回错误："
                    + node.path("error").path("message").asText(eventData));
            default -> {
                // message_start / message_stop / ping 等事件无需处理
            }
        }
    }

    /**
     * 鉴权与协议头。
     */
    private void applyHeaders(org.springframework.http.HttpHeaders headers) {
        String token = properties.getAnthropicApiKey().trim();
        headers.set("x-api-key", token);
        headers.set("authorization", "Bearer " + token);
        headers.set("anthropic-version", ANTHROPIC_VERSION);
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    /**
     * 消息接口完整地址。
     */
    private String messagesUrl() {
        String baseUrl = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + MESSAGES_PATH;
    }

    /**
     * api-key 缺失时给出明确中文提示，不在启动阶段触发。
     */
    private void requireApiKey() {
        String token = properties.getAnthropicApiKey();
        if (token == null || token.isBlank()) {
            throw new MiniMaxException("MiniMax API Key 未配置，请在 application.yaml 的 minimax.anthropic-api-key "
                    + "或环境变量 MINIMAX_ANTHROPIC_API_KEY 中填写");
        }
    }

    /**
     * 流式解析过程中的工具调用累积状态。
     */
    private final class StreamParseState {

        /**
         * 内容块下标 -> 工具调用累积器
         */
        private final Map<Integer, ToolAccumulator> tools = new HashMap<>();

        void startTool(int index, String id, String name, JsonNode input) {
            ToolAccumulator accumulator = new ToolAccumulator();
            accumulator.id = id;
            accumulator.name = name;
            if (input != null && input.isObject() && !input.isEmpty()) {
                accumulator.initialInput = objectMapper.convertValue(input, new TypeReference<Map<String, Object>>() {
                });
            }
            tools.put(index, accumulator);
        }

        void appendToolJson(int index, String partialJson) {
            ToolAccumulator accumulator = tools.get(index);
            if (accumulator != null && partialJson != null && !partialJson.isEmpty()) {
                accumulator.json.append(partialJson);
            }
        }

        MiniMaxToolUse finishTool(int index) {
            ToolAccumulator accumulator = tools.remove(index);
            if (accumulator == null) {
                return null;
            }
            Map<String, Object> input = accumulator.initialInput;
            if (input == null && accumulator.json.length() > 0) {
                try {
                    input = objectMapper.readValue(accumulator.json.toString(),
                            new TypeReference<Map<String, Object>>() {
                            });
                } catch (JsonProcessingException e) {
                    throw new MiniMaxException("MiniMax 工具入参解析失败：" + e.getOriginalMessage(), e);
                }
            }
            return new MiniMaxToolUse(accumulator.id, accumulator.name, input == null ? Map.of() : input);
        }

        private static final class ToolAccumulator {
            private String id;
            private String name;
            private Map<String, Object> initialInput;
            private final StringBuilder json = new StringBuilder();
        }
    }
}
