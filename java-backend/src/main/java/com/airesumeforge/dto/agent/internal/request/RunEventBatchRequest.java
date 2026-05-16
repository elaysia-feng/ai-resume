package com.airesumeforge.dto.agent.internal.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Python 批量上报 Agent run 事件请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunEventBatchRequest {

    /**
     * Python 批量上报的事件列表
     */
    @Valid
    private List<RunEventRequest> events = new ArrayList<>();
}

