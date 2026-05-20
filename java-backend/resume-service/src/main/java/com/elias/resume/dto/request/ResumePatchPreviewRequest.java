package com.elias.resume.dto.request;

import com.elias.common.dto.request.ResumeSectionPatchRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 简历 patch 预览请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumePatchPreviewRequest {

    /**
     * 需要预览的 patch 列表
     */
    @Valid
    private List<ResumeSectionPatchRequest> patches = new ArrayList<>();
}
