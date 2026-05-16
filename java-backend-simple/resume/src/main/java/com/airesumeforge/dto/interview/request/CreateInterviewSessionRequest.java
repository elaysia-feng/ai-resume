package com.airesumeforge.dto.interview.request;


import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateInterviewSessionRequest {
    private Long resumeId;
    @Nullable
    private String jobDescription;
    private String sceneCode;
}
