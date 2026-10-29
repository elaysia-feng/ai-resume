package com.elias.chat.minimax;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Anthropic 兼容协议的工具声明。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MiniMaxTool {

    /**
     * 工具名
     */
    private String name;

    /**
     * 工具描述，模型据此决定何时调用
     */
    private String description;

    /**
     * 入参 JSON Schema
     */
    @JsonProperty("input_schema")
    private Map<String, Object> inputSchema;
}
