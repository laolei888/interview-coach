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
                                          @RequestParam(value = "targetPosition", required = false) String targetPosition,
                                          @RequestParam(value = "jobDescription", required = false) String jobDescription,
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
        targetPosition = normalize(targetPosition, 100);
        jobDescription = normalize(jobDescription, 6000);

        log.info("收到简历文件: {}, 大小: {}KB", file.getOriginalFilename(), file.getSize() / 1024);

        // 2. 解析 PDF
        String resumeText = resumeService.parsePdf(file);

        // 3. 构建 AI Prompt
        String prompt = """
                你是一位资深技术招聘负责人和 ATS 简历优化顾问。请基于简历事实进行岗位定向诊断，
                不要虚构候选人的经历、数据或技术栈。目标岗位和 JD 未提供时，按简历内容推断最匹配方向。

                ===目标岗位===
                %s
                ===岗位 JD===
                %s

                ===简历内容开始===
                %s
                ===简历内容结束===

                输出格式（严格按此结构，不要用数字序号）：
                ## 综合评分：XX分
                用 2-3 句话概括岗位匹配结论和最关键的改进方向。

                ## 岗位匹配度
                - 核心要求覆盖：说明已覆盖的技能和经历
                - 关键缺口：说明 JD 要求但简历缺少证据的部分
                - ATS 关键词：列出建议自然补充的关键词，不得建议虚构

                ## 亮点
                - 列出 3 条有事实依据、对目标岗位有效的亮点

                ## 风险与不足
                - 列出 3 条会影响筛选或面试判断的问题

                ## 项目经历改写建议
                - 给出 2-4 条可直接用于改写的建议，优先使用“行动 + 技术 + 结果”结构

                ## 优先行动清单
                - P0：本次投递前必须修改的事项
                - P1：可进一步增强说服力的事项

                ## 推荐岗位
                - 推荐 2-3 个匹配方向，并简述理由
                """.formatted(
                targetPosition == null ? "未指定" : targetPosition,
                jobDescription == null ? "未提供" : jobDescription,
                resumeText
        );

        // 4. 调用 DeepSeek AI
        String aiResult = aiGateway.complete("resume-analysis", prompt);

        log.info("AI 分析完成，结果长度: {} 字符", aiResult.length());

        // 5. 保存分析记录
        ResumeAnalysis analysis = resumeService.saveAnalysis(
                userId,
                file.getOriginalFilename(),
                targetPosition,
                jobDescription,
                resumeText,
                aiResult
        );

        return Result.ok(analysis);
    }

    private String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
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
