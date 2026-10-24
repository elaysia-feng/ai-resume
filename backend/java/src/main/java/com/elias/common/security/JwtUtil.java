package com.elias.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class JwtUtil {

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

    public JwtUtil(){}

    public JwtUtil(String secret, Long expiration) {
        this.secret = secret;
        this.expiration = expiration;
    }

    /**
     * 获取签名密钥
     *
     * HS256 要求密钥至少 256 bit（32 字节）。配置里的 secret 往往是
     * "change-me" 这种短字符串，直接当密钥会在签发时抛 WeakKeyException ——
     * 表现是注册接口返回「key byte array is 72 bits ... not secure enough」，
     * 而且用户已经写进库了（半成品数据），用户根本注册不进去。
     *
     * 这里统一处理：长度够就直接用（保持与旧行为一致，旧 token 继续有效）；
     * 不够就用 SHA-256 派生到 32 字节 —— 任意长度的配置都能得到合法密钥，
     * 且同一 secret 派生结果稳定，重启后已签发的 token 依然有效。
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length >= 32) {
            return Keys.hmacShaKeyFor(keyBytes);
        }
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
     * @param token JWT Token
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
     * @param token JWT Token
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
     * @param token JWT Token
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

    /**
     * 生成验证码验证凭证（临时token，用于注册时设置密码）
     * 有效期短（5分钟）
     *
     * @param email 邮箱
     * @return 验证凭证token
     */
    public String generateVerifyToken(String email) {
        Date now = new Date();
        // 验证token有效期5分钟
        Date expiryDate = new Date(now.getTime() + 5 * 60 * 1000);

        String token = Jwts.builder()
                .subject(email)                    // subject存储邮箱
                .claim("type", "verify")          // 标记为验证token
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
        log.debug("[JwtUtil] generateVerifyToken: email={}", email);
        return token;
    }

    /**
     * 从验证凭证中获取邮箱
     *
     * @param token 验证凭证
     * @return 邮箱，如果token无效返回null
     */
    public String getEmailFromVerifyToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            // 检查是否是验证token
            if (!"verify".equals(claims.get("type", String.class))) {
                log.warn("[JwtUtil] getEmailFromVerifyToken: type claim 不匹配");
                return null;
            }
            String email = claims.getSubject();
            log.debug("[JwtUtil] getEmailFromVerifyToken: 解析成功, email={}", email);
            return email;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JwtUtil] getEmailFromVerifyToken: JWT解析失败, error={}", e.getMessage());
            return null;
        }
    }
}
