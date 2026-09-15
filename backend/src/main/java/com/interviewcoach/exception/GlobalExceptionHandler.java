package com.interviewcoach.exception;

import com.interviewcoach.dto.Result;
import com.interviewcoach.service.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 忽略静态资源 404（favicon.ico 等），不打印日志
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResourceFound(NoResourceFoundException e) {
        // 静默忽略，浏览器请求 favicon.ico 等不存在的静态资源是正常行为
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.error("文件大小超限: {}", e.getMessage());
        return Result.fail("文件大小超过限制（最大 10MB）");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        log.error("参数异常: {}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntime(RuntimeException e) {
        log.error("运行时异常: ", e);
        return Result.fail("服务暂时不可用，请稍后重试");
    }

    @ExceptionHandler(AiServiceException.class)
    public Result<?> handleAiService(AiServiceException e) {
        log.error("AI 服务异常: ", e);
        return Result.fail(503, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleAll(Exception e) {
        log.error("未知异常: ", e);
        return Result.fail("服务器内部错误，请稍后重试");
    }
}
