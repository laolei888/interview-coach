package com.interviewcoach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewcoach.entity.InterviewMessage;
import com.interviewcoach.entity.InterviewSession;
import com.interviewcoach.mapper.InterviewMessageMapper;
import com.interviewcoach.mapper.InterviewSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试服务 - 管理面试会话、AI 对话、流式返回
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService extends ServiceImpl<InterviewMessageMapper, InterviewMessage> {

    private static final int MAX_CONTEXT_CHARS = 12000;

    private final InterviewSessionMapper sessionMapper;
    private final ChatClient chatClient;
    private final AiGateway aiGateway;

    // ==================== 会话管理 ====================

    /**
     * 创建新的面试会话
     */
    public InterviewSession createSession(Long userId, String position) {
        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setPosition(position);
        session.setStatus("active");
        session.setCreatedAt(LocalDateTime.now());
        sessionMapper.insert(session);
        log.info("创建面试会话，ID: {}, 岗位: {}", session.getId(), position);
        return session;
    }

    /**
     * 获取当前用户的面试会话（按时间倒序）
     */
    public List<InterviewSession> getSessions(Long userId) {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getUserId, userId)
                        .orderByDesc(InterviewSession::getCreatedAt)
        );
    }

    /**
     * 获取当前用户可访问的单个会话
     */
    public InterviewSession getSession(Long id, Long userId) {
        return sessionMapper.selectOne(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getId, id)
                        .eq(InterviewSession::getUserId, userId)
                        .last("LIMIT 1")
        );
    }

    /**
     * 结束面试会话
     */
    public boolean endSession(Long sessionId, Long userId) {
        InterviewSession session = getSession(sessionId, userId);
        if (session == null) {
            return false;
        }
        session.setStatus("completed");
        sessionMapper.updateById(session);
        return true;
    }

    /** 删除当前用户的面试会话及其全部消息。 */
    @org.springframework.transaction.annotation.Transactional
    public boolean removeSession(Long sessionId, Long userId) {
        InterviewSession session = getSession(sessionId, userId);
        if (session == null) return false;
        remove(new LambdaQueryWrapper<InterviewMessage>().eq(InterviewMessage::getSessionId, sessionId));
        return sessionMapper.deleteById(sessionId) > 0;
    }

    /**
     * 统计当前用户面试会话数量
     */
    public long countSessions(Long userId) {
        return sessionMapper.selectCount(new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId));
    }

    /**
     * 统计当前用户已完成面试会话数量
     */
    public long countCompletedSessions(Long userId) {
        return sessionMapper.selectCount(new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId)
                .eq(InterviewSession::getStatus, "completed"));
    }

    // ==================== 消息管理 ====================

    /**
     * 保存对话消息
     */
    public InterviewMessage saveMessage(Long sessionId, Long userId, String role, String content, Integer score) {
        if (getSession(sessionId, userId) == null) {
            throw new IllegalArgumentException("面试会话不存在或无权限访问");
        }
        InterviewMessage msg = new InterviewMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setScore(score);
        msg.setCreatedAt(LocalDateTime.now());
        save(msg);
        return msg;
    }

    /**
     * 获取某个会话的历史消息（按时间排序）
     */
    public List<InterviewMessage> getHistory(Long sessionId, Long userId) {
        if (getSession(sessionId, userId) == null) {
            throw new IllegalArgumentException("面试会话不存在或无权限访问");
        }
        return list(new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByAsc(InterviewMessage::getCreatedAt));
    }

    // ==================== AI 对话 ====================

    /**
     * 非流式：生成面试第一个问题
     */
    public String generateFirstQuestion(String position) {
        String prompt = """
                你是资深 %s 面试官，正在进行一场真实的技术面试。
                请直接提出第一个面试问题，不需要打招呼和自我介绍。
                问题要有一定深度，考察候选人的实际能力。
                只输出一个自然段，不使用 Markdown 标题、列表、引用或代码块。
                """.formatted(position);

        return aiGateway.complete("interview-first-question", prompt);
    }

    /**
     * 非流式：生成面试总结报告
     */
    public String generateSummary(Long sessionId, Long userId) {
        List<InterviewMessage> history = getHistory(sessionId, userId);

        StringBuilder conversation = new StringBuilder();
        for (InterviewMessage msg : history) {
            String roleTag = "ai".equals(msg.getRole()) ? "面试官" : "候选人";
            conversation.append(roleTag).append(": ").append(msg.getContent()).append("\n");
        }

        String prompt = """
                你是一位资深技术面试官。请根据以下面试对话，做综合分析：

                %s

                请严格按以下 Markdown 结构输出，不要省略维度：

                ## 综合评分：X/10
                用 2-3 句话说明总体表现。

                ## 能力雷达
                - 技术基础：X/10 - 一句话依据
                - 问题分析：X/10 - 一句话依据
                - 项目实践：X/10 - 一句话依据
                - 表达沟通：X/10 - 一句话依据
                - 岗位匹配：X/10 - 一句话依据

                ## 表现亮点
                - 列出 3 条，并引用对话中的具体表现

                ## 关键短板
                - 列出 3 条，说明短板可能造成的面试风险

                ## 下一步训练计划
                - 按优先级列出 3-5 条可执行任务

                ## 一句话结论
                给出客观、简洁的录用倾向判断。
                """.formatted(conversation.toString());

        return aiGateway.complete("interview-summary", prompt);
    }

    /**
     * ⭐ 流式：AI 追问对话（SSE 推送）
     * <p>
     * 这是简历上最大的技术亮点
     */
    public Flux<String> chatStream(Long sessionId, String userAnswer, Long userId) {
        InterviewSession session = getSession(sessionId, userId);
        if (session == null) {
            return Flux.just("event: error\ndata: 面试会话不存在或无权限访问\n\n");
        }
        if (!"active".equals(session.getStatus())) {
            return Flux.just("event: error\ndata: 该面试已经结束，不能继续回答\n\n");
        }

        // 保存用户消息
        InterviewMessage userMessage = saveMessage(sessionId, userId, "user", userAnswer, null);

        // 获取历史记录，构建上下文
        List<InterviewMessage> history = getHistory(sessionId, userId);
        StringBuilder context = new StringBuilder();
        for (InterviewMessage msg : history) {
            String roleTag = "ai".equals(msg.getRole()) ? "面试官" : "候选人";
            context.append(roleTag).append(": ").append(msg.getContent()).append("\n");
        }
        if (context.length() > MAX_CONTEXT_CHARS) {
            context = new StringBuilder(context.substring(context.length() - MAX_CONTEXT_CHARS));
            log.info("面试上下文已截断，sessionId={}, chars={}", sessionId, MAX_CONTEXT_CHARS);
        }

        String systemPrompt = """
                你是一位资深技术面试官，正在进行真实的技术面试。

                以下是对话历史：
                %s

                规则：
                1. 回复固定为两部分：“简短点评：”和“下一问：”，每部分只写一个自然段
                2. 简短点评控制在 1-2 句话，指出最新回答中最具体的一项优点或缺口
                3. 下一问只提出一个问题；回答充分时深入追问，回答模糊时要求补充事实或数据
                4. 不使用 Markdown 标题、列表、表格、引用、代码块或反引号，不重复候选人的整段回答
                5. 使用统一、克制、专业的中文表达，不使用 emoji，不写寒暄或总结报告
                6. 在回复最后另起一行输出机器标记 [评分:X/10]，X 为最新回答的 1-10 分整数评分
                """.formatted(context.toString());

        // 流式调用 AI，同时收集 token 用于最后持久化
        java.util.List<String> allTokens = new java.util.ArrayList<>();

        return chatClient.prompt()
                .system(systemPrompt)
                .user("请根据候选人的最新回答进行追问或点评")
                .stream()
                .content()
                .doOnNext(allTokens::add)
                .doOnComplete(() -> {
                    // 流结束后把 AI 完整回复存到数据库
                    String fullResponse = String.join("", allTokens);
                    if (fullResponse.isBlank()) {
                        log.warn("AI 流式回复为空，sessionId: {}", sessionId);
                        return;
                    }
                    Integer score = extractAnswerScore(fullResponse);
                    if (score != null) {
                        userMessage.setScore(score);
                        updateById(userMessage);
                    }
                    saveMessage(sessionId, userId, "ai", fullResponse, null);
                    log.info("AI 流式回复已保存，sessionId: {}, 长度: {}, score: {}", sessionId, fullResponse.length(), score);
                })
                .doOnError(error -> log.error("AI 流式调用失败，sessionId: {}", sessionId, error))
                .doOnCancel(() -> log.info("AI 流式调用被客户端取消，sessionId: {}", sessionId));
    }

    private Integer extractAnswerScore(String response) {
        var matcher = java.util.regex.Pattern.compile("[\\[【]评分[：:]?\\s*(10|[1-9])\\s*/\\s*10[\\]】]", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(response);
        Integer score = null;
        while (matcher.find()) score = Integer.parseInt(matcher.group(1));
        return score;
    }
}
