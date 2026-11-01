package com.elias.security.common.impl;
import com.elias.security.common.JwtUtil;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * JWT工具类
 * 用于生成和验证JWT Token
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtUtilImpl implements JwtUtil {

    /**
     * 签名密钥
     */
    private String secret;

    /** Token有效期（毫秒），默认24小时 */
    private Long expiration;

    /**
     * 密钥没配就启动失败：留空等于所有 token 都签不出来，排查起来很费时间。
     */
    @jakarta.annotation.PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "未配置 jwt.secret，请在 .env 或环境变量里设置 JWT_SECRET");
        }
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }

    /** Spring 配置绑定使用的无参构造器。 */
    /** Spring 配置绑定使用的无参构造器。 */
    public JwtUtilImpl() {
    }

    /**
     * 创建测试或独立令牌工具。
     *
     * @param secret 签名密钥
     * @param expiration 令牌有效期，单位为毫秒
     */
    /**
     * 创建测试或独立令牌工具。
     *
     * @param secret 签名密钥
     * @param expiration 令牌有效期，单位为毫秒
     */
    public JwtUtilImpl(String secret, Long expiration) {
        this.secret = secret;
        this.expiration = expiration;
    }

    /**
     * 获取签名密钥
     *
     * HS256 要求密钥至少 256 bit（32 字节）。配置里的 secret 往往是
     * "change-me" 这种短字符串，直接当密钥会在签发时抛 WeakKeyException ——
     * 表现是本地会话令牌无法签发，且本机数据记录已写入数据库。
     *
     * 这里统一处理：长度够就直接用（保持与旧行为一致，旧 token 继续有效）；
     * 不够就用 SHA-256 派生到 32 字节，保持重启后的签名稳定。
     *
     * @return 可供 HS256 使用的签名密钥
     */
    private SecretKey getSigningKey() {
        // 1. 密钥长度足够时保留现有签名方式。
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length >= 32) {
            return Keys.hmacShaKeyFor(keyBytes);
        }
        // 2. 短密钥使用 SHA-256 派生到 HS256 要求的长度。
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(keyBytes);
            return Keys.hmacShaKeyFor(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256，无法派生 JWT 签名密钥", e);
        }
    }

    /**
     * 生成JWT Token
     *
     * @param username 用户名（作为Token的subject）
     * @param userId   用户ID（存储在Token的claim中）
     * @return JWT Token字符串
     */
    public String generateToken(String username, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)                    // 设置主题（用户名）
                .claim("userId", userId)            // 自定义claim存储用户ID
                .issuedAt(now)                       // 签发时间
                .expiration(expiryDate)              // 过期时间
                .signWith(getSigningKey())           // 使用HMAC-SHA签名
                .compact();
    }

    /**
     * 从Token中获取用户名
     *
     * @param token 已签发的本地 API 令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token 已签发的本地 API 令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userId", Long.class);
    }

    /**
     * 验证Token是否有效
     *
     * @param token 已签发的本地 API 令牌
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token解析失败（伪造、过期、格式错误等）
            return false;
        }
    }

}
