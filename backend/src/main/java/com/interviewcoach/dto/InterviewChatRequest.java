package com.interviewcoach.dto;

/**
 * 模拟面试流式追问请求。
 *
 * 将候选人的回答放在请求体中，避免把完整回答暴露在 URL、访问日志或浏览器历史里。
 */
public record InterviewChatRequest(Long sessionId, String userAnswer) {
}
