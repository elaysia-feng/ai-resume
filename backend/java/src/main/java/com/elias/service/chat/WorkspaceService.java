package com.elias.service.chat;

import com.elias.dto.chat.request.ChatWorkspaceRequest;
import com.elias.dto.chat.response.ChatWorkspaceResponse;

/**
 * 工作区服务。
 */
public interface WorkspaceService {

    /**
     * 读取当前用户的工作区根目录
     */
    ChatWorkspaceResponse getWorkspace();

    /**
     * 保存当前用户的工作区根目录
     */
    ChatWorkspaceResponse setWorkspace(ChatWorkspaceRequest request);

    /**
     * 解析本轮对话使用的工作区根目录。
     *
     * <p>本轮显式传了就校验并落库，否则沿用已保存的工作区。</p>
     *
     * @param userId        当前用户ID
     * @param requestedPath 本轮传入的路径，可为空
     * @return 工作区根目录，没设置过时为 null
     */
    String resolveWorkspaceRoot(Long userId, String requestedPath);
}
