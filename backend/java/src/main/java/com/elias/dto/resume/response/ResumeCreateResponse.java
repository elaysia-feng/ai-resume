package com.elias.dto.resume.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 简历创建成功后返回的标识。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeCreateResponse {

    /** 创建接口的规范简历标识字段。 */
    private Long resumeId;

    /** 兼容旧前端读取的标识字段；新调用方请使用 resumeId。 */
    private Long id;
}
