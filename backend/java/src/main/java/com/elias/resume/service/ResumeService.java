package com.elias.resume.service;

import com.elias.resume.dto.request.ResumeCreateRequest;
import com.elias.resume.dto.request.ResumeUpdateRequest;
import com.elias.resume.dto.request.ResumeVersionSaveRequest;
import com.elias.resume.dto.request.SectionCreateRequest;
import com.elias.resume.dto.request.SectionUpdateRequest;
import com.elias.resume.dto.request.SectionReorderRequest;
import com.elias.common.dto.response.ResumeDetailResponse;
import com.elias.resume.dto.response.ResumeAvatarUploadResponse;
import com.elias.resume.dto.response.ResumeListResponse;
import com.elias.resume.dto.response.ResumeVersionDetailResponse;
import com.elias.resume.dto.response.ResumeVersionItemResponse;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author 爱门
 */
public interface ResumeService {

    // 创建空白简历
    void createResume(ResumeCreateRequest resumeCreateRequest);
    // 列出当前用户的简历
    List<ResumeListResponse> getResume();

    ResumeDetailResponse getDetailedResume(Long resumeId);
    // 更新简历(也相当于保存简历了)
    void updateResume(Long resumeId, ResumeUpdateRequest resumeUpdateRequest);

    // 删除简历
    void deleteResume(Long resumeId);
    // 添加section
    void addSection(SectionCreateRequest sectionCreateRequest, Long resumeId);
    // 更新section
    void updateSection(SectionUpdateRequest sectionUpdateRequest, Long resumeId, Long sectionId);
    // 删除模块
    void deleteSection(Long resumeId, Long sectionId);
    // 重新排序模块
    void reorderSection(Long resumeId, SectionReorderRequest sectionReorderRequest);
    // 保存简历版本
    void saveResume(@Valid ResumeVersionSaveRequest resumeVersionSaveRequest, Long resumeId);
    // 列出历史版本
    List<ResumeVersionItemResponse> listVersionResume(Long resumeId);
    // 查询版本详情
    ResumeVersionDetailResponse getResumeVersionDetail(Long resumeId, Long versionId);
    // 恢复历史版本
    ResumeDetailResponse restoreResume(Long resumeId, Long versionId);
    // 上传简历头像
    ResumeAvatarUploadResponse uploadResumeAvatar(Long resumeId, MultipartFile file);
}
