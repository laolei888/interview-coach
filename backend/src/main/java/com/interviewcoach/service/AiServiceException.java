package com.interviewcoach.service;

/** AI 上游不可用或响应不符合预期时使用的业务异常。 */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
