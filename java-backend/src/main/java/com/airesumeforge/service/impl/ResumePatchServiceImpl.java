package com.airesumeforge.service.impl;

import com.airesumeforge.common.SchemaValidator;
import com.airesumeforge.common.UserContext;
import com.airesumeforge.dto.agent.internal.request.AiVersionSaveRequest;
import com.airesumeforge.dto.resume.request.ResumePatchApplyRequest;
import com.airesumeforge.dto.resume.request.ResumePatchPreviewRequest;
import com.airesumeforge.dto.resume.request.ResumeSectionPatchRequest;
import com.airesumeforge.entity.Resume;
import com.airesumeforge.entity.ResumeSection;
import com.airesumeforge.entity.ResumeVersion;
import com.airesumeforge.entity.ResumeVersionSection;
import com.airesumeforge.exception.BusinessException;
import com.airesumeforge.mapper.ResumeMapper;
import com.airesumeforge.mapper.ResumeSectionMapper;
import com.airesumeforge.mapper.ResumeVersionMapper;
import com.airesumeforge.mapper.ResumeVersionSectionMapper;
import com.airesumeforge.service.ResumePatchService;
import com.airesumeforge.dto.agent.internal.response.AiVersionSaveResponse;
import com.airesumeforge.dto.resume.response.ResumePatchApplyResponse;
import com.airesumeforge.dto.resume.response.ResumePatchPreviewResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历 patch 服务实现
 */
@Service
@RequiredArgsConstructor
public class ResumePatchServiceImpl implements ResumePatchService {

    private final ResumeMapper resumeMapper;
    private final ResumeSectionMapper resumeSectionMapper;
    private final ResumeVersionMapper resumeVersionMapper;
    private final ResumeVersionSectionMapper resumeVersionSectionMapper;
    private final ObjectMapper objectMapper;
    private final SchemaValidator schemaValidator;

    @Override
    public ResumePatchPreviewResponse previewPatch(Long resumeId, ResumePatchPreviewRequest request) {
        getOwnedResume(resumeId);
        List<Map<String, Object>> previews = new ArrayList<>();
        for (ResumeSectionPatchRequest patch : request.getPatches()) {
            // 预览只组装 before/after，不写库，供前端确认前展示。
            ResumeSection section = getOwnedSection(resumeId, patch.getSectionId());
            Map<String, Object> preview = new LinkedHashMap<>();
            preview.put("patchId", patch.getPatchId());
            preview.put("sectionId", section.getId());
            preview.put("sectionTitle", section.getSectionTitle());
            preview.put("beforeJson", readJson(section.getContentJson()));
            preview.put("afterJson", patch.getAfterJson());
            preview.put("changeSummary", patch.getChangeSummary());
            previews.add(preview);
        }
        return ResumePatchPreviewResponse.builder().resumeId(resumeId).previews(previews).build();
    }

    @Override
    @Transactional
    public ResumePatchApplyResponse applyPatch(Long resumeId, ResumePatchApplyRequest request) {
        Resume resume = getOwnedResume(resumeId);
        int appliedCount = 0;
        for (ResumeSectionPatchRequest patch : request.getPatches()) {
            if (!"REPLACE_SECTION_CONTENT".equalsIgnoreCase(patch.getOperation())) {
                throw BusinessException.badRequest("仅支持 REPLACE_SECTION_CONTENT patch");
            }
            ResumeSection section = getOwnedSection(resumeId, patch.getSectionId());
            String afterJson = writeJson(patch.getAfterJson());
            // AI 结果写库前必须过当前 sectionCode 的 schema 校验，避免污染简历结构。
            schemaValidator.validate(section.getSectionCode(), afterJson);
            section.setContentJson(afterJson);
            section.setUpdatedAt(LocalDateTime.now());
            resumeSectionMapper.updateById(section);
            appliedCount++;
        }
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);
        return ResumePatchApplyResponse.builder()
                .runId(request.getRunId())
                .resumeId(resumeId)
                .appliedPatchCount(appliedCount)
                .build();
    }

    @Override
    @Transactional
    public AiVersionSaveResponse saveAiVersion(Long resumeId, AiVersionSaveRequest request) {
        Resume resume = getOwnedResume(resumeId);
        // 版本号按当前简历递增；保存的是应用 patch 后的完整简历快照。
        ResumeVersion lastVersion = resumeVersionMapper.selectOne(new LambdaQueryWrapper<ResumeVersion>()
                .eq(ResumeVersion::getResumeId, resumeId)
                .orderByDesc(ResumeVersion::getVersionNo)
                .last("limit 1"));
        int nextVersionNo = lastVersion == null ? 1 : lastVersion.getVersionNo() + 1;
        ResumeVersion version = ResumeVersion.builder()
                .resumeId(resumeId)
                .versionNo(nextVersionNo)
                .versionName(request.getVersionName() == null || request.getVersionName().isBlank() ? "AI v" + nextVersionNo : request.getVersionName())
                .resumeTitle(resume.getTitle())
                .resumeTemplate(resume.getTemplate())
                .source("AI")
                .build();
        resumeVersionMapper.insert(version);

        for (ResumeSection section : listSections(resumeId)) {
            // 版本 section 独立保存一份内容，后续回滚或查看历史不受当前简历继续编辑影响。
            resumeVersionSectionMapper.insert(ResumeVersionSection.builder()
                    .versionId(version.getId())
                    .sectionCode(section.getSectionCode())
                    .sectionTitle(section.getSectionTitle())
                    .sectionType(section.getSectionType())
                    .schemaType(section.getSchemaType())
                    .contentJson(section.getContentJson())
                    .visible(section.getVisible())
                    .sortOrder(section.getSortOrder())
                    .build());
        }
        return AiVersionSaveResponse.builder()
                .versionId(version.getId())
                .versionName(version.getVersionName())
                .build();
    }

    private Resume getOwnedResume(Long resumeId) {
        Long userId = UserContext.verifyGetUserId();
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !userId.equals(resume.getUserId())) {
            throw BusinessException.notFound("简历不存在或无权限访问");
        }
        return resume;
    }

    private ResumeSection getOwnedSection(Long resumeId, Long sectionId) {
        ResumeSection section = resumeSectionMapper.selectById(sectionId);
        if (section == null || !resumeId.equals(section.getResumeId())) {
            throw BusinessException.notFound("简历模块不存在或不属于当前简历");
        }
        return section;
    }

    private List<ResumeSection> listSections(Long resumeId) {
        return resumeSectionMapper.selectList(new LambdaQueryWrapper<ResumeSection>().eq(ResumeSection::getResumeId, resumeId))
                .stream()
                .sorted(Comparator.comparing(ResumeSection::getSortOrder, Comparator.nullsLast(Integer::compareTo)).thenComparing(ResumeSection::getId))
                .toList();
    }

    private Object readJson(String contentJson) {
        try {
            return objectMapper.readValue(contentJson == null || contentJson.isBlank() ? "{}" : contentJson, Object.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("patch afterJson 不是合法 JSON");
        }
    }
}

