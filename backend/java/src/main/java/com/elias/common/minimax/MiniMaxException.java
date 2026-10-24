package com.elias.common.minimax;

/**
 * MiniMax 调用异常。
 *
 * <p>消息面向最终用户，全部用中文描述，方便直接透出到前端。</p>
 */
public class MiniMaxException extends RuntimeException {

    public MiniMaxException(String message) {
        super(message);
    }

    public MiniMaxException(String message, Throwable cause) {
        super(message, cause);
    }
}
