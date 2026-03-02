package com.killuayz.aicode.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 指标收集器
 */
@Component
@Slf4j
public class AiModelMetricsCollector {

    @Resource
    private MeterRegistry meterRegistry;

    // 缓存已创建的指标，避免重复创建（按指标类型分离缓存）
    private final ConcurrentMap<String, Counter> requestCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> errorCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> tokenCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> responseTimersCache = new ConcurrentHashMap<>();

    /** Micrometer Tag 不允许 null，使用占位符 */
    private static String orUnknown(String value) {
        return value == null || value.isEmpty() ? "unknown" : value;
    }

    /**
     * 记录请求次数
     */
    public void recordRequest(String userId, String appId, String modelName, String status) {
        String u = orUnknown(userId);
        String a = orUnknown(appId);
        String m = orUnknown(modelName);
        String s = orUnknown(status);
        String key = String.format("%s_%s_%s_%s", u, a, m, s);
        Counter counter = requestCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_requests_total")
                        .description("AI模型总请求次数")
                        .tag("user_id", u)
                        .tag("app_id", a)
                        .tag("model_name", m)
                        .tag("status", s)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * 记录错误
     */
    public void recordError(String userId, String appId, String modelName, String errorMessage) {
        String u = orUnknown(userId);
        String a = orUnknown(appId);
        String m = orUnknown(modelName);
        String msg = orUnknown(errorMessage);
        String key = String.format("%s_%s_%s_%s", u, a, m, msg);
        Counter counter = errorCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_errors_total")
                        .description("AI模型错误次数")
                        .tag("user_id", u)
                        .tag("app_id", a)
                        .tag("model_name", m)
                        .tag("error_message", msg)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * 记录Token消耗
     */
    public void recordTokenUsage(String userId, String appId, String modelName,
                                 String tokenType, long tokenCount) {
        String u = orUnknown(userId);
        String a = orUnknown(appId);
        String m = orUnknown(modelName);
        String t = orUnknown(tokenType);
        String key = String.format("%s_%s_%s_%s", u, a, m, t);
        Counter counter = tokenCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_tokens_total")
                        .description("AI模型Token消耗总数")
                        .tag("user_id", u)
                        .tag("app_id", a)
                        .tag("model_name", m)
                        .tag("token_type", t)
                        .register(meterRegistry)
        );
        counter.increment(tokenCount);
    }

    /**
     * 记录响应时间
     */
    public void recordResponseTime(String userId, String appId, String modelName, Duration duration) {
        String u = orUnknown(userId);
        String a = orUnknown(appId);
        String m = orUnknown(modelName);
        String key = String.format("%s_%s_%s", u, a, m);
        Timer timer = responseTimersCache.computeIfAbsent(key, k ->
                Timer.builder("ai_model_response_duration_seconds")
                        .description("AI模型响应时间")
                        .tag("user_id", u)
                        .tag("app_id", a)
                        .tag("model_name", m)
                        .register(meterRegistry)
        );
        timer.record(duration);
    }
}