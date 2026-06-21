package com.aurora.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 操作工具
 *
 * <p>薄封装 {@link RedisTemplate}，提供常用 KV / 过期 / 删除 / 自增 操作。
 * 业务模块直接注入此工具即可，避免到处直接用 RedisTemplate。</p>
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public void set(String key, Object value, long ttl, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, ttl, unit);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    public String getString(String key) {
        Object v = redisTemplate.opsForValue().get(key);
        return v == null ? null : v.toString();
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Long deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys == null || keys.isEmpty() ? 0L : redisTemplate.delete(keys);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public Boolean expire(String key, long ttl, TimeUnit unit) {
        return redisTemplate.expire(key, ttl, unit);
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 不存在则设置（SETNX）- 用于幂等性校验、防重复提交
     *
     * @return true 表示设置成功（首次），false 表示 key 已存在
     */
    public Boolean setIfAbsent(String key, Object value, long ttl, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, ttl, unit);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }
}
