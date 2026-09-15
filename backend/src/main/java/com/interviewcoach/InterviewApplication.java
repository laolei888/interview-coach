package com.interviewcoach;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 智能面试辅导平台 - 启动类
 *
 * @author InterviewCoach
 */
@SpringBootApplication
@MapperScan("com.interviewcoach.mapper")
public class InterviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewApplication.class, args);
        System.out.println("""
                ╔══════════════════════════════════════════════╗
                ║  🚀 AI 智能面试辅导平台 启动成功！            ║
                ║  API 文档: http://localhost:8080/doc.html     ║
                ║  健康检查: http://localhost:8080/api/health   ║
                ╚══════════════════════════════════════════════╝
                """);
    }
}
