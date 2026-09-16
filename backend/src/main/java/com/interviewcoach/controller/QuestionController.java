package com.interviewcoach.controller;

import com.interviewcoach.dto.Result;
import com.interviewcoach.entity.Question;
import com.interviewcoach.service.QuestionService;
import com.interviewcoach.service.QuestionProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionProgressService questionProgressService;

    @GetMapping("/progress")
    public Result<Map<String, Object>> progress(@RequestAttribute("userId") Long userId) {
        return Result.ok(Map.of(
                "records", questionProgressService.listByUser(userId),
                "stats", questionProgressService.stats(userId)
        ));
    }

    @PostMapping("/{id}/progress")
    public Result<?> recordProgress(@PathVariable Long id,
                                    @RequestBody Map<String, String> body,
                                    @RequestAttribute("userId") Long userId) {
        String status = body.get("status");
        if (!java.util.Set.of("review", "mastered").contains(status)) {
            return Result.badRequest("练习状态无效");
        }
        var progress = questionProgressService.record(userId, id, status);
        return progress == null ? Result.fail(404, "题目不存在或无权限访问") : Result.ok(progress);
    }

    /**
     * 获取题目列表（支持搜索 + 分类 + 难度 + 分页）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "all") String source,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestAttribute("userId") Long userId) {
        if (page < 1 || size < 1 || size > 100) {
            return Result.badRequest("分页参数无效，size 需在 1 到 100 之间");
        }
        if (!java.util.Set.of("all", "public", "mine").contains(source)) {
            return Result.badRequest("题目来源参数无效");
        }
        var result = questionService.page(userId, keyword, category, difficulty, source, page, size);
        return Result.ok(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    /**
     * AI 批量生成题目
     */
    @PostMapping("/ai-generate")
    public Result<java.util.List<Question>> aiGenerate(@RequestBody Map<String, Object> body,
                                                       @RequestAttribute("userId") Long userId) {
        String category = (String) body.getOrDefault("category", "Java");
        String difficulty = (String) body.getOrDefault("difficulty", "中等");
        int count = body.containsKey("count") ? ((Number) body.get("count")).intValue() : 1;
        if (count < 1 || count > 5) {
            return Result.badRequest("生成数量需在 1 到 5 之间");
        }
        if (category.isBlank() || category.length() > 50 || !java.util.Set.of("简单", "中等", "困难").contains(difficulty)) {
            return Result.badRequest("分类或难度参数无效");
        }
        return Result.ok(questionService.generateByAi(userId, category, difficulty, count));
    }

    /**
     * 手动添加题目
     */
    @PostMapping("/add")
    public Result<Question> add(@RequestBody Question question,
                                @RequestAttribute("userId") Long userId) {
        Result<Question> validation = validateQuestion(question);
        if (validation != null) return validation;
        question.setId(null);
        question.setUserId(userId);
        questionService.save(question);
        return Result.ok(question);
    }

    @PutMapping("/{id}")
    public Result<Question> update(@PathVariable Long id, @RequestBody Question question,
                                   @RequestAttribute("userId") Long userId) {
        Result<Question> validation = validateQuestion(question);
        if (validation != null) return validation;
        Question updated = questionService.updateOwned(id, userId, question);
        return updated == null
                ? Result.fail(404, "题目不存在或无权限修改")
                : Result.ok(updated);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id,
                                  @RequestAttribute("userId") Long userId) {
        return questionService.removeOwned(id, userId)
                ? Result.ok(true)
                : Result.fail(404, "题目不存在或无权限删除");
    }

    private Result<Question> validateQuestion(Question question) {
        if (question == null || question.getQuestion() == null || question.getQuestion().isBlank()) {
            return Result.badRequest("题目内容不能为空");
        }
        if (question.getQuestion().length() > 2000 ||
                (question.getAnswer() != null && question.getAnswer().length() > 10000)) {
            return Result.badRequest("题目或答案内容过长");
        }
        if (question.getCategory() == null || question.getCategory().isBlank()) {
            return Result.badRequest("题目分类不能为空");
        }
        if (!java.util.Set.of("简单", "中等", "困难").contains(question.getDifficulty())) {
            return Result.badRequest("题目难度无效");
        }
        return null;
    }
}
