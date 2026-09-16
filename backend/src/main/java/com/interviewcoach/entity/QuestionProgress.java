package com.interviewcoach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question_progress")
public class QuestionProgress {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long questionId;
    private String status;
    private Integer practiceCount;
    private LocalDateTime lastPracticedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
