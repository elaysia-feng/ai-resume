package com.elias.common.client;

import com.elias.common.ApiResponse;
import com.elias.common.dto.agent.internal.request.InternalAgentRunCreateRequest;
import com.elias.common.dto.agent.internal.request.InternalBootstrapRequest;
import com.elias.common.dto.agent.internal.request.RunEventBatchRequest;
import com.elias.common.dto.agent.internal.request.RunStatusUpdateRequest;
import com.elias.common.dto.agent.internal.response.InternalAgentRunCreateResponse;
import com.elias.common.dto.agent.internal.response.InternalAgentRunDetailResponse;
import com.elias.common.dto.agent.internal.response.InternalBootstrapResponse;
import com.elias.common.dto.response.AgentSessionDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "agent-service")
public interface AgentClient {

    @PostMapping("/internal/agent/bootstrap")
    ApiResponse<InternalBootstrapResponse> bootstrap(@RequestBody InternalBootstrapRequest request);

    @PostMapping("/internal/agent/runs/{runId}/events/saveBatch")
    ApiResponse<Void> saveRunEvents(@PathVariable Long runId, @RequestBody RunEventBatchRequest request);

    @PostMapping("/internal/agent/runs/{runId}/status")
    ApiResponse<Void> updateRunStatus(@PathVariable Long runId, @RequestBody RunStatusUpdateRequest request);

    @PostMapping("/internal/agent/runs/{runId}/claim")
    ApiResponse<Boolean> claimRun(@PathVariable Long runId);

    @GetMapping("/internal/agent/runs/{runId}/status")
    ApiResponse<String> getRunStatus(@PathVariable Long runId);

    @PostMapping("/internal/agent/runs/interview")
    ApiResponse<InternalAgentRunCreateResponse> createInterviewRun(@RequestBody InternalAgentRunCreateRequest request);

    @GetMapping("/internal/agent/runs/{runId}")
    ApiResponse<InternalAgentRunDetailResponse> getInternalRunDetail(@PathVariable Long runId);

    @GetMapping("/api/agent/sessions/{sessionId}")
    ApiResponse<AgentSessionDetailResponse> getSessionStatus(@PathVariable Long sessionId);
}
