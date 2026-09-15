package com.interviewcoach.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** 统一封装非流式 AI 调用，集中处理超时、重试和可观测性。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGateway {

    private final ChatClient chatClient;

    @Value("${app.ai.timeout-seconds:45}")
    private long timeoutSeconds;

    public String complete(String scene, String prompt) {
        long started = System.nanoTime();
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String content = CompletableFuture
                        .supplyAsync(() -> chatClient.prompt().user(prompt).call().content())
                        .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                        .join();
                if (content == null || content.isBlank()) {
                    throw new IllegalStateException("AI 返回了空内容");
                }
                log.info("AI 调用成功 scene={}, attempt={}, chars={}, durationMs={}",
                        scene, attempt, content.length(), elapsedMs(started));
                return content.trim();
            } catch (RuntimeException ex) {
                last = ex;
                log.warn("AI 调用失败 scene={}, attempt={}, durationMs={}, reason={}",
                        scene, attempt, elapsedMs(started), ex.getMessage());
                if (attempt < 2) {
                    try {
                        Thread.sleep(Duration.ofMillis(350L * attempt).toMillis());
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("AI 调用被中断", interrupted);
                    }
                }
            }
        }
        if (containsTimeout(last)) {
            throw new AiServiceException("AI 服务响应超时，请稍后重试", last);
        }
        throw new AiServiceException("AI 服务暂时不可用，请稍后重试", last);
    }

    private long elapsedMs(long started) {
        return Duration.ofNanos(System.nanoTime() - started).toMillis();
    }

    private boolean containsTimeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof TimeoutException) return true;
            current = current.getCause();
        }
        return false;
    }
}
