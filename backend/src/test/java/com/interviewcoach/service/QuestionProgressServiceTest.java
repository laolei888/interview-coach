package com.interviewcoach.service;

import com.interviewcoach.mapper.QuestionProgressMapper;
import com.interviewcoach.entity.QuestionProgress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionProgressServiceTest {
    @Mock
    private QuestionProgressMapper progressMapper;

    @Mock
    private QuestionService questionService;

    @Test
    void invisibleQuestionCannotCreateProgress() {
        QuestionProgressService service = new QuestionProgressService(questionService);
        ReflectionTestUtils.setField(service, "baseMapper", progressMapper);
        when(questionService.isVisible(9L, 22L)).thenReturn(false);

        assertNull(service.record(22L, 9L, "mastered"));
        verify(progressMapper, never()).insert(org.mockito.ArgumentMatchers.any(QuestionProgress.class));
    }
}
