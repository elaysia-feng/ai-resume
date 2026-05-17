package com.airesumeforge.client;

import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.common.dto.interview.internal.request.InternalInterviewBootstrapRequest;
import com.airesumeforge.common.dto.interview.internal.request.InternalInterviewQuestionAnalysisRequest;
import com.airesumeforge.common.dto.interview.internal.response.InternalInterviewBootstrapResponse;
import com.airesumeforge.common.dto.interview.internal.response.InternalInterviewRoundDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "interview-service")
public interface InterviewClient {

    @PostMapping("/internal/agent/interviewBootstrap")
    ApiResponse<InternalInterviewBootstrapResponse> interviewBootstrap(@RequestBody InternalInterviewBootstrapRequest request);

    @PostMapping("/internal/agent/interview/rounds/{roundId}/analysis")
    ApiResponse<Void> updateQuestionAnalysis(@PathVariable Long roundId,
                                             @RequestBody InternalInterviewQuestionAnalysisRequest request);

    @GetMapping("/internal/agent/interview/rounds/{roundId}")
    ApiResponse<InternalInterviewRoundDetailResponse> getQuestionAnswer(@PathVariable Long roundId);
}