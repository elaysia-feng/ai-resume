package com.airesumeforge.dto.resume.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历模块排序请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionReorderRequest {

    /**
     * 按期望顺序排列的 sectionId 列表
     */
    @NotEmpty(message = "sectionIds 不能为空")
    private List<Long> sectionIds;
}

