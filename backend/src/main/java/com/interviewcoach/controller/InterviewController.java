package com.interviewcoach.controller;

import com.interviewcoach.dto.Result;
import com.interviewcoach.entity.InterviewSession;
import com.interviewcoach.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import com.interviewcoach.utils.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * AI 模拟面试接口
 * <p>
 * 核心功能：
 * 1. 创建面试会话 → 生成第一个问题
 * 2. SSE 流式对话 → 面试官追问（最大技术亮点）
 * 3. 结束面试 → 生成评估报告
 */
@Slf4j
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private static final int MAX_POSITION_LENGTH = 100;
    private static final int MAX_ANSWER_LENGTH = 4000;

    private final InterviewService interviewService;
    private final RateLimiter rateLimiter;

    /**
     * 开始面试（限流：每分钟 3 次） - 创建会话并返回第一个问题
     * <p>
     * GET /api/interview/start?position=Java后端开发
     */
    @GetMapping("/start")
    public Result<Map<String, Object>> startInterview(@RequestParam(defaultValue = "Java后端开发") String position,
                                                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (position == null || position.isBlank() || position.length() > MAX_POSITION_LENGTH) {
            return Result.badRequest("岗位名称不能为空且不能超过 100 个字符");
        }
        if (!rateLimiter.allow("ai:interview:" + userId, 3, 60)) {
            return Result.fail(429, "操作太频繁，请 1 分钟后再试");
        }
        // 生成第一个问题
        String firstQuestion = interviewService.generateFirstQuestion(position);

        // AI 成功后再创建会话，避免失败时留下无效记录
        InterviewSession session = interviewService.createSession(userId, position);

        // 保存 AI 的第一条消息
        interviewService.saveMessage(session.getId(), userId, "ai", firstQuestion, null);

        log.info("面试开始，会话ID: {}, 岗位: {}", session.getId(), position);

        return Result.ok(Map.of(
                "sessionId", session.getId(),
                "position", position,
                "firstQuestion", firstQuestion
        ));
    }

    /**
     * ⭐ SSE 流式对话 - 核心亮点
     * <p>
     * 前端用 EventSource 或 fetch + ReadableStream 接收
     * 后端用 Spring WebFlux Flux 逐字推送
     * <p>
     * GET /api/interview/chat-stream?sessionId=1&userAnswer=我觉得...
     * <p>
     * 返回格式：text/event-stream
     */
    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam Long sessionId,
                                    @RequestParam String userAnswer,
                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userAnswer == null || userAnswer.isBlank() || userAnswer.length() > MAX_ANSWER_LENGTH) {
            return sseError("回答不能为空且不能超过 4000 个字符");
        }
        if (!rateLimiter.allow("ai:chat:" + userId, 10, 60)) {
            return sseError("操作太频繁，请稍后再试");
        }
        InterviewSession session = interviewService.getSession(sessionId, userId);
        if (session == null) {
            return sseError("面试会话不存在或无权限访问");
        }
        if (!"active".equals(session.getStatus())) {
            return sseError("该面试已经结束，不能继续回答");
        }
        log.info("SSE 流式对话 - sessionId: {}, 回答长度: {}", sessionId, userAnswer.length());
        return interviewService.chatStream(sessionId, userAnswer, userId);
    }

    private Flux<String> sseError(String message) {
        return Flux.just("event: error\ndata: " + message.replace("\n", " ") + "\n\n");
    }

    /**
     * 结束面试 - 生成评估报告
     * <p>
     * POST /api/interview/end?sessionId=1
     */
    @PostMapping("/end")
    public Result<Map<String, Object>> endInterview(@RequestParam Long sessionId,
                                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        InterviewSession current = interviewService.getSession(sessionId, userId);
        if (current == null) {
            return Result.fail(404, "面试会话不存在或无权限访问");
        }
        if (!"active".equals(current.getStatus())) {
            return Result.fail(409, "该面试已经结束，不能重复生成报告");
        }
        if (!interviewService.endSession(sessionId, userId)) {
            return Result.fail(404, "面试会话不存在或无权限访问");
        }

        // 生成总结报告
        String summary = interviewService.generateSummary(sessionId, userId);

        // 保存总结
        interviewService.saveMessage(sessionId, userId, "ai", "【面试总结】\n" + summary, null);

        log.info("面试结束，会话ID: {}", sessionId);

        return Result.ok(Map.of(
                "sessionId", sessionId,
                "summary", summary
        ));
    }

    /**
     * 获取面试历史消息
     */
    @GetMapping("/history/{sessionId}")
    public Result<java.util.List<com.interviewcoach.entity.InterviewMessage>> history(@PathVariable Long sessionId,
                                                                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(interviewService.getHistory(sessionId, userId));
    }

    /**
     * 获取所有面试会话列表
     */
    @GetMapping("/sessions")
    public Result<java.util.List<InterviewSession>> sessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(interviewService.getSessions(userId));
    }

    /**
     * 获取某个会话的完整详情（含消息列表）
     */
    @GetMapping("/session/{id}")
    public Result<Map<String, Object>> sessionDetail(@PathVariable Long id,
                                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var session = interviewService.getSession(id, userId);
        if (session == null) {
            return Result.fail(404, "面试会话不存在或无权限访问");
        }
        var messages = interviewService.getHistory(id, userId);
        return Result.ok(Map.of("session", session, "messages", messages));
    }

    @DeleteMapping("/session/{id}")
    public Result<Boolean> deleteSession(@PathVariable Long id,
                                         @RequestAttribute("userId") Long userId) {
        return interviewService.removeSession(id, userId)
                ? Result.ok(true)
                : Result.fail(404, "面试会话不存在或无权限删除");
    }
}
