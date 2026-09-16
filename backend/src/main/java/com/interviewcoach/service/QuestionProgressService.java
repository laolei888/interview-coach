package com.interviewcoach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewcoach.entity.QuestionProgress;
import com.interviewcoach.mapper.QuestionProgressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionProgressService extends ServiceImpl<QuestionProgressMapper, QuestionProgress> {
    private final QuestionService questionService;

    public QuestionProgress record(Long userId, Long questionId, String status) {
        if (!questionService.isVisible(questionId, userId)) return null;
        QuestionProgress progress = getOne(new LambdaQueryWrapper<QuestionProgress>()
                .eq(QuestionProgress::getUserId, userId)
                .eq(QuestionProgress::getQuestionId, questionId)
                .last("LIMIT 1"));
        if (progress == null) {
            progress = new QuestionProgress();
            progress.setUserId(userId);
            progress.setQuestionId(questionId);
            progress.setPracticeCount(1);
        } else {
            progress.setPracticeCount(progress.getPracticeCount() + 1);
        }
        progress.setStatus(status);
        progress.setLastPracticedAt(LocalDateTime.now());
        saveOrUpdate(progress);
        return progress;
    }

    public List<QuestionProgress> listByUser(Long userId) {
        return list(new LambdaQueryWrapper<QuestionProgress>().eq(QuestionProgress::getUserId, userId));
    }

    public Map<String, Long> stats(Long userId) {
        List<QuestionProgress> rows = listByUser(userId);
        return Map.of(
                "practiced", (long) rows.size(),
                "mastered", rows.stream().filter(row -> "mastered".equals(row.getStatus())).count(),
                "review", rows.stream().filter(row -> "review".equals(row.getStatus())).count()
        );
    }
}
