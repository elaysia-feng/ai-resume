package com.elias.common.minimax;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Anthropic 兼容协议的内容块。
 *
 * <p>同一个数组里可能混排 text 和 tool_use 两种块：
 * text 块带 text 字段，tool_use 块带 id / name / input 字段。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MiniMaxContentBlock {

    /**
     * 块类型：text / tool_use / thinking
     */
    private String type;

    /**
     * 文本内容，仅 text 块有值
     */
    private String text;

    /**
     * 工具调用 ID，仅 tool_use 块有值
     */
    private String id;

    /**
     * 工具名，仅 tool_use 块有值
     */
    private String name;

    /**
     * 工具入参，仅 tool_use 块有值
     */
    private Map<String, Object> input;

    public static MiniMaxContentBlock ofText(String text) {
        MiniMaxContentBlock block = new MiniMaxContentBlock();
        block.setType("text");
        block.setText(text);
        return block;
    }

    public static MiniMaxContentBlock ofToolUse(String id, String name, Map<String, Object> input) {
        MiniMaxContentBlock block = new MiniMaxContentBlock();
        block.setType("tool_use");
        block.setId(id);
        block.setName(name);
        block.setInput(input);
        return block;
    }

    public boolean isToolUse() {
        return "tool_use".equals(type);
    }
}
