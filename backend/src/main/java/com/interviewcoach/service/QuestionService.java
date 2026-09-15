package com.interviewcoach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewcoach.entity.Question;
import com.interviewcoach.mapper.QuestionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QuestionService extends ServiceImpl<QuestionMapper, Question> {

    private final AiGateway aiGateway;

    @Autowired
    public QuestionService(AiGateway aiGateway) {
        this.aiGateway = aiGateway;
    }

    /** 保留测试及旧调用方的构造兼容性。 */
    public QuestionService(ChatClient chatClient) {
        this.aiGateway = new AiGateway(chatClient);
    }

    /**
     * AI 批量生成面试题
     */
    public java.util.List<Question> generateByAi(Long userId, String category, String difficulty, int count) {
        String prompt = """
                你是一位资深技术面试官。请出 %d 道 %s 相关的、难度为 %s 的面试题。
                每题都要包含题目和参考答案。
                用以下格式输出，题目之间用 --- 分隔：
                ### 题目
                题目内容
                ### 参考答案
                答案内容
                ---
                ### 题目
                题目内容
                ### 参考答案
                答案内容
                """.formatted(count, category, difficulty);

        String result = aiGateway.complete("question-generation", prompt);

        // 按 --- 分割每道题
        java.util.List<Question> questions = new java.util.ArrayList<>();
        String[] blocks = result.split("---");
        for (String block : blocks) {
            String question = "", answer = "";
            String[] parts = block.split("###\\s*参考答案");
            if (parts.length > 0) {
                question = parts[0].replaceAll("###\\s*题目", "").trim();
            }
            if (parts.length > 1) {
                answer = parts[1].trim();
            }
            if (!question.isEmpty()) {
                Question q = new Question();
                q.setUserId(userId);
                q.setCategory(category);
                q.setDifficulty(difficulty);
                q.setQuestion(question);
                q.setAnswer(answer);
                save(q);
                questions.add(q);
            }
        }
        log.info("AI 批量生成 {} 道题，成功 {} 道", count, questions.size());
        return questions;
    }

    /**
     * 按分类、难度、关键词筛选 + 分页
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<Question> page(
            Long userId, String keyword, String category, String difficulty, String source, int page, int size) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if ("public".equals(source)) {
            wrapper.isNull(Question::getUserId);
        } else if ("mine".equals(source)) {
            wrapper.eq(Question::getUserId, userId);
        } else {
            wrapper.and(w -> w.isNull(Question::getUserId).or().eq(Question::getUserId, userId));
        }
        if (category != null && !category.isBlank()) wrapper.eq(Question::getCategory, category);
        if (difficulty != null && !difficulty.isBlank()) wrapper.eq(Question::getDifficulty, difficulty);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Question::getQuestion, keyword).or().like(Question::getAnswer, keyword));
        }
        wrapper.orderByDesc(Question::getCreatedAt);
        return page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), wrapper);
    }

    /**
     * 统计当前用户可见题目数量（公共题 + 自己创建的题）
     */
    public long countVisible(Long userId) {
        return count(new LambdaQueryWrapper<Question>()
                .and(w -> w.isNull(Question::getUserId).or().eq(Question::getUserId, userId)));
    }

    public Question getOwned(Long questionId, Long userId) {
        return getOne(new LambdaQueryWrapper<Question>()
                .eq(Question::getId, questionId)
                .eq(Question::getUserId, userId)
                .last("LIMIT 1"));
    }

    public Question updateOwned(Long questionId, Long userId, Question input) {
        Question existing = getOwned(questionId, userId);
        if (existing == null) return null;
        existing.setCategory(input.getCategory());
        existing.setDifficulty(input.getDifficulty());
        existing.setQuestion(input.getQuestion());
        existing.setAnswer(input.getAnswer());
        updateById(existing);
        return existing;
    }

    public boolean removeOwned(Long questionId, Long userId) {
        Question existing = getOwned(questionId, userId);
        return existing != null && removeById(existing.getId());
    }
}
