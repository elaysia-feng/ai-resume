package com.elias.service.resume.impl;

import com.elias.resume.validation.SchemaValidator;
import com.elias.dto.common.response.ResumeSnapshotResponse;
import com.elias.dto.common.response.SectionResponse;
import com.elias.common.context.UserContext;
import com.elias.dto.common.agent.internal.request.AiVersionSaveRequest;
import com.elias.dto.common.request.ResumePatchApplyRequest;
import com.elias.dto.resume.request.ResumePatchPreviewRequest;
import com.elias.dto.common.request.ResumeSectionPatchRequest;
import com.elias.entity.resume.Resume;
import com.elias.entity.resume.ResumeSection;
import com.elias.entity.resume.ResumeVersion;
import com.elias.entity.resume.ResumeVersionSection;
import com.elias.exception.BusinessException;
import com.elias.mapper.resume.ResumeMapper;
import com.elias.mapper.resume.ResumeSectionMapper;
import com.elias.mapper.resume.ResumeVersionMapper;
import com.elias.mapper.resume.ResumeVersionSectionMapper;
import com.elias.service.resume.ResumePatchService;
import com.elias.dto.common.agent.internal.response.AiVersionSaveResponse;
import com.elias.dto.common.response.ResumePatchApplyResponse;
import com.elias.dto.resume.response.ResumePatchPreviewResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
    public ResumeSnapshotResponse getSnapshot(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw BusinessException.notFound("简历不存在");
        }
        List<ResumeSection> sections = resumeSectionMapper.selectList(
                new LambdaQueryWrapper<ResumeSection>()
                        .eq(ResumeSection::getResumeId, resumeId)
                        .orderByAsc(ResumeSection::getSortOrder, ResumeSection::getId)
        );
        return ResumeSnapshotResponse.builder()
                .id(resume.getId())
                .title(resume.getTitle())
                .template(resume.getTemplate())
                .sections(sections.stream().map(this::buildSectionResponse).toList())
                .build();
    }

    private SectionResponse buildSectionResponse(ResumeSection section) {
        return SectionResponse.builder()
                .id(section.getId())
                .resumeId(section.getResumeId())
                .sectionCode(section.getSectionCode())
                .sectionTitle(section.getSectionTitle())
                .sectionType(section.getSectionType())
                .schemaType(section.getSchemaType())
                .contentJson(section.getContentJson())
                .visible(section.getVisible())
                .sortOrder(section.getSortOrder())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .build();
    }

    @Override
    public ResumePatchPreviewResponse previewPatch(Long resumeId, ResumePatchPreviewRequest request) {
        // 1. 先校验简历归属，再批量读取本次建议涉及的模块，避免逐 patch 查库。
        getOwnedResume(resumeId);
        Map<Long, ResumeSection> sectionsById = findOwnedSections(resumeId,
                request.getPatches().stream().map(ResumeSectionPatchRequest::getSectionId).toList());
        List<Map<String, Object>> previews = new ArrayList<>();
        for (ResumeSectionPatchRequest patch : request.getPatches()) {
            // 预览只组装 before/after，不写库，供前端确认前展示。
            ResumeSection section = sectionsById.get(patch.getSectionId());
            if (section == null) {
                throw BusinessException.notFound("简历模块不存在或不属于当前简历");
            }
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

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ResumePatchApplyResponse applyPatch(Long resumeId, ResumePatchApplyRequest request) {
        // 1. 校验每个建议仍基于当前模块内容，避免覆盖生成建议后的人工修改。
        Resume resume = getOwnedResume(resumeId);
        Map<Long, ResumeSection> sectionsById = findOwnedSections(resumeId,
                request.getPatches().stream().map(ResumeSectionPatchRequest::getSectionId).toList());
        int appliedCount = 0;
        for (ResumeSectionPatchRequest patch : request.getPatches()) {
            if (!"REPLACE_SECTION_CONTENT".equalsIgnoreCase(patch.getOperation())) {
                throw BusinessException.badRequest("仅支持 REPLACE_SECTION_CONTENT patch");
            }
            ResumeSection section = sectionsById.get(patch.getSectionId());
            if (section == null) {
                throw BusinessException.notFound("简历模块不存在或不属于当前简历");
            }
            if (patch.getBeforeJson() == null) {
                throw BusinessException.conflict("修改建议缺少原始内容，无法安全应用，请重新生成建议");
            }
            if (!readJson(section.getContentJson()).equals(patch.getBeforeJson())) {
                throw BusinessException.conflict("简历模块在生成建议后已发生变化，请重新生成修改建议");
            }
            String afterJson = writeJson(patch.getAfterJson());
            // 2. 通过当前模块的 schema 后再写入建议内容。
            schemaValidator.validate(section.getSectionCode(), afterJson);
            section.setContentJson(afterJson);
            section.setUpdatedAt(LocalDateTime.now());
            resumeSectionMapper.updateById(section);
            appliedCount++;
        }

        // 3. 更新简历时间并返回实际应用数量。
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

    /** 按简历归属批量读取模块，调用方再按原 patch 顺序校验和处理。 */
    private Map<Long, ResumeSection> findOwnedSections(Long resumeId, List<Long> sectionIds) {
        if (sectionIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ResumeSection> sectionsById = new HashMap<>();
        resumeSectionMapper.selectList(new LambdaQueryWrapper<ResumeSection>()
                        .eq(ResumeSection::getResumeId, resumeId)
                        .in(ResumeSection::getId, sectionIds.stream().distinct().toList()))
                .forEach(section -> sectionsById.put(section.getId(), section));
        return sectionsById;
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
