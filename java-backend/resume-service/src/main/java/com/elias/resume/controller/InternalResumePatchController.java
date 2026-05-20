package com.elias.resume.controller;

import com.elias.common.dto.agent.internal.request.AiVersionSaveRequest;
import com.elias.common.dto.request.ResumePatchApplyRequest;
import com.elias.common.dto.response.ResumeSnapshotResponse;
import com.elias.resume.dto.request.ResumePatchPreviewRequest;
import com.elias.resume.service.ResumePatchService;
import com.elias.common.dto.agent.internal.response.AiVersionSaveResponse;
import com.elias.common.dto.response.ResumePatchApplyResponse;
import com.elias.resume.dto.response.ResumePatchPreviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/resumes")
@RequiredArgsConstructor
public class InternalResumePatchController {

    private final ResumePatchService resumePatchService;

    /**
     * 获取简历快照
     */
    @GetMapping("/{resumeId}/snapshot")
    public ResumeSnapshotResponse getSnapshot(@PathVariable Long resumeId) {
        return resumePatchService.getSnapshot(resumeId);
    }

    /**
     * 预览 patch 应用效果
     */
    @PostMapping("/{resumeId}/patch-preview")
    public ResumePatchPreviewResponse previewPatch(@PathVariable Long resumeId,
                                                   @Valid @RequestBody ResumePatchPreviewRequest request) {
        return resumePatchService.previewPatch(resumeId, request);
    }

    /**
     * 应用 patch 到当前简历
     *
     * @param resumeId 简历ID
     * @param request  应用请求
     * @return 应用结果
     */
    @PostMapping("/{resumeId}/patch-apply")
    public ResumePatchApplyResponse applyPatch(@PathVariable Long resumeId,
                                               @Valid @RequestBody ResumePatchApplyRequest request) {
        return resumePatchService.applyPatch(resumeId, request);
    }

    /**
     * 保存 AI 版本
     *
     * @param resumeId 简历ID
     * @param request  版本保存请求
     * @return 版本信息
     */
    @PostMapping("/{resumeId}/versions/ai")
    public AiVersionSaveResponse saveAiVersion(@PathVariable Long resumeId,
                                               @Valid @RequestBody AiVersionSaveRequest request) {
        return resumePatchService.saveAiVersion(resumeId, request);
    }
}
