package com.airesumeforge.service;

import com.airesumeforge.dto.agent.internal.request.AiVersionSaveRequest;
import com.airesumeforge.dto.resume.request.ResumePatchApplyRequest;
import com.airesumeforge.dto.resume.request.ResumePatchPreviewRequest;
import com.airesumeforge.dto.agent.internal.response.AiVersionSaveResponse;
import com.airesumeforge.dto.resume.response.ResumePatchApplyResponse;
import com.airesumeforge.dto.resume.response.ResumePatchPreviewResponse;

/**
 * 简历 patch 服务接口
 * ServiceImpl 负责 patch 预览、冲突检测、应用和 AI 版本保存
 */
public interface ResumePatchService {

    /**
     * 预览 patch 应用效果
     *
     * @param resumeId 简历ID
     * @param request  patch 预览请求
     * @return 预览结果
     */
    ResumePatchPreviewResponse previewPatch(Long resumeId, ResumePatchPreviewRequest request);

    /**
     * 应用 patch 到当前简历
     *
     * @param resumeId 简历ID
     * @param request  patch 应用请求
     * @return 应用结果
     */
    ResumePatchApplyResponse applyPatch(Long resumeId, ResumePatchApplyRequest request);

    /**
     * 保存 AI 修改后的简历版本
     *
     * @param resumeId 简历ID
     * @param request  版本保存请求
     * @return 版本信息
     */
    AiVersionSaveResponse saveAiVersion(Long resumeId, AiVersionSaveRequest request);
}

