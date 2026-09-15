package com.interviewcoach.interceptor;

import com.interviewcoach.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 登录拦截器 — 校验 JWT Token
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // 放行 OPTIONS（预检请求）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从 Header 获取 Token
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.setStatus(401);
            writeUnauthorized(response);
            return false;
        }

        String token = auth.substring(7);
        if (!JwtUtil.validate(token)) {
            response.setStatus(401);
            writeUnauthorized(response);
            return false;
        }

        // 将用户信息存入 request 属性，Controller 中可以获取
        request.setAttribute("userId", JwtUtil.getUserId(token));
        request.setAttribute("username", JwtUtil.getUsername(token));
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response) {
        try {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已失效，请重新登录\",\"data\":null}");
        } catch (IOException ignored) {
            // The status code has already been set; there is nothing else to recover here.
        }
    }
}
