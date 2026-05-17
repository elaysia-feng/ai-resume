package com.airesumeforge.resume.controller;

import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.resume.dto.request.ResumeCreateRequest;
import com.airesumeforge.resume.dto.request.ResumeUpdateRequest;
import com.airesumeforge.resume.dto.request.ResumeVersionSaveRequest;
import com.airesumeforge.resume.dto.request.SectionCreateRequest;
import com.airesumeforge.resume.dto.request.SectionUpdateRequest;
import com.airesumeforge.resume.dto.request.SectionReorderRequest;
import com.airesumeforge.common.dto.response.ResumeDetailResponse;
import com.airesumeforge.resume.service.ResumeService;
import com.airesumeforge.resume.dto.response.ResumeAvatarUploadResponse;
import com.airesumeforge.resume.dto.response.ResumeListResponse;
import com.airesumeforge.resume.dto.response.ResumeVersionDetailResponse;
import com.airesumeforge.resume.dto.response.ResumeVersionItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {
    private final ResumeService resumeService;

    /**
     * 创建空白简历
     * @param resumeCreateRequest
     * @return
     */
    @PostMapping
    public ApiResponse<Void> createResume(@Valid @RequestBody ResumeCreateRequest resumeCreateRequest) {
        resumeService.createResume(resumeCreateRequest);
        return ApiResponse.ok();
    }

    /**
     * 获取当前用户所有简历
     * @return
     */
    @GetMapping()
    public ApiResponse<List<ResumeListResponse>> getResume() {
        return ApiResponse.ok(resumeService.getResume());
    }


    /**
     * 查询简历的详细信息
     * @param resumeId
     * @return
     */
    @GetMapping("/{resumeId}")
    public ApiResponse<ResumeDetailResponse> getDetailedResume(@PathVariable Long resumeId) {
        return ApiResponse.ok(resumeService.getDetailedResume(resumeId));
    }

    /**
     * 只更新简历使用的模板
     * @param resumeId
     * @param resumeUpdateRequest
     * @return
     */
    @PutMapping("/{resumeId}")
    public ApiResponse<Void> updateResume(@PathVariable Long resumeId, @Valid @RequestBody ResumeUpdateRequest resumeUpdateRequest) {
        resumeService.updateResume(resumeId, resumeUpdateRequest);
        return ApiResponse.ok();
    }

    /**
     * 上传简历头像
     * @param resumeId
     * @param file
     * @return
     */
    @PostMapping("/{resumeId}/avatar")
    public ApiResponse<ResumeAvatarUploadResponse> uploadResumeAvatar(@PathVariable Long resumeId,
                                                                      @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(resumeService.uploadResumeAvatar(resumeId, file));
    }

    /**
     * 删除简历
     * @param resumeId
     * @return
     */
    @DeleteMapping("/{resumeId}")
    public ApiResponse<Void> deleteResume(@PathVariable Long resumeId) {
        resumeService.deleteResume(resumeId);
        return ApiResponse.ok();
    }

    /**
     * 添加模块
     * @param resumeId
     * @return
     */
    @PostMapping("/{resumeId}/sections")
    public ApiResponse<Void> addSection(@Valid @RequestBody SectionCreateRequest sectionCreateRequest, @PathVariable Long resumeId){
        resumeService.addSection(sectionCreateRequest, resumeId);
        return ApiResponse.ok();
    }

    /**
     * 更新模块
     * @param sectionUpdateRequest
     * @param resumeId
     * @param sectionId
     * @return
     */
    @PutMapping("{resumeId}/sections/{sectionId}")
    public ApiResponse<Void> updateSection(@PathVariable Long resumeId, @PathVariable Long sectionId,
                                           @Valid @RequestBody SectionUpdateRequest sectionUpdateRequest){
        resumeService.updateSection(sectionUpdateRequest, resumeId, sectionId);
        return ApiResponse.ok();
    }

    /**
     * 删除模块
     * @param resumeId
     * @param sectionId
     * @return
     */
    @DeleteMapping("{resumeId}/sections/{sectionId}")
    public ApiResponse<Void> deleteSection(@PathVariable Long resumeId, @PathVariable Long sectionId) {
        resumeService.deleteSection(resumeId, sectionId);
        return ApiResponse.ok();
    }

    @PutMapping("{resumeId}/sections/reorder")
    public ApiResponse<Void> reorderSection(@PathVariable Long resumeId,
                                            @Valid @RequestBody SectionReorderRequest sectionReorderRequest) {
        resumeService.reorderSection(resumeId, sectionReorderRequest);
        return ApiResponse.ok();
    }


    // 保存当前版本
    @PostMapping("{resumeId}/versions")
    public ApiResponse<Void> saveResume(@Valid @RequestBody ResumeVersionSaveRequest resumeVersionSaveRequest, @PathVariable Long resumeId) {
        resumeService.saveResume(resumeVersionSaveRequest, resumeId);
        return ApiResponse.ok();
    }
    // 版本列表
    @GetMapping("/{resumeId}/versions")
    public ApiResponse<List<ResumeVersionItemResponse>> listVersionResume(@PathVariable Long resumeId) {
        return ApiResponse.ok(resumeService.listVersionResume(resumeId));
    }

    // 版本详情
    @GetMapping("/{resumeId}/versions/{versionId}")
    public ApiResponse<ResumeVersionDetailResponse> getResumeVersionDetail(@PathVariable Long resumeId,
                                                                           @PathVariable Long versionId) {
        return ApiResponse.ok(resumeService.getResumeVersionDetail(resumeId, versionId));
    }

    // 恢复版本
    @PostMapping("/{resumeId}/versions/{versionId}/restore")
    public ApiResponse<ResumeDetailResponse> restoreResume(@PathVariable Long resumeId, @PathVariable Long versionId) {
        return ApiResponse.ok(resumeService.restoreResume(resumeId, versionId));
    }

}
