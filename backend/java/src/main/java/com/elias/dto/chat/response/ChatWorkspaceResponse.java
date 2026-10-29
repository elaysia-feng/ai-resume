package com.elias.dto.chat.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作区响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatWorkspaceResponse {

    /**
     * 工作区根目录，没设置过为 null
     */
    private String path;
}
