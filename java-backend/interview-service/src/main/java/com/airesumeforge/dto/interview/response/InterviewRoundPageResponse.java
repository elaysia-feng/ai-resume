package com.airesumeforge.dto.interview.response;

import com.airesumeforge.interview.dto.response.InterviewRoundResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 面试轮次分页响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRoundPageResponse {

    /**
     * 当前页数据
     */
    private List<InterviewRoundResponse> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码，从1开始
     */
    private Long pageNum;

    /**
     * 每页条数
     */
    private Long pageSize;
}
