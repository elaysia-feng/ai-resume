package com.elias.agent.dto.run.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户补充答案
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClarificationAnswerRequest {

    /**
     * 被回答的问题字段标识，例如 jobDescription
     */
    @NotBlank(message = "fieldKey 不能为空")
    private String fieldKey;

    /**
     * 用户填写的补充内容
     */
    @NotBlank(message = "value 不能为空")
    private String value;
}
