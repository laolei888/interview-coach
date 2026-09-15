package com.interviewcoach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试会话
 */
@Data
@TableName("interview_session")
public class InterviewSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 面试岗位（如 Java后端、前端开发） */
    private String position;

    /** 状态：active-进行中, completed-已结束 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
