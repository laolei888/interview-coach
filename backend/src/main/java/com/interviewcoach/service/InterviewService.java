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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private static final Map<String, String> MODE_LABELS = Map.of(
            "technical", "技术深挖",
            "behavioral", "行为面试",
            "system-design", "系统设计",
            "project", "项目复盘"
    );
    private static final Map<String, String> DIFFICULTY_LABELS = Map.of(
            "junior", "初级",
            "intermediate", "中级",
            "senior", "高级"
    );
    private static final Map<String, String> FEEDBACK_STYLE_LABELS = Map.of(
            "encouraging", "鼓励型",
            "balanced", "平衡型",
            "strict", "严格型"
    );

    // ==================== 会话管理 ====================

    /**
     * 创建新的面试会话
     */
    public InterviewSession createSession(Long userId, String position) {
        return createSession(userId, position, "technical", "intermediate", 5);
    }

    public InterviewSession createSession(Long userId, String position, String interviewMode, String difficultyLevel, Integer targetRounds) {
        return createSession(userId, position, interviewMode, difficultyLevel, targetRounds, "balanced");
    }

    public InterviewSession createSession(Long userId, String position, String interviewMode, String difficultyLevel, Integer targetRounds, String feedbackStyle) {
        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setPosition(position);
        session.setInterviewMode(normalizeMode(interviewMode));
        session.setDifficultyLevel(normalizeDifficulty(difficultyLevel));
        session.setTargetRounds(normalizeTargetRounds(targetRounds));
        session.setFeedbackStyle(normalizeFeedbackStyle(feedbackStyle));
        session.setStatus("active");
        session.setCreatedAt(LocalDateTime.now());
        sessionMapper.insert(session);
        log.info("创建面试会话，ID: {}, 岗位: {}, 模式: {}, 难度: {}, 反馈风格: {}", session.getId(), position, session.getInterviewMode(), session.getDifficultyLevel(), session.getFeedbackStyle());
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

    public Map<String, Object> scoreAnalytics(Long userId) {
        List<InterviewSession> sessions = getSessions(userId);
        if (sessions.isEmpty()) {
            return emptyScoreAnalytics();
        }
        Map<Long, InterviewSession> sessionById = sessions.stream()
                .collect(Collectors.toMap(InterviewSession::getId, item -> item));
        List<Long> sessionIds = new ArrayList<>(sessionById.keySet());
        List<InterviewMessage> scoredMessages = list(new LambdaQueryWrapper<InterviewMessage>()
                .in(InterviewMessage::getSessionId, sessionIds)
                .eq(InterviewMessage::getRole, "user")
                .isNotNull(InterviewMessage::getScore)
                .orderByAsc(InterviewMessage::getCreatedAt));

        if (scoredMessages.isEmpty()) {
            return emptyScoreAnalytics();
        }

        double average = scoredMessages.stream().mapToInt(InterviewMessage::getScore).average().orElse(0);
        int highest = scoredMessages.stream().map(InterviewMessage::getScore).filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
        int lowest = scoredMessages.stream().map(InterviewMessage::getScore).filter(Objects::nonNull).min(Integer::compareTo).orElse(0);

        List<Map<String, Object>> scoreTrend = scoredMessages.stream()
                .skip(Math.max(0, scoredMessages.size() - 8))
                .map(message -> {
                    InterviewSession session = sessionById.get(message.getSessionId());
                    return Map.<String, Object>of(
                            "score", message.getScore(),
                            "position", session == null ? "未知岗位" : session.getPosition(),
                            "mode", modeLabel(session == null ? null : session.getInterviewMode()),
                            "createdAt", message.getCreatedAt()
                    );
                })
                .toList();

        List<Map<String, Object>> modeScores = scoredMessages.stream()
                .collect(Collectors.groupingBy(
                        message -> modeLabel(sessionById.get(message.getSessionId()) == null ? null : sessionById.get(message.getSessionId()).getInterviewMode()),
                        LinkedHashMap::new,
                        Collectors.averagingInt(InterviewMessage::getScore)
                ))
                .entrySet().stream()
                .map(entry -> Map.<String, Object>of("name", entry.getKey(), "score", Math.round(entry.getValue() * 10.0) / 10.0))
                .toList();

        List<Map<String, Object>> weakAnswers = scoredMessages.stream()
                .sorted(Comparator.comparing(InterviewMessage::getScore).thenComparing(InterviewMessage::getCreatedAt))
                .limit(3)
                .map(message -> {
                    InterviewSession session = sessionById.get(message.getSessionId());
                    return Map.<String, Object>of(
                            "score", message.getScore(),
                            "position", session == null ? "未知岗位" : session.getPosition(),
                            "mode", modeLabel(session == null ? null : session.getInterviewMode()),
                            "excerpt", excerpt(message.getContent())
                    );
                })
                .toList();

        return Map.of(
                "averageScore", Math.round(average * 10.0) / 10.0,
                "highestScore", highest,
                "lowestScore", lowest,
                "scoredAnswerCount", scoredMessages.size(),
                "scoreTrend", scoreTrend,
                "modeScores", modeScores,
                "weakAnswers", weakAnswers
        );
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
        return generateFirstQuestion(position, "technical", "intermediate");
    }

    public String generateFirstQuestion(String position, String interviewMode, String difficultyLevel) {
        return generateFirstQuestion(position, interviewMode, difficultyLevel, "balanced");
    }

    public String generateFirstQuestion(String position, String interviewMode, String difficultyLevel, String feedbackStyle) {
        String mode = modeLabel(interviewMode);
        String difficulty = difficultyLabel(difficultyLevel);
        String style = feedbackStyleInstruction(feedbackStyle);
        String prompt = """
                你是资深 %s 面试官，正在进行一场真实的%s面试，难度为%s。
                本场反馈风格为：%s
                请直接提出第一个面试问题，不需要打招呼和自我介绍。
                问题要贴合面试类型：技术深挖重视原理和工程取舍；行为面试重视 STAR 证据；系统设计重视边界、容量、架构权衡；项目复盘重视候选人的真实贡献。
                问题要有一定深度，考察候选人的实际能力，但一次只问一个问题。
                只输出一个自然段，不使用 Markdown 标题、列表、引用或代码块。
                """.formatted(position, mode, difficulty, style);

        return aiGateway.complete("interview-first-question", prompt);
    }

    /**
     * 非流式：生成面试总结报告
     */
    public String generateSummary(Long sessionId, Long userId) {
        return generateSummary(sessionId, userId, 0, 0, 0);
    }

    public String generateSummary(Long sessionId, Long userId, Integer averageSpeakingRate, Integer fillerWordCount, Integer averageAnswerSeconds) {
        List<InterviewMessage> history = getHistory(sessionId, userId);
        InterviewSession session = getSession(sessionId, userId);

        StringBuilder conversation = new StringBuilder();
        for (InterviewMessage msg : history) {
            String roleTag = "ai".equals(msg.getRole()) ? "面试官" : "候选人";
            conversation.append(roleTag).append(": ").append(msg.getContent()).append("\n");
        }

        String prompt = """
                你是一位资深技术面试官。请根据以下面试对话，做综合分析。
                面试岗位：%s
                面试类型：%s
                难度档位：%s
                反馈风格：%s
                表达表现信号：%s

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
                """.formatted(
                session == null ? "未知岗位" : session.getPosition(),
                modeLabel(session == null ? null : session.getInterviewMode()),
                difficultyLabel(session == null ? null : session.getDifficultyLevel()),
                feedbackStyleInstruction(session == null ? null : session.getFeedbackStyle()),
                deliverySignalContext(averageSpeakingRate, fillerWordCount, averageAnswerSeconds),
                conversation.toString());

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
                你是一位资深%s岗位面试官，正在进行真实的%s面试，难度为%s。
                岗位：%s
                建议总轮次：%d 轮
                反馈风格：%s

                以下是对话历史：
                %s

                规则：
                1. 回复固定为两部分：“简短点评：”和“下一问：”，每部分只写一个自然段
                2. 简短点评控制在 1-2 句话，指出最新回答中最具体的一项优点或缺口
                3. 下一问只提出一个问题；回答充分时深入追问，回答模糊时要求补充事实或数据，并贴合当前面试类型
                4. 不使用 Markdown 标题、列表、表格、引用、代码块或反引号，不重复候选人的整段回答
                5. 使用统一、克制、专业的中文表达，不使用 emoji，不写寒暄或总结报告
                6. 在回复最后另起一行输出机器标记 [评分:X/10]，X 为最新回答的 1-10 分整数评分
                """.formatted(
                session.getPosition(),
                modeLabel(session.getInterviewMode()),
                difficultyLabel(session.getDifficultyLevel()),
                session.getPosition(),
                session.getTargetRounds() == null ? 5 : session.getTargetRounds(),
                feedbackStyleInstruction(session.getFeedbackStyle()),
                context.toString());

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

    private String normalizeMode(String value) {
        return MODE_LABELS.containsKey(value) ? value : "technical";
    }

    private String normalizeDifficulty(String value) {
        return DIFFICULTY_LABELS.containsKey(value) ? value : "intermediate";
    }

    private int normalizeTargetRounds(Integer value) {
        if (value == null) return 5;
        return Math.max(3, Math.min(8, value));
    }

    private String normalizeFeedbackStyle(String value) {
        return FEEDBACK_STYLE_LABELS.containsKey(value) ? value : "balanced";
    }

    private String modeLabel(String value) {
        return MODE_LABELS.getOrDefault(value, MODE_LABELS.get("technical"));
    }

    private String difficultyLabel(String value) {
        return DIFFICULTY_LABELS.getOrDefault(value, DIFFICULTY_LABELS.get("intermediate"));
    }

    private String feedbackStyleInstruction(String value) {
        return switch (normalizeFeedbackStyle(value)) {
            case "encouraging" -> "鼓励型：先肯定回答中真实存在的亮点，再温和指出一项最重要的改进点，帮助候选人建立信心。";
            case "strict" -> "严格型：按照真实面试的淘汰标准进行判断，明确指出证据缺口、技术风险和不合格原因，不用模糊的安慰性表述。";
            default -> "平衡型：客观直接地兼顾回答亮点、风险和下一步建议，保持专业克制。";
        };
    }

    private String deliverySignalContext(Integer averageSpeakingRate, Integer fillerWordCount, Integer averageAnswerSeconds) {
        int speed = clampMetric(averageSpeakingRate, 0, 600);
        int fillers = clampMetric(fillerWordCount, 0, 100);
        int seconds = clampMetric(averageAnswerSeconds, 0, 600);
        if (speed == 0 && fillers == 0 && seconds == 0) {
            return "暂无可用的表达信号，主要依据回答内容进行评估。";
        }
        return "平均语速约 %d 字/分钟，平均回答时长 %d 秒，检测到约 %d 个填充词。仅作为辅助观察，不可替代对回答内容和事实证据的判断。".formatted(speed, seconds, fillers);
    }

    private int clampMetric(Integer value, int min, int max) {
        if (value == null) return 0;
        return Math.max(min, Math.min(max, value));
    }

    private Map<String, Object> emptyScoreAnalytics() {
        return Map.of(
                "averageScore", 0,
                "highestScore", 0,
                "lowestScore", 0,
                "scoredAnswerCount", 0,
                "scoreTrend", List.of(),
                "modeScores", List.of(),
                "weakAnswers", List.of()
        );
    }

    private String excerpt(String content) {
        if (content == null || content.isBlank()) return "";
        String compact = content.replaceAll("\\s+", " ").trim();
        return compact.length() > 72 ? compact.substring(0, 72) + "..." : compact;
    }
}
