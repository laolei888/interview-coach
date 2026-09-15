package com.interviewcoach.controller;

import com.interviewcoach.dto.Result;
import com.interviewcoach.entity.ResumeAnalysis;
import com.interviewcoach.service.ResumeService;
import com.interviewcoach.service.AiGateway;
import com.interviewcoach.utils.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历分析接口
 */
@Slf4j
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final AiGateway aiGateway;
    private final RateLimiter rateLimiter;

    /**
     * 上传 PDF 简历并进行 AI 分析（限流：每分钟 5 次）
     */

    /**
     * 上传 PDF 简历并进行 AI 分析
     * <p>
     * 请求：POST /api/resume/analyze
     * Body: multipart/form-data, file = PDF 文件
     * <p>
     * 返回：
     * { "code": 200, "message": "success", "data": { "id": 1, "aiScore": 85, "aiFeedback": "..." } }
     */
    @PostMapping("/analyze")
    public Result<ResumeAnalysis> analyze(@RequestParam("file") MultipartFile file,
                                          HttpServletRequest request) {
        // 限流：每分钟最多 5 次
        Long userId = (Long) request.getAttribute("userId");
        if (!rateLimiter.allow("ai:resume:" + userId, 5, 60)) {
            return Result.fail(429, "操作太频繁，请 1 分钟后再试");
        }
        // 1. 校验文件
        if (file.isEmpty()) {
            return Result.badRequest("请上传 PDF 文件");
        }
        if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            return Result.badRequest("仅支持 PDF 格式简历");
        }

        log.info("收到简历文件: {}, 大小: {}KB", file.getOriginalFilename(), file.getSize() / 1024);

        // 2. 解析 PDF
        String resumeText = resumeService.parsePdf(file);

        // 3. 构建 AI Prompt
        String prompt = """
                你是一位资深 HR 和技术面试官，有 10 年招聘经验。
                请仔细分析以下简历，给出专业的评估：

                ===简历内容开始===
                %s
                ===简历内容结束===

                输出格式（严格按此结构，不要用数字序号）：
                ## 综合评分：XX分
                评分理由

                ## 亮点
                - 亮点1
                - 亮点2
                - 亮点3

                ## 不足
                - 不足1
                - 不足2
                - 不足3

                ## 优化建议
                - 建议1
                - 建议2
                - 建议3
                - 建议4

                ## 推荐岗位
                - 岗位1
                - 岗位2
                """.formatted(resumeText);

        // 4. 调用 DeepSeek AI
        String aiResult = aiGateway.complete("resume-analysis", prompt);

        log.info("AI 分析完成，结果长度: {} 字符", aiResult.length());

        // 5. 保存分析记录
        ResumeAnalysis analysis = resumeService.saveAnalysis(
                userId,
                file.getOriginalFilename(),
                resumeText,
                aiResult
        );

        return Result.ok(analysis);
    }

    /**
     * 查询简历分析历史
     */
    @GetMapping("/history")
    public Result<java.util.List<ResumeAnalysis>> history(@RequestAttribute("userId") Long userId) {
        return Result.ok(resumeService.listByUserId(userId));
    }

    /**
     * 查看某次分析的详情
     */
    @GetMapping("/{id}")
    public Result<ResumeAnalysis> detail(@PathVariable Long id,
                                         @RequestAttribute("userId") Long userId) {
        ResumeAnalysis analysis = resumeService.getByIdAndUserId(id, userId);
        if (analysis == null) {
            return Result.fail(404, "分析记录不存在或无权限访问");
        }
        return Result.ok(analysis);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id,
                                  @RequestAttribute("userId") Long userId) {
        return resumeService.removeByIdAndUserId(id, userId)
                ? Result.ok(true)
                : Result.fail(404, "分析记录不存在或无权限删除");
    }
}
