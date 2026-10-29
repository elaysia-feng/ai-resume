package com.elias.security.auth.impl;
import com.elias.security.auth.RateLimiter;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 极简内存限流器：同一个 key 在滑动窗口内最多允许 N 次。
 *
 * <p>为什么需要它：桌面端后端监听 127.0.0.1，匿名接口（注册 / 登录 / 发验证码）
 * 对本机任何进程都是可达的。没有限流时，本机脚本可以无限刷注册、爆破密码，
 * 或者拿受害者邮箱反复触发验证码邮件。</p>
 *
 * <p>按业务键限流（邮箱 / 用户名）而不是按 IP：桌面端所有请求都来自 127.0.0.1，
 * 按 IP 限会把正常用户一起限掉。</p>
 */
@Component
public class RateLimiterImpl implements RateLimiter {

    /** 窗口起点 + 已放行次数 */
    private record Bucket(long windowStart, int count) {
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * 尝试获取一次配额。
     *
     * @param key         限流维度（邮箱 / 用户名 / 接口名）
     * @param limit       窗口内允许的最大次数
     * @param windowMillis 窗口长度（毫秒）
     * @return true 表示放行，false 表示已经超限
     */
    public boolean tryAcquire(String key, int limit, long windowMillis) {
        long now = System.currentTimeMillis();
        buckets.compute(key, (k, old) -> {
            if (old == null || now - old.windowStart() >= windowMillis) {
                return new Bucket(now, 1);
            }
            return new Bucket(old.windowStart(), old.count() + 1);
        });
        return buckets.get(key).count() <= limit;
    }

    /**
     * 只检查是否已超限，不计数。
     *
     * <p>用于「操作成功才算一次」的场景（比如发验证码：发失败就不该占用配额，
     * 否则用户网络一抖就得等一分钟）。</p>
     */
    public boolean isBlocked(String key, int limit, long windowMillis) {
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            return false;
        }
        return System.currentTimeMillis() - bucket.windowStart() < windowMillis && bucket.count() >= limit;
    }

    /** 记一次（配合 {@link #isBlocked} 使用） */
    public void record(String key, long windowMillis) {
        long now = System.currentTimeMillis();
        buckets.compute(key, (k, old) -> {
            if (old == null || now - old.windowStart() >= windowMillis) {
                return new Bucket(now, 1);
            }
            return new Bucket(old.windowStart(), old.count() + 1);
        });
    }
}
