package com.elias.chat.service;

import com.elias.chat.dto.request.ChatMessageRequest;
import com.elias.chat.dto.request.ChatSessionCreateRequest;
import com.elias.chat.dto.response.ChatMessageResponse;
import com.elias.chat.dto.response.ChatSessionCreateResponse;
import com.elias.chat.dto.response.ChatSessionItemResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 对话服务。
 */
public interface ChatService {

    /**
     * 创建对话会话
     */
    ChatSessionCreateResponse createSession(ChatSessionCreateRequest request);

    /**
     * 查询当前用户的会话列表
     */
    List<ChatSessionItemResponse> listSessions();

    /**
     * 查询会话内的消息列表
     */
    List<ChatMessageResponse> listMessages(Long sessionId);

    /**
     * 删除会话（连带消息）
     */
    void deleteSession(Long sessionId);

    /**
     * 发送消息并以 SSE 形式返回对话过程
     */
    SseEmitter sendMessage(Long sessionId, ChatMessageRequest request);
}
