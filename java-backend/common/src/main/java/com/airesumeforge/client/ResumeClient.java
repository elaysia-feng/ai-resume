package com.airesumeforge.client;

import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.common.dto.request.ResumePatchApplyRequest;
import com.airesumeforge.common.dto.response.ResumePatchApplyResponse;
import com.airesumeforge.common.dto.response.ResumeDetailResponse;
import com.airesumeforge.common.dto.response.ResumeSnapshotResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "resume-service")
public interface ResumeClient {
    @GetMapping("/api/resumes/{resumeId}")
    ApiResponse<ResumeDetailResponse> getDetailedResume(@PathVariable Long resumeId);

    @GetMapping("/internal/resumes/{resumeId}/snapshot")
    ResumeSnapshotResponse getResumeSnapshot(@PathVariable Long resumeId);

    @PostMapping("/internal/resumes/{resumeId}/patch-apply")
    ResumePatchApplyResponse applyPatch(@PathVariable Long resumeId,
                                        @Valid @RequestBody ResumePatchApplyRequest request);

}
