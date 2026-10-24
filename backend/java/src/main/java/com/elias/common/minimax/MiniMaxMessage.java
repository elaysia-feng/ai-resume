package com.elias.common.minimax;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Anthropic 兼容协议的一条消息。
 *
 * <p>content 允许两种形态：纯字符串，或内容块数组（tool_use 回填时必须用数组）。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MiniMaxMessage {

    /**
     * 角色：user / assistant
     */
    private String role;

    /**
     * 消息内容：字符串或 List&lt;MiniMaxContentBlock&gt;
     */
    private Object content;
}
