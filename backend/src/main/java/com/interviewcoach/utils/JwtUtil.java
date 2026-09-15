package com.interviewcoach.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类 — 生成和解析 Token
 */
public class JwtUtil {

    // 生产环境通过环境变量注入；默认值仅用于本地开发。
    private static final String SECRET = System.getenv().getOrDefault(
            "INTERVIEW_JWT_SECRET", "interview-coach-local-jwt-secret-change-me");
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRE = 24 * 60 * 60 * 1000; // 24小时

    /**
     * 生成 Token
     */
    public static String generate(Long userId, String username) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(KEY)
                .compact();
    }

    /**
     * 从 Token 中解析用户 ID
     */
    public static Long getUserId(String token) {
        try {
            String subject = parse(token).getPayload().getSubject();
            return Long.valueOf(subject);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 Token 中解析用户名
     */
    public static String getUsername(String token) {
        try {
            return (String) parse(token).getPayload().get("username");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public static boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token);
    }
}
