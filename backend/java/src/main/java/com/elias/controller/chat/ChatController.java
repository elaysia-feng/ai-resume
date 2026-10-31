package com.elias.controller.chat;

import com.elias.dto.chat.request.ChatMessageRequest;
import com.elias.dto.chat.request.ChatSessionCreateRequest;
import com.elias.dto.chat.request.ChatWorkspaceRequest;
import com.elias.dto.chat.response.ChatMessageResponse;
import com.elias.dto.chat.response.ChatSessionCreateResponse;
import com.elias.dto.chat.response.ChatSessionItemResponse;
import com.elias.dto.chat.response.ChatWorkspaceResponse;
import com.elias.service.chat.ChatSessionService;
import com.elias.service.chat.WorkspaceService;
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

    private final ChatSessionService chatSessionService;
    private final WorkspaceService workspaceService;

    /**
     * 创建聊天会话。
     *
     * @param request 会话标题和可选简历 ID
     * @return 新建会话信息
     */
    @PostMapping("/sessions")
    public ApiResponse<ChatSessionCreateResponse> createSession(
            @RequestBody(required = false) ChatSessionCreateRequest request) {
        return ApiResponse.ok(chatSessionService.createSession(request));
    }

    /**
     * 查询当前用户的会话列表，按最后消息时间倒序。
     *
     * @return 当前用户的聊天会话
     */
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionItemResponse>> listSessions() {
        return ApiResponse.ok(chatSessionService.listSessions());
    }

    /**
     * 查询会话内的消息，按 seqNo 升序。
     *
     * @param id 会话 ID
     * @return 会话消息列表
     */
    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<ChatMessageResponse>> listMessages(@PathVariable Long id) {
        return ApiResponse.ok(chatSessionService.listMessages(id));
    }

    /**
     * 删除当前用户的会话及其消息。
     *
     * @param id 会话 ID
     * @return 空成功响应
     */
    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        chatSessionService.deleteSession(id);
        return ApiResponse.ok();
    }

    /**
     * 保存用户消息并代理 Python 的 SSE 对话流。
     *
     * @param id 会话 ID
     * @param request 用户消息和可选工作区路径
     * @return 对话事件流
     */
    @PostMapping("/sessions/{id}/messages")
    public SseEmitter sendMessage(@PathVariable Long id, @RequestBody ChatMessageRequest request) {
        return chatSessionService.sendMessage(id, request);
    }

    /**
     * 查询当前用户保存的工作区根目录。
     *
     * @return 工作区信息
     */
    @GetMapping("/workspace")
    public ApiResponse<ChatWorkspaceResponse> getWorkspace() {
        return ApiResponse.ok(workspaceService.getWorkspace());
    }

    /**
     * 设置当前用户的工作区根目录。
     *
     * @param request 新工作区路径
     * @return 已保存的工作区信息
     */
    @PostMapping("/workspace")
    public ApiResponse<ChatWorkspaceResponse> setWorkspace(@RequestBody ChatWorkspaceRequest request) {
        return ApiResponse.ok(workspaceService.setWorkspace(request));
    }
}
