package com.elias.security.auth;

/** 登录与验证码入口的并发限流契约。 */
public interface RateLimiter {

    boolean tryAcquire(String key, int limit, long windowMillis);

    boolean isBlocked(String key, int limit, long windowMillis);

    void record(String key, long windowMillis);
}
