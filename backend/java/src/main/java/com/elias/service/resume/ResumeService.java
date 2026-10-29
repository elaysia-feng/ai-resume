package com.elias.service.resume;

import com.elias.dto.resume.request.ResumeCreateRequest;
import com.elias.dto.resume.request.ResumeUpdateRequest;
import com.elias.dto.resume.request.ResumeVersionSaveRequest;
import com.elias.dto.resume.request.SectionCreateRequest;
import com.elias.dto.resume.request.SectionUpdateRequest;
import com.elias.dto.resume.request.SectionReorderRequest;
import com.elias.dto.common.response.ResumeDetailResponse;
import com.elias.dto.resume.response.ResumeAvatarUploadResponse;
import com.elias.dto.resume.response.ResumeListResponse;
import com.elias.dto.resume.response.ResumeVersionDetailResponse;
import com.elias.dto.resume.response.ResumeVersionItemResponse;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author 爱门
 */
public interface ResumeService {

    // 创建空白简历并返回简历 ID
    Long createResume(ResumeCreateRequest resumeCreateRequest);
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
