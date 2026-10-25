package com.elias.interview.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    @Min(1)
    @Max(value = 10_000, message = "页码不能超过 10000")
    private Long pageNum = 1L;

    /**
     * 每页条数
     */
    @NotNull
    @Min(1)
    @Max(100)
    private Long pageSize = 20L;

}
