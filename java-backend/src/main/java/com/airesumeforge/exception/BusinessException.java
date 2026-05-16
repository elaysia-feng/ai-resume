package com.airesumeforge.exception;

/**
 * 业务异常基类
 * 用于表达业务逻辑层面的错误（如无权限、不存在、资源冲突等）
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    // ========== 常用静态工厂 ==========

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(409, message);
    }

    public static BusinessException business(String message) {
        return new BusinessException(400, message);
    }
}
