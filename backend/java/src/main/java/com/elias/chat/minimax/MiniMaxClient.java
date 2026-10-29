package com.elias.chat.minimax;

import java.util.List;

/** MiniMax Anthropic 兼容协议客户端契约。 */
public interface MiniMaxClient {

    MiniMaxResponse chat(List<MiniMaxMessage> messages, List<MiniMaxTool> tools);

    MiniMaxResponse chat(List<MiniMaxMessage> messages, List<MiniMaxTool> tools, String system);

    void chatStream(List<MiniMaxMessage> messages, List<MiniMaxTool> tools, MiniMaxStreamListener listener);

    void chatStream(
            List<MiniMaxMessage> messages,
            List<MiniMaxTool> tools,
            String system,
            MiniMaxStreamListener listener);

    MiniMaxResponse parseResponse(String rawBody);
}
