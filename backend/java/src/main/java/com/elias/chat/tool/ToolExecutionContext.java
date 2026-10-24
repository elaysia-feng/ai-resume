package com.elias.chat.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具执行上下文。
 *
 * <p>工作区根目录由调用方统一解析后下发，工具本身不查库。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionContext {

    /**
     * 当前用户ID
     */
    private Long userId;

    /**
     * 工作区根目录，为 null 表示用户还没选择工作区
     */
    private String workspaceRoot;

    /**
     * 模型给出的工具入参
     */
    private Map<String, Object> input;

    /**
     * 读取字符串入参
     */
    public String stringArg(String key) {
        Object value = input == null ? null : input.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 读取整型入参，缺省或非法时返回默认值
     */
    public int intArg(String key, int defaultValue) {
        Object value = input == null ? null : input.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 读取布尔入参，缺省时返回默认值
     */
    public boolean booleanArg(String key, boolean defaultValue) {
        Object value = input == null ? null : input.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value).trim());
    }
}
