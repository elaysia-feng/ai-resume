package com.elias.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一API返回格式
 *
 * @param <T> data类型
 */
@Data
@AllArgsConstructor
public class ApiResponse<T> {

    /** 状态码：200成功，其他失败 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 数据（可为空） */
    private T data;

    // ========== 成功响应 ==========

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(200, "success", null);
    }

    // ========== 失败响应 ==========

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(400, message, null);
    }
}
