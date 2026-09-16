package com.interviewcoach.dto;

import com.interviewcoach.entity.ResumeAnalysis;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumeSummaryTest {

    @Test
    void summaryExcludesSensitiveAndLargeResumeFields() {
        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setId(12L);
        analysis.setFileName("backend-resume.pdf");
        analysis.setTargetPosition("Java 后端开发");
        analysis.setAiScore(86);
        analysis.setMatchPosition("Java 后端开发");
        analysis.setCreatedAt(LocalDateTime.of(2026, 9, 16, 10, 30));
        analysis.setContent("private resume content");
        analysis.setJobDescription("private job description");
        analysis.setAiFeedback("large AI report");

        ResumeSummary summary = ResumeSummary.from(analysis);
        Set<String> fields = Arrays.stream(ResumeSummary.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of("id", "fileName", "targetPosition", "aiScore", "matchPosition", "createdAt"), fields);
        assertEquals(analysis.getId(), summary.id());
        assertEquals(analysis.getAiScore(), summary.aiScore());
    }
}
