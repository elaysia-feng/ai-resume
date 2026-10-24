package com.elias.chat.tool;

/**
 * 工具执行异常。
 *
 * <p>消息面向模型，必须是可执行的下一步提示，不能包含越界路径等内部细节。</p>
 */
public class ToolException extends RuntimeException {

    public ToolException(String message) {
        super(message);
    }
}
