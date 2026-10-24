package com.elias.common.minimax;

/**
 * MiniMax 流式响应回调。
 *
 * <p>流式协议里 tool_use 的入参是分片下发的（input_json_delta），
 * 客户端拼完整后才回调 onToolUse。</p>
 */
public interface MiniMaxStreamListener {

    /**
     * 文本增量
     */
    void onTextDelta(String text);

    /**
     * 完整的工具调用
     */
    void onToolUse(MiniMaxToolUse toolUse);

    /**
     * 停止原因，例如 end_turn / tool_use
     */
    void onStopReason(String stopReason);
}
