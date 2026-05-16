package com.airesumeforge.dto.interview.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterviewQuestionRoundPageRequest {

    /**
     * 当前页码，从1开始
     */
    private Long pageNum;

    /**
     * 每页条数
     */
    private Long pageSize;

}

