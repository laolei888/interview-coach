package com.interviewcoach.controller;

import com.interviewcoach.dto.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import javax.sql.DataSource;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查接口
 */
@RestController
public class HealthController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final String deepSeekApiKey;

    public HealthController(DataSource dataSource,
                            RedisConnectionFactory redisConnectionFactory,
                            @Value("${spring.ai.openai.api-key:}") String deepSeekApiKey) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
        this.deepSeekApiKey = deepSeekApiKey;
    }

    @GetMapping("/api/health")
    public Result<Map<String, Object>> health() {
        boolean database = checkDatabase();
        boolean redis = checkRedis();
        boolean ai = deepSeekApiKey != null && !deepSeekApiKey.isBlank();
        String status = database && redis && ai ? "UP" : "DEGRADED";
        return Result.ok(Map.of(
                "status", status,
                "service", "AI Interview Coach",
                "time", LocalDateTime.now().toString(),
                "dependencies", Map.of("database", state(database), "redis", state(redis), "ai", state(ai))
        ));
    }

    private boolean checkDatabase() {
        try (var connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkRedis() {
        try (var connection = redisConnectionFactory.getConnection()) {
            return "PONG".equalsIgnoreCase(connection.ping());
        } catch (Exception e) {
            return false;
        }
    }

    private String state(boolean available) {
        return available ? "UP" : "DOWN";
    }
}
