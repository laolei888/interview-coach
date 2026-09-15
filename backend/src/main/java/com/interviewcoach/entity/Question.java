package com.interviewcoach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID，null 表示公共预置题 */
    private Long userId;

    /** 分类：Java、前端、数据库、算法... */
    private String category;

    /** 题目 */
    private String question;

    /** 参考答案 */
    private String answer;

    /** 难度：简单 / 中等 / 困难 */
    private String difficulty;

    private LocalDateTime createdAt;
}
