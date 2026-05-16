package com.airesumeforge.interview.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 爱门
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StartInterviewRunRequest {
    private Long sessionId;
}
