package com.elias.agent.dto.run.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送Agent消息请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessageCreateRequest {

    /**
     * 消息角色，前端发起时通常为 USER
     */
    private String role = "USER";

    /**
     * 消息正文
     */
    @NotBlank(message = "content 不能为空")
    private String content;

    /**
     * 内容类型：TEXT / JSON
     */
    private String contentType = "TEXT";
}
