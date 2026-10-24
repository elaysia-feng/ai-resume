package com.elias.common.minimax;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 一次工具调用的结果描述（流式解析出来的 tool_use 块）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MiniMaxToolUse {

    /**
     * 工具调用 ID，回填 tool_result 时要用
     */
    private String id;

    /**
     * 工具名
     */
    private String name;

    /**
     * 工具入参
     */
    private Map<String, Object> input;
}
