package com.elias.dto.chat.request;

import lombok.Data;

/**
 * 设置工作区请求。
 */
@Data
public class ChatWorkspaceRequest {

    /**
     * 工作区根目录，必须是绝对路径且已存在的目录
     */
    private String path;
}
