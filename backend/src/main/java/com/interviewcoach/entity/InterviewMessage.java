package com.interviewcoach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试对话消息
 */
@Data
@TableName("interview_message")
public class InterviewMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属面试会话 ID */
    private Long sessionId;

    /** 角色： user-候选人, ai-面试官 */
    private String role;

    /** 消息内容 */
    private String content;

    /** AI 对用户回答的评分（1-10），仅 role=user 时有值 */
    private Integer score;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
