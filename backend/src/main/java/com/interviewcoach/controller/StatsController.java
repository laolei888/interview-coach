package com.interviewcoach.controller;

import com.interviewcoach.dto.Result;
import com.interviewcoach.service.InterviewService;
import com.interviewcoach.service.ResumeService;
import com.interviewcoach.service.QuestionService;
import com.interviewcoach.service.QuestionProgressService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * 数据看板统计
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final InterviewService interviewService;
    private final ResumeService resumeService;
    private final QuestionService questionService;
    private final QuestionProgressService questionProgressService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        long interviewCount = interviewService.countSessions(userId);
        long completedCount = interviewService.countCompletedSessions(userId);
        long resumeCount = resumeService.countByUserId(userId);
        long questionCount = questionService.countVisible(userId);
        var sessions = interviewService.getSessions(userId);
        var resumes = resumeService.listByUserId(userId);
        long activeCount = sessions.stream().filter(item -> "active".equals(item.getStatus())).count();
        double averageResumeScore = resumes.stream()
                .filter(item -> item.getAiScore() != null)
                .mapToInt(item -> item.getAiScore())
                .average().orElse(0);

        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("MM/dd");
        Map<LocalDate, Long> sessionsByDay = sessions.stream()
                .filter(item -> item.getCreatedAt() != null)
                .collect(Collectors.groupingBy(item -> item.getCreatedAt().toLocalDate(), Collectors.counting()));
        List<Map<String, Object>> weeklyTrend = java.util.stream.IntStream.rangeClosed(0, 6)
                .mapToObj(offset -> LocalDate.now().minusDays(6L - offset))
                .map(day -> Map.<String, Object>of("date", day.format(dayFormat), "count", sessionsByDay.getOrDefault(day, 0L)))
                .toList();

        Map<String, Long> positionCounts = sessions.stream()
                .collect(Collectors.groupingBy(item -> item.getPosition() == null ? "其他" : item.getPosition(), Collectors.counting()));
        List<Map<String, Object>> positionDistribution = positionCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> Map.<String, Object>of("name", entry.getKey(), "count", entry.getValue()))
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("interviewCount", interviewCount);
        data.put("completedCount", completedCount);
        data.put("activeCount", activeCount);
        data.put("resumeCount", resumeCount);
        data.put("averageResumeScore", Math.round(averageResumeScore));
        data.put("questionCount", questionCount);
        data.put("questionProgress", questionProgressService.stats(userId));
        data.put("weeklyTrend", weeklyTrend);
        data.put("positionDistribution", positionDistribution);
        return Result.ok(data);
    }
}
