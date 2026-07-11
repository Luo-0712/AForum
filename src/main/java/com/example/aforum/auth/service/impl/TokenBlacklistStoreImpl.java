package com.example.aforum.auth.service.impl;

import com.example.aforum.auth.service.TokenBlacklistStore;
import com.example.aforum.common.constant.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单存储实现：Redis 优先，内存兜底。
 * <p>
 * Redis 可用时（生产/dev 部署了 Redis）：黑名单写入带 TTL 的 key，由 Redis 自动过期，
 * 重启不丢失、分布式节点共享。Redis 不可用时（test profile 或未部署 Redis）：
 * 回退到内存 {@link ConcurrentHashMap}，行为与原内存实现一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistStoreImpl implements TokenBlacklistStore {

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    /** 内存兜底：key=token, value=过期毫秒时间戳 */
    private final ConcurrentHashMap<String, Long> inMemory = new ConcurrentHashMap<>();

    @Override
    public void blacklist(String token, long expireAtMillis) {
        long ttlMillis = expireAtMillis - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis != null) {
            try {
                redis.opsForValue().set(
                    CacheConstants.TOKEN_BLACKLIST_PREFIX + token,
                    "1", ttlMillis, TimeUnit.MILLISECONDS);
                return;
            } catch (Exception e) {
                log.warn("Redis 黑名单写入失败，回退内存: {}", e.getMessage());
            }
        }
        inMemory.put(token, expireAtMillis);
    }

    @Override
    public boolean isBlacklisted(String token) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis != null) {
            try {
                return Boolean.TRUE.equals(
                    redis.hasKey(CacheConstants.TOKEN_BLACKLIST_PREFIX + token));
            } catch (Exception e) {
                log.warn("Redis 黑名单查询失败，回退内存: {}", e.getMessage());
            }
        }
        return inMemory.containsKey(token);
    }

    @Override
    public void cleanup() {
        // Redis 模式由 key TTL 自动过期；仅内存模式需要主动清理
        if (redisTemplateProvider.getIfAvailable() != null) {
            return;
        }
        long now = System.currentTimeMillis();
        inMemory.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
