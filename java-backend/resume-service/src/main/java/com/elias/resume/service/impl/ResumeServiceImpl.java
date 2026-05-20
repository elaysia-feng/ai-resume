package com.elias.resume.service.impl;

import com.elias.resume.common.SectionCode;
import com.elias.common.OssProperties;
import com.elias.common.context.UserContext;
import com.elias.resume.dto.request.ResumeCreateRequest;
import com.elias.resume.dto.request.ResumeUpdateRequest;
import com.elias.resume.dto.request.ResumeVersionSaveRequest;
import com.elias.resume.dto.request.SectionCreateRequest;
import com.elias.resume.dto.request.SectionUpdateRequest;
import com.elias.resume.dto.request.SectionReorderRequest;

import com.elias.resume.entity.Resume;
import com.elias.resume.entity.ResumeSection;
import com.elias.resume.entity.ResumeVersion;
import com.elias.resume.entity.ResumeVersionSection;
import com.elias.common.exception.BusinessException;
import com.elias.resume.mapper.ResumeMapper;
import com.elias.resume.mapper.ResumeSectionMapper;
import com.elias.resume.mapper.ResumeVersionMapper;
import com.elias.resume.mapper.ResumeVersionSectionMapper;
import com.elias.resume.service.ResumeService;
import com.elias.common.SchemaValidator;
import com.elias.resume.dto.response.ResumeAvatarUploadResponse;
import com.elias.common.dto.response.ResumeDetailResponse;
import com.elias.resume.dto.response.ResumeListResponse;
import com.elias.resume.dto.response.ResumeVersionDetailResponse;
import com.elias.resume.dto.response.ResumeVersionItemResponse;
import com.elias.resume.dto.response.ResumeVersionSectionResponse;
import com.elias.common.dto.response.SectionResponse;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * @author 爱门
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private static final long MAX_RESUME_AVATAR_SIZE = 100 * 1024 * 1024L;

    private final ResumeMapper resumeMapper;
    private final ResumeSectionMapper resumeSectionMapper;
    private final ObjectMapper objectMapper;
    private final ResumeVersionMapper resumeVersionMapper;
    private final ResumeVersionSectionMapper resumeVersionSectionMapper;
    private final OSS ossClient;
    private final OssProperties ossProperties;
    private final SchemaValidator schemaValidator;

    /**
     * 创建空白简历
     * @param resumeCreateRequest
     */
    @Override
    @Transactional
    public void createResume(ResumeCreateRequest resumeCreateRequest) {
        Long userId = UserContext.verifyGetUserId();
        Long count = resumeMapper.selectCount(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, userId)
        );

        if (count >= 6) {
            throw BusinessException.business("最多只能创建 6 份简历");
        }

        Resume resume = Resume.builder()
                .userId(userId)
                .title(resumeCreateRequest.getTitle() != null ? resumeCreateRequest.getTitle() : "Untitled Resume")
                .template(resumeCreateRequest.getTemplate() != null ? resumeCreateRequest.getTemplate() : "classic")
                .build();
        resumeMapper.insert(resume);
    }

    /**
     * 获取当前用户所有简历
     * @return
     */
    @Override
    public List<ResumeListResponse> getResume() {
        Long userId = UserContext.verifyGetUserId();
        List<Resume> userResumes = resumeMapper.selectList(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId));

        return userResumes.stream()
                .map(resume -> new ResumeListResponse(
                        resume.getId(),
                        resume.getTitle(),
                        resume.getTemplate(),
                        resume.getUpdatedAt()
                )).toList();
    }

    /**
     * 查询简历的详细信息
     * @param resumeId
     * @return
     */
    @Override
    public ResumeDetailResponse getDetailedResume(Long resumeId) {
        Long userId = UserContext.verifyGetUserId();

        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw BusinessException.notFound("简历不存在或无权限访问");
        }

        List<ResumeSection> resumeSections = resumeSectionMapper.selectList(
                new LambdaQueryWrapper<ResumeSection>()
                        .eq(ResumeSection::getResumeId, resumeId)
                        .orderByAsc(ResumeSection::getSortOrder)
        );

        List<SectionResponse> sectionResponses = resumeSections.stream()
                .map(resumeSection -> SectionResponse.builder()
                        .id(resumeSection.getId())
                        .resumeId(resumeSection.getResumeId())
                        .sectionCode(resumeSection.getSectionCode())
                        .sectionTitle(resumeSection.getSectionTitle())
                        .sectionType(resumeSection.getSectionType())
                        .schemaType(resumeSection.getSchemaType())
                        .contentJson(resumeSection.getContentJson())
                        .visible(resumeSection.getVisible())
                        .sortOrder(resumeSection.getSortOrder())
                        .createdAt(resumeSection.getCreatedAt())
                        .updatedAt(resumeSection.getUpdatedAt())
                        .build())
                .toList();

        return ResumeDetailResponse.builder()
                .id(resume.getId())
                .userId(String.valueOf(resume.getUserId()))
                .title(resume.getTitle())
                .template(resume.getTemplate())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .sections(sectionResponses)
                .build();
    }

    /**
     * 更新简历(也相当于保存简历)
     * @param resumeId
     * @param resumeUpdateRequest
     */
    @Override
    @Transactional
    public void updateResume(Long resumeId, ResumeUpdateRequest resumeUpdateRequest) {
        Resume resume = getOwnedResume(resumeId);

        if (resumeUpdateRequest.getTitle() != null) {
            resume.setTitle(resumeUpdateRequest.getTitle());
        }
        if (resumeUpdateRequest.getTemplate() != null) {
            resume.setTemplate(resumeUpdateRequest.getTemplate());
        }

        updateResumeSections(resumeId, resumeUpdateRequest.getSections());
        resumeMapper.updateById(resume);
    }

    /**
     * 删除简历
     * @param resumeId
     */
    @Override
    @Transactional
    public void deleteResume(Long resumeId) {
        Long userId = UserContext.verifyGetUserId();
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw BusinessException.notFound("简历不存在或无权限访问");
        }
        resumeMapper.deleteById(resumeId);
    }

    /**
     * 添加简历模块
     * @param sectionCreateRequest
     * @param resumeId
     */
    @Override
    @Transactional
    public void addSection(SectionCreateRequest sectionCreateRequest, Long resumeId) {
        Resume resume = getOwnedResume(resumeId);
        String sectionCode = sectionCreateRequest.getSectionCode();

        if (SectionCode.isSystem(sectionCode) && hasSystemSection(resumeId, sectionCode)) {
            throw BusinessException.conflict("系统模块已存在，不能重复添加");
        }

        String contentJson = sectionCreateRequest.getContentJson() != null ? sectionCreateRequest.getContentJson() : "{}";
        try {
            schemaValidator.validate(sectionCode, contentJson);
        } catch (BusinessException e) {
            throw BusinessException.business("Schema校验失败: " + e.getMessage());
        }

        ResumeSection resumeSection = ResumeSection.builder()
                .resumeId(resumeId)
                .sectionCode(sectionCode)
                .sectionTitle(sectionCreateRequest.getSectionTitle())
                .sectionType(SectionCode.isSystem(sectionCode) ? "SYSTEM" : "CUSTOM")
                .schemaType(sectionCreateRequest.getSchemaType())
                .contentJson(contentJson)
                .visible(sectionCreateRequest.getVisible() != null ? sectionCreateRequest.getVisible() : true)
                .sortOrder(getNextSortOrder(resumeId))
                .build();

        resumeSectionMapper.insert(resumeSection);
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);
    }

    /**
     * 跟新section
     * @param sectionUpdateRequest
     * @param resumeId
     * @param sectionId
     */
    @Override
    @Transactional
    public void updateSection(SectionUpdateRequest sectionUpdateRequest, Long resumeId, Long sectionId) {
        Resume resume = getOwnedResume(resumeId);
        ResumeSection resumeSection = resumeSectionMapper.selectById(sectionId);
        if (resumeSection == null || !resumeSection.getResumeId().equals(resumeId)) {
            throw BusinessException.notFound("简历不存在或无权限访问");
        }
        if (sectionUpdateRequest.getSectionTitle() != null) {
            resumeSection.setSectionTitle(sectionUpdateRequest.getSectionTitle());
        }
        if (sectionUpdateRequest.getContentJson() != null) {
            String contentJson = sectionUpdateRequest.getContentJson();
            try {
                schemaValidator.validate(resumeSection.getSectionCode(), contentJson);
            } catch (BusinessException e) {
                throw BusinessException.business("Schema校验失败: " + e.getMessage());
            }
            resumeSection.setContentJson(contentJson);
        }
        if (sectionUpdateRequest.getVisible() != null) {
            resumeSection.setVisible(sectionUpdateRequest.getVisible());
        }
        resumeSection.setUpdatedAt(LocalDateTime.now());
        resumeSectionMapper.updateById(resumeSection);
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);
    }

    /**
     * 删除模块
     * @param resumeId
     * @param sectionId
     */
    @Override
    @Transactional
    public void deleteSection(Long resumeId, Long sectionId) {
        Resume resume = getOwnedResume(resumeId);
        ResumeSection resumeSection = resumeSectionMapper.selectById(sectionId);
        if (resumeSection == null || !resumeSection.getResumeId().equals(resumeId)) {
            throw BusinessException.notFound("简历不存在或无权限访问");
        }
        if ("SYSTEM".equals(resumeSection.getSectionType())) {
            throw BusinessException.business("系统模块禁止删除");
        }
        resumeSectionMapper.deleteById(resumeSection.getId());
        resequenceSections(resumeId);
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);
    }

    /**
     * 重新排序模块
     * @param resumeId
     * @param sectionReorderRequest
     */
    @Override
    @Transactional
    public void reorderSection(Long resumeId, SectionReorderRequest sectionReorderRequest) {
        Resume resume = getOwnedResume(resumeId);
        List<Long> sectionIds = sectionReorderRequest.getSectionIds();
        for (int i = 0; i < sectionIds.size(); i++) {
            Long sectionId = sectionIds.get(i);
            ResumeSection section = resumeSectionMapper.selectById(sectionId);
            if (section == null || !resumeId.equals(section.getResumeId())) {
                throw BusinessException.business("部分模块不属于当前简历，排序失败");
            }
            section.setSortOrder(i + 1);
            resumeSectionMapper.updateById(section);
        }
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);
    }

    /**
     * 保存简历版本
     *
     * @param resumeVersionSaveRequest
     * @param resumeId
     */
    @Override
    @Transactional
    public void saveResume(@Valid ResumeVersionSaveRequest resumeVersionSaveRequest, Long resumeId) {
        Resume resume = getOwnedResume(resumeId);
        ResumeVersion lastVersion = resumeVersionMapper.selectOne(new LambdaQueryWrapper<ResumeVersion>()
                .eq(ResumeVersion::getResumeId, resumeId)
                .orderByDesc(ResumeVersion::getVersionNo)
                .last("limit 1"));

        int nextVersionNo = lastVersion == null ? 1 : lastVersion.getVersionNo() + 1;
        ResumeVersion resumeVersion = ResumeVersion.builder()
                .resumeId(resumeId)
                .versionName(resolveVersionName(resumeVersionSaveRequest.getVersionName(), nextVersionNo))
                .versionNo(nextVersionNo)
                .resumeTitle(resume.getTitle())
                .resumeTemplate(resume.getTemplate())
                .source(resolveSource(resumeVersionSaveRequest.getSource()))
                .build();
        resumeVersionMapper.insert(resumeVersion);

        List<ResumeSection> resumeSections = resumeSectionMapper.selectList(new LambdaQueryWrapper<ResumeSection>()
                .eq(ResumeSection::getResumeId, resumeId)
                .orderByAsc(ResumeSection::getSortOrder, ResumeSection::getId));
        for (ResumeSection resumeSection : resumeSections) {
            ResumeVersionSection resumeVersionSection = ResumeVersionSection.builder()
                    .versionId(resumeVersion.getId())
                    .sectionCode(resumeSection.getSectionCode())
                    .sectionTitle(resumeSection.getSectionTitle())
                    .sectionType(resumeSection.getSectionType())
                    .schemaType(resumeSection.getSchemaType())
                    .contentJson(resumeSection.getContentJson())
                    .visible(resumeSection.getVisible())
                    .sortOrder(resumeSection.getSortOrder())
                    .build();
            resumeVersionSectionMapper.insert(resumeVersionSection);
        }

        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);
    }

    /**
     * 列出历史版本
     * @param resumeId
     * @return
     */
    @Override
    public List<ResumeVersionItemResponse> listVersionResume(Long resumeId) {
        getOwnedResume(resumeId);
        List<ResumeVersion> resumeVersions = resumeVersionMapper.selectList(new LambdaQueryWrapper<ResumeVersion>()
                .eq(ResumeVersion::getResumeId, resumeId)
                .orderByDesc(ResumeVersion::getVersionNo, ResumeVersion::getCreatedAt));
        List<ResumeVersionItemResponse> resumeVersionItemResponseList = new ArrayList<ResumeVersionItemResponse>();


        for (ResumeVersion resumeVersion : resumeVersions) {
            resumeVersionItemResponseList.add(ResumeVersionItemResponse.builder()
                    .id(resumeVersion.getId())
                    .versionName(resumeVersion.getVersionName())
                    .versionNo(resumeVersion.getVersionNo())
                    .source(resumeVersion.getSource())
                    .createdAt(resumeVersion.getCreatedAt())
                    .build());
        }

        return resumeVersionItemResponseList;
    }

    /**
     * 查询版本详情
     *
     * @param resumeId
     * @param versionId
     * @return
     */
    @Override
    public ResumeVersionDetailResponse getResumeVersionDetail(Long resumeId, Long versionId) {
        getOwnedResume(resumeId);
        ResumeVersion resumeVersion = getOwnedResumeVersion(resumeId, versionId);
        List<ResumeVersionSection> versionSections = listResumeVersionSections(versionId);

        return new ResumeVersionDetailResponse(
                resumeVersion.getId(),
                resumeVersion.getResumeId(),
                resumeVersion.getVersionNo(),
                resumeVersion.getVersionName(),
                resumeVersion.getResumeTitle(),
                resumeVersion.getResumeTemplate(),
                resumeVersion.getSource(),
                resumeVersion.getCreatedAt(),
                versionSections.stream().map(this::buildResumeVersionSectionResponse).toList()
        );
    }

    /**
     * 恢复历史版本
     *
     * @param resumeId
     * @param versionId
     * @return
     */
    @Override
    @Transactional
    public ResumeDetailResponse restoreResume(Long resumeId, Long versionId) {
        Resume resume = getOwnedResume(resumeId);
        ResumeVersion resumeVersion = getOwnedResumeVersion(resumeId, versionId);
        List<ResumeVersionSection> versionSections = listResumeVersionSections(versionId);

        if (versionSections.isEmpty()) {
            throw BusinessException.business("该版本没有可恢复的模块内容");
        }

        resume.setTitle(resumeVersion.getResumeTitle());
        resume.setTemplate(resumeVersion.getResumeTemplate());
        resumeSectionMapper.delete(new LambdaQueryWrapper<ResumeSection>()
                .eq(ResumeSection::getResumeId, resumeId));

        for (ResumeVersionSection versionSection : versionSections) {
            ResumeSection resumeSection = ResumeSection.builder()
                    .resumeId(resumeId)
                    .sectionCode(versionSection.getSectionCode())
                    .sectionTitle(versionSection.getSectionTitle())
                    .sectionType(versionSection.getSectionType())
                    .schemaType(versionSection.getSchemaType())
                    .contentJson(versionSection.getContentJson())
                    .visible(versionSection.getVisible())
                    .sortOrder(versionSection.getSortOrder())
                    .build();
            resumeSectionMapper.insert(resumeSection);
        }

        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);

        return getDetailedResume(resumeId);
    }

    /**
     * 上传简历头像到OSS
     * @param resumeId
     * @param file
     * @return
     */
    @Override
    public ResumeAvatarUploadResponse uploadResumeAvatar(Long resumeId, MultipartFile file) {
        getOwnedResume(resumeId);

        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("头像文件不能为空");
        }

        if (file.getSize() > MAX_RESUME_AVATAR_SIZE) {
            throw BusinessException.badRequest("头像文件不能超过 100MB");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw BusinessException.badRequest("头像仅支持图片文件");
        }

        if (!StringUtils.hasText(ossProperties.getBucketName())
                || !StringUtils.hasText(ossProperties.getDomain())
                || !StringUtils.hasText(ossProperties.getResumeAvatarDir())) {
            throw BusinessException.business("OSS配置不完整，请检查 oss.bucket-name / oss.domain / oss.resume-avatar-dir");
        }

        String resumeAvatarDir = ossProperties.getResumeAvatarDir().replace("\\", "/");
        if (resumeAvatarDir.endsWith("/")) {
            resumeAvatarDir = resumeAvatarDir.substring(0, resumeAvatarDir.length() - 1);
        }

        String objectName = resumeAvatarDir + "/" + resumeId + "/" + UUID.randomUUID() + resolveFileSuffix(file);
        String domain = ossProperties.getDomain().trim();
        if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
            domain = "https://" + domain;
        }
        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }

        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(contentType);

            ossClient.putObject(ossProperties.getBucketName(), objectName, inputStream, metadata);
        } catch (OSSException | ClientException e) {
            log.error("[简历头像上传] OSS上传失败, resumeId={}, message={}", resumeId, e.getMessage(), e);
            throw BusinessException.business("上传简历头像失败，请稍后重试");
        } catch (IOException e) {
            log.error("[简历头像上传] 读取头像文件失败, resumeId={}, message={}", resumeId, e.getMessage(), e);
            throw BusinessException.business("读取头像文件失败");
        }

        return ResumeAvatarUploadResponse.builder()
                .avatarUrl(domain + "/" + objectName)
                .build();
    }

    private Resume getOwnedResume(Long resumeId) {
        Long userId = UserContext.verifyGetUserId();
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw BusinessException.notFound("简历不存在或无权限访问");
        }
        return resume;
    }

    private boolean hasSystemSection(Long resumeId, String sectionCode) {
        return resumeSectionMapper.selectCount(new LambdaQueryWrapper<ResumeSection>()
                .eq(ResumeSection::getResumeId, resumeId)
                .eq(ResumeSection::getSectionCode, sectionCode)) > 0;
    }

    private int getNextSortOrder(Long resumeId) {
        return listResumeSections(resumeId).stream()
                .map(ResumeSection::getSortOrder)
                .filter(sortOrder -> sortOrder != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private void resequenceSections(Long resumeId) {
        List<ResumeSection> sections = listResumeSections(resumeId);
        for (int i = 0; i < sections.size(); i++) {
            ResumeSection section = sections.get(i);
            int expectedSortOrder = i + 1;
            if (!Integer.valueOf(expectedSortOrder).equals(section.getSortOrder())) {
                section.setSortOrder(expectedSortOrder);
                section.setUpdatedAt(LocalDateTime.now());
                resumeSectionMapper.updateById(section);
            }
        }
    }

    private List<ResumeSection> listResumeSections(Long resumeId) {
        return resumeSectionMapper.selectList(new LambdaQueryWrapper<ResumeSection>()
                .eq(ResumeSection::getResumeId, resumeId))
                .stream()
                .sorted(Comparator.comparing(ResumeSection::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ResumeSection::getId))
                .toList();
    }
    // 跟新简历的模块
    private void updateResumeSections(Long resumeId, Map<Long, JsonNode> sections){
        if (sections == null || sections.isEmpty()) {
            return;
        }


        for (Map.Entry<Long, JsonNode> entry : sections.entrySet()) {
            Long sectionId = entry.getKey();
            JsonNode sectionContent = entry.getValue();

            ResumeSection section = resumeSectionMapper.selectById(sectionId);
            if (section == null || !section.getResumeId().equals(resumeId)) {
                throw BusinessException.business("部分模块不属于当前简历, 保存失败");
            }

            section.setContentJson(writeJson(sectionContent));
            section.setUpdatedAt(LocalDateTime.now());
            resumeSectionMapper.updateById(section);
        }
    }

    // Json反序列化
    private String writeJson(JsonNode jsonNode) {
        try {
            return objectMapper.writeValueAsString(jsonNode);
        }
        catch (JsonProcessingException e) {
            throw BusinessException.business("模快内容格式不合法, 保存失败");
        }
     }

    private ResumeVersion getOwnedResumeVersion(Long resumeId, Long versionId) {
        ResumeVersion resumeVersion = resumeVersionMapper.selectById(versionId);
        if (resumeVersion == null || !resumeVersion.getResumeId().equals(resumeId)) {
            throw BusinessException.notFound("简历版本不存在或无权限访问");
        }
        return resumeVersion;
    }

    private List<ResumeVersionSection> listResumeVersionSections(Long versionId) {
        return resumeVersionSectionMapper.selectList(
                new LambdaQueryWrapper<ResumeVersionSection>()
                        .eq(ResumeVersionSection::getVersionId, versionId)
                        .orderByAsc(ResumeVersionSection::getSortOrder, ResumeVersionSection::getId)
        );
    }

    private ResumeVersionSectionResponse buildResumeVersionSectionResponse(ResumeVersionSection versionSection) {
        return ResumeVersionSectionResponse.builder()
                .id(versionSection.getId())
                .versionId(versionSection.getVersionId())
                .sectionCode(versionSection.getSectionCode())
                .sectionTitle(versionSection.getSectionTitle())
                .sectionType(versionSection.getSectionType())
                .schemaType(versionSection.getSchemaType())
                .contentJson(versionSection.getContentJson())
                .visible(versionSection.getVisible())
                .sortOrder(versionSection.getSortOrder())
                .createdAt(versionSection.getCreatedAt())
                .build();
    }

    private String resolveVersionName(String versionName, int versionNo) {
        if (versionName == null || versionName.isBlank()) {
            return "v" + versionNo;
        }
        return versionName;
    }

    private String resolveSource(String source) {
        if (source == null || source.isBlank()) {
            return "MANUAL";
        }
        return source;
    }

    private String resolveFileSuffix(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        }

        String contentType = file.getContentType();
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        if ("image/gif".equalsIgnoreCase(contentType)) {
            return ".gif";
        }
        return ".jpg";
    }
}
