package com.interviewcoach.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interviewcoach.entity.ResumeAnalysis;
import com.interviewcoach.mapper.ResumeAnalysisMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 简历服务 - PDF 解析 + 数据库操作
 */
@Slf4j
@Service
public class ResumeService extends ServiceImpl<ResumeAnalysisMapper, ResumeAnalysis> {

    /**
     * 从上传的 PDF 文件中提取纯文本
     */
    public String parsePdf(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            // 按行排序，保留基本结构
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            log.info("PDF 解析成功，文件名: {}, 字符数: {}", file.getOriginalFilename(), text.length());
            return text;
        } catch (IOException e) {
            log.error("PDF 解析失败: {}", e.getMessage());
            throw new RuntimeException("PDF 文件解析失败，请检查文件格式: " + e.getMessage());
        }
    }

    /**
     * 保存简历分析记录
     */
    public ResumeAnalysis saveAnalysis(Long userId, String fileName, String content, String aiFeedback) {
        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setUserId(userId);
        analysis.setFileName(fileName);
        analysis.setContent(content);
        analysis.setAiFeedback(aiFeedback);
        analysis.setMatchPosition(extractMatchPosition(aiFeedback));

        // 尝试从 AI 反馈中提取分数
        analysis.setAiScore(extractScore(aiFeedback));

        save(analysis);
        log.info("简历分析记录已保存，ID: {}", analysis.getId());
        return analysis;
    }

    /**
     * 查询当前用户的简历分析历史
     */
    public List<ResumeAnalysis> listByUserId(Long userId) {
        return list(new LambdaQueryWrapper<ResumeAnalysis>()
                .eq(ResumeAnalysis::getUserId, userId)
                .orderByDesc(ResumeAnalysis::getCreatedAt));
    }

    /**
     * 查询当前用户可访问的简历分析详情
     */
    public ResumeAnalysis getByIdAndUserId(Long id, Long userId) {
        return getOne(new LambdaQueryWrapper<ResumeAnalysis>()
                .eq(ResumeAnalysis::getId, id)
                .eq(ResumeAnalysis::getUserId, userId)
                .last("LIMIT 1"));
    }

    /**
     * 统计当前用户的简历分析数量
     */
    public long countByUserId(Long userId) {
        return count(new LambdaQueryWrapper<ResumeAnalysis>()
                .eq(ResumeAnalysis::getUserId, userId));
    }

    public boolean removeByIdAndUserId(Long id, Long userId) {
        ResumeAnalysis analysis = getByIdAndUserId(id, userId);
        return analysis != null && removeById(analysis.getId());
    }

    /**
     * 从 AI 返回的文本中提取评分（跳过列表序号，取真正的分数）
     */
    private Integer extractScore(String aiFeedback) {
        try {
            String[] lines = aiFeedback.split("\n");
            for (String line : lines) {
                if (line.contains("评分") || line.contains("分数") || line.contains("综合")) {
                    // 去掉行首的列表序号和 markdown 标记（如 "1. " "## " 等）
                    String cleaned = line.replaceFirst("^[#\\d]+[\\.\\、\\#\\s]*", "").trim();
                    String numbers = cleaned.replaceAll("[^0-9]", " ").trim();
                    if (!numbers.isEmpty()) {
                        // 取所有数字中最大的那个（列表序号已被去掉，真正的分数通常最大）
                        String[] parts = numbers.split("\\s+");
                        int best = 0;
                        for (String p : parts) {
                            int n = Integer.parseInt(p);
                            if (n >= 10 && n <= 100 && n > best) {
                                best = n;
                            }
                        }
                        if (best >= 10) return best;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("无法从 AI 反馈中提取分数");
        }
        return null;
    }

    /**
     * 从 AI 反馈中提取推荐岗位（截断到 200 字符）
     */
    private String extractMatchPosition(String aiFeedback) {
        for (String line : aiFeedback.split("\n")) {
            if (line.contains("岗位") || line.contains("方向") || line.contains("适合")) {
                String result = line.replaceAll("[#*\\-\\d.]", "").trim();
                // 截断防止超出数据库字段长度
                return result.length() > 200 ? result.substring(0, 200) : result;
            }
        }
        return null;
    }
}
