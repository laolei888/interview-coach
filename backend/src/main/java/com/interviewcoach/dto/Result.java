package com.interviewcoach.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 * <p>
 * 前端收到的 JSON 格式统一为：
 * { "code": 200, "message": "success", "data": {...} }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private int code;
    private String message;
    private T data;

    // ========== 成功 ==========
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(200, message, data);
    }

    // ========== 失败 ==========
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    // ========== 常用快捷方法 ==========
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null);
    }
}
