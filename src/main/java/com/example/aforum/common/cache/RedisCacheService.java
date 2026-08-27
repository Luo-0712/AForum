package com.example.aforum.common.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class RedisCacheService {

    static final String KEY_PREFIX = "aforum:cache:v1:";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;

    public RedisCacheService(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                             ObjectMapper objectMapper,
                             CacheProperties properties) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public boolean isEnabled() {
        if (!properties.isEnabled()) {
            return false;
        }
        try {
            return redisTemplateProvider.getIfAvailable() != null;
        } catch (Exception ex) {
            log.warn("Redis cache disabled because availability check failed", ex);
            return false;
        }
    }

    public String buildHashedKey(String namespace, Object... parts) {
        return KEY_PREFIX + namespace + ":" + digest(parts);
    }

    public String buildDirectKey(String namespace, Object part) {
        return KEY_PREFIX + namespace + ":" + Objects.toString(part);
    }

    public long readGeneration(String generationKey) {
        if (!isEnabled()) {
            return 0L;
        }
        try {
            String value = requireRedis().opsForValue().get(generationKey);
            return StringUtils.hasText(value) ? Long.parseLong(value) : 1L;
        } catch (Exception ex) {
            log.warn("Failed to read Redis generation key {}", generationKey, ex);
            return -1L;
        }
    }

    public long incrementGeneration(String generationKey) {
        Long value = requireRedis().opsForValue().increment(generationKey);
        return value == null ? 1L : value;
    }

    public void incrementAfterCommit(Runnable callback) {
        TransactionPhaseRunner.run(callback);
    }

    public <T> CacheLookup<T> get(String key, Class<T> valueType) {
        if (!isEnabled()) {
            return CacheLookup.miss();
        }
        String json;
        try {
            json = requireRedis().opsForValue().get(key);
        } catch (Exception ex) {
            log.warn("Failed to read Redis cache key {}", key, ex);
            return CacheLookup.miss();
        }
        if (!StringUtils.hasText(json)) {
            return CacheLookup.miss();
        }
        try {
            JavaType envelopeType = objectMapper.getTypeFactory().constructParametricType(
                    CacheEnvelope.class,
                    objectMapper.getTypeFactory().constructType(valueType));
            CacheEnvelope<T> envelope = objectMapper.readValue(json, envelopeType);
            if (envelope.present()) {
                return CacheLookup.hit(envelope.data());
            }
            return CacheLookup.negative();
        } catch (Exception ex) {
            log.warn("Failed to deserialize Redis cache key {}; treating it as a miss", key, ex);
            deleteQuietly(key);
            return CacheLookup.miss();
        }
    }

    public void put(String key, Object value, Duration ttl) {
        put(key, value, ttl, true);
    }

    public void putWithJitter(String key, Object value, long baseTtlSeconds) {
        put(key, value, jitter(baseTtlSeconds), true);
    }

    public void putNegativeWithJitter(String key, long baseTtlSeconds) {
        put(key, new CacheEnvelope<>(false, null), jitter(baseTtlSeconds), true);
    }

    private void put(String key, Object envelopeOrValue, Duration ttl, boolean wrap) {
        if (!isEnabled() || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            Object payload = wrap && !(envelopeOrValue instanceof CacheEnvelope<?>)
                    ? new CacheEnvelope<>(true, envelopeOrValue)
                    : envelopeOrValue;
            String json = objectMapper.writeValueAsString(payload);
            requireRedis().opsForValue().set(key, json, ttl);
        } catch (Exception ex) {
            log.warn("Failed to write Redis cache key {}", key, ex);
        }
    }

    public void delete(String key) {
        if (!isEnabled()) {
            return;
        }
        try {
            requireRedis().delete(key);
        } catch (Exception ex) {
            log.warn("Failed to delete Redis cache key {}", key, ex);
        }
    }

    private void deleteQuietly(String key) {
        try {
            requireRedis().delete(key);
        } catch (Exception ex) {
            log.debug("Failed to remove unreadable Redis cache key {}", key, ex);
        }
    }

    private Duration jitter(long baseTtlSeconds) {
        long base = Math.max(1L, baseTtlSeconds);
        long maxJitter = Math.max(1L, base / 10L);
        return Duration.ofSeconds(base + ThreadLocalRandom.current().nextLong(0, maxJitter + 1));
    }

    private StringRedisTemplate requireRedis() {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            throw new IllegalStateException("Redis is not configured");
        }
        return redis;
    }

    private String digest(Object[] parts) {
        String normalized = Stream.of(parts)
                .map(part -> Objects.toString(part, ""))
                .collect(Collectors.joining("\u001F"));
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to calculate cache key", ex);
        }
    }

    public record CacheLookup<T>(State state, T value) {

        public boolean hit() {
            return state == State.HIT;
        }

        public boolean negativeHit() {
            return state == State.NEGATIVE_HIT;
        }

        static <T> CacheLookup<T> miss() {
            return new CacheLookup<>(State.MISS, null);
        }

        static <T> CacheLookup<T> hit(T value) {
            return new CacheLookup<>(State.HIT, value);
        }

        static <T> CacheLookup<T> negative() {
            return new CacheLookup<>(State.NEGATIVE_HIT, null);
        }
    }

    public enum State {
        HIT,
        NEGATIVE_HIT,
        MISS
    }

    public record CacheEnvelope<T>(boolean present, T data) {
    }
}
