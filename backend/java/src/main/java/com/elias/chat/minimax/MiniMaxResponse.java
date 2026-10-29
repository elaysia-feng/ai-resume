package com.elias.chat.minimax;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Anthropic 兼容协议的非流式响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MiniMaxResponse {

    /**
     * 响应 ID
     */
    private String id;

    /**
     * 实际使用的模型名
     */
    private String model;

    /**
     * 停止原因：end_turn / tool_use / max_tokens ...
     */
    @JsonProperty("stop_reason")
    private String stopReason;

    /**
     * 内容块数组，可能混排 text 与 tool_use
     */
    private List<MiniMaxContentBlock> content;

    /**
     * token 用量
     */
    private MiniMaxUsage usage;

    /**
     * 取出全部 tool_use 块
     */
    public List<MiniMaxToolUse> toolUses() {
        if (content == null) {
            return List.of();
        }
        return content.stream()
                .filter(MiniMaxContentBlock::isToolUse)
                .map(block -> new MiniMaxToolUse(block.getId(), block.getName(), block.getInput()))
                .toList();
    }

    /**
     * 取出全部文本内容
     */
    public String text() {
        if (content == null) {
            return "";
        }
        return content.stream()
                .filter(block -> "text".equals(block.getType()))
                .map(MiniMaxContentBlock::getText)
                .filter(text -> text != null)
                .reduce("", String::concat);
    }
}
