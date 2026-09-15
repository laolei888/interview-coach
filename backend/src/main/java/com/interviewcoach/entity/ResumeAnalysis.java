package com.interviewcoach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历分析记录
 */
@Data
@TableName("resume_analysis")
public class ResumeAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 原始文件名 */
    private String fileName;

    /** 本次分析对应的目标岗位 */
    private String targetPosition;

    /** 用户提供的目标岗位描述 */
    private String jobDescription;

    /** PDF 解析出的纯文本 */
    private String content;

    /** AI 综合评分（0-100） */
    private Integer aiScore;

    /** AI 完整反馈（JSON 格式保存解析结果） */
    private String aiFeedback;

    /** AI 推荐的匹配岗位 */
    private String matchPosition;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
