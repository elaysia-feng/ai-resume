package com.airesumeforge.resume.controller;

import com.airesumeforge.common.dto.agent.internal.request.AiVersionSaveRequest;
import com.airesumeforge.resume.dto.request.ResumePatchApplyRequest;
import com.airesumeforge.resume.dto.request.ResumePatchPreviewRequest;
import com.airesumeforge.resume.service.ResumePatchService;
import com.airesumeforge.common.dto.agent.internal.response.AiVersionSaveResponse;
import com.airesumeforge.resume.dto.response.ResumePatchApplyResponse;
import com.airesumeforge.resume.dto.response.ResumePatchPreviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 简历 patch 内部接口控制器
 * 用于 Agent patch 预览、应用和 AI 版本保存
 */
@RestController
@RequestMapping("/internal/resumes")
@RequiredArgsConstructor
public class InternalResumePatchController {

    private final ResumePatchService resumePatchService;

    /**
     * 预览 patch 应用效果
     *
     * @param resumeId 简历ID
     * @param request  预览请求
     * @return 预览结果
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
