package com.interviewcoach.service;

import com.interviewcoach.entity.InterviewSession;
import com.interviewcoach.mapper.InterviewSessionMapper;
import com.interviewcoach.mapper.QuestionMapper;
import com.interviewcoach.mapper.ResumeAnalysisMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataIsolationServiceTest {

    @Mock
    private InterviewSessionMapper sessionMapper;

    @Mock
    private ChatClient chatClient;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private ResumeAnalysisMapper resumeAnalysisMapper;

    @InjectMocks
    private InterviewService interviewService;

    @Test
    void foreignQuestionCannotBeDeleted() {
        QuestionService questionService = new QuestionService(chatClient);
        ReflectionTestUtils.setField(questionService, "baseMapper", questionMapper);
        when(questionMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        assertFalse(questionService.removeOwned(9L, 22L));
        verify(questionMapper).selectOne(any(), anyBoolean());
        verify(questionMapper, never()).deleteById(any(java.io.Serializable.class));
    }

    @Test
    void foreignResumeCannotBeDeleted() {
        ResumeService resumeService = new ResumeService();
        ReflectionTestUtils.setField(resumeService, "baseMapper", resumeAnalysisMapper);
        when(resumeAnalysisMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        assertFalse(resumeService.removeByIdAndUserId(5L, 22L));
        verify(resumeAnalysisMapper).selectOne(any(), anyBoolean());
        verify(resumeAnalysisMapper, never()).deleteById(any(java.io.Serializable.class));
    }

    @Test
    void getSessionScopesLookupToCurrentUser() {
        InterviewSession session = new InterviewSession();
        session.setId(7L);
        session.setUserId(11L);
        when(sessionMapper.selectOne(any())).thenReturn(session);

        InterviewSession result = interviewService.getSession(7L, 11L);

        assertSame(session, result);
        verify(sessionMapper).selectOne(any());
    }

    @Test
    void foreignSessionCannotBeEnded() {
        when(sessionMapper.selectOne(any())).thenReturn(null);

        assertFalse(interviewService.endSession(7L, 22L));

        verify(sessionMapper, never()).updateById(any(InterviewSession.class));
    }

    @Test
    void sessionListIsRequestedThroughMapper() {
        when(sessionMapper.selectList(any())).thenReturn(List.of());

        assertTrue(interviewService.getSessions(22L).isEmpty());

        verify(sessionMapper).selectList(any());
    }

    @Test
    void scoreMarkerIsParsedFromAiResponse() {
        Integer score = ReflectionTestUtils.invokeMethod(interviewService,
                "extractAnswerScore", "回答点评内容\n[评分:8/10]");
        assertEquals(8, score);
    }

    @Test
    void malformedScoreMarkerIsIgnored() {
        Integer score = ReflectionTestUtils.invokeMethod(interviewService,
                "extractAnswerScore", "[评分:12/10]");
        assertNull(score);
    }

    @Test
    void fullWidthScoreMarkerIsParsed() {
        Integer score = ReflectionTestUtils.invokeMethod(interviewService,
                "extractAnswerScore", "简短点评：回答有具体数据。\n【评分：9/10】");
        assertEquals(9, score);
    }
}
