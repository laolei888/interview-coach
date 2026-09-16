package com.interviewcoach.dto;

import com.interviewcoach.entity.ResumeAnalysis;

import java.time.LocalDateTime;

public record ResumeSummary(
        Long id,
        String fileName,
        String targetPosition,
        Integer aiScore,
        String matchPosition,
        LocalDateTime createdAt
) {
    public static ResumeSummary from(ResumeAnalysis analysis) {
        return new ResumeSummary(
                analysis.getId(),
                analysis.getFileName(),
                analysis.getTargetPosition(),
                analysis.getAiScore(),
                analysis.getMatchPosition(),
                analysis.getCreatedAt()
        );
    }
}
