package com.example.aforum.auth.service;

/**
 * Token 黑名单存储抽象。
 * <p>
 * 优先使用 Redis（重启不丢失、分布式共享）；Redis 不可用时回退到内存实现，
 * 保证测试与未部署 Redis 的环境仍可正常运行。
 */
public interface TokenBlacklistStore {

    /**
     * 将 token 加入黑名单，直到其自然过期。
     *
     * @param token           待失效的 token
     * @param expireAtMillis  token 的过期毫秒时间戳
     */
    void blacklist(String token, long expireAtMillis);

    /**
     * 判断 token 是否已被加入黑名单。
     */
    boolean isBlacklisted(String token);

    /**
     * 清理已过期的黑名单条目。
     * <p>
     * Redis 模式下由 key TTL 自动过期，此方法为 no-op；内存模式下由定时任务调用。
     */
    void cleanup();
}
