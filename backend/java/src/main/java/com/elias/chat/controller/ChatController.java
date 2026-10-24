package com.elias.chat.controller;

import com.elias.chat.dto.request.ChatMessageRequest;
import com.elias.chat.dto.request.ChatSessionCreateRequest;
import com.elias.chat.dto.request.ChatWorkspaceRequest;
import com.elias.chat.dto.response.ChatMessageResponse;
import com.elias.chat.dto.response.ChatSessionCreateResponse;
import com.elias.chat.dto.response.ChatSessionItemResponse;
import com.elias.chat.dto.response.ChatWorkspaceResponse;
import com.elias.chat.service.ChatService;
import com.elias.chat.service.WorkspaceService;
import com.elias.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 对话接口。
 *
 * <p>路径与契约第 3 节一致，全部挂在 /api/chat 下，鉴权走现有 JWT。</p>
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final WorkspaceService workspaceService;

    /**
     * 创建对话会话
     */
    @PostMapping("/sessions")
    public ApiResponse<ChatSessionCreateResponse> createSession(
            @RequestBody(required = false) ChatSessionCreateRequest request) {
        return ApiResponse.ok(chatService.createSession(request));
    }

    /**
     * 查询会话列表，按最后消息时间倒序
     */
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionItemResponse>> listSessions() {
        return ApiResponse.ok(chatService.listSessions());
    }

    /**
     * 查询会话内的消息，按 seqNo 升序
     */
    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<ChatMessageResponse>> listMessages(@PathVariable Long id) {
        return ApiResponse.ok(chatService.listMessages(id));
    }

    /**
     * 删除会话（连带消息）
     */
    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        chatService.deleteSession(id);
        return ApiResponse.ok();
    }

    /**
     * 发送消息，返回 SSE 流
     */
    @PostMapping("/sessions/{id}/messages")
    public SseEmitter sendMessage(@PathVariable Long id, @RequestBody ChatMessageRequest request) {
        return chatService.sendMessage(id, request);
    }

    /**
     * 查询当前工作区根目录
     */
    @GetMapping("/workspace")
    public ApiResponse<ChatWorkspaceResponse> getWorkspace() {
        return ApiResponse.ok(workspaceService.getWorkspace());
    }

    /**
     * 设置当前工作区根目录
     */
    @PostMapping("/workspace")
    public ApiResponse<ChatWorkspaceResponse> setWorkspace(@RequestBody ChatWorkspaceRequest request) {
        return ApiResponse.ok(workspaceService.setWorkspace(request));
    }
}
