package com.elias.security.common;

import com.elias.security.common.impl.JwtUtilImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JwtUtil 单元测试。
 *
 * <p>不依赖 Spring 容器，直接用固定密钥构造，重点覆盖签名/解析/过期/类型校验这几条链路。</p>
 */
class JwtUtilTest {

    private static final String SECRET = "test-secret-key-for-unit-test-only-32bytes";
    private static final long EXPIRATION = 60_000L;

    private JwtUtil newJwtUtil() {
        return new JwtUtilImpl(SECRET, EXPIRATION);
    }

    @Test
    @DisplayName("签发后能解析出用户名和用户ID")
    void generateAndParse() {
        JwtUtil jwtUtil = newJwtUtil();
        String token = jwtUtil.generateToken("alice", 42L);

        assertNotNull(token);
        assertEquals("alice", jwtUtil.getUsernameFromToken(token));
        assertEquals(42L, jwtUtil.getUserIdFromToken(token));
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("被篡改的 token 校验不通过")
    void tamperedTokenRejected() {
        JwtUtil jwtUtil = newJwtUtil();
        String token = jwtUtil.generateToken("alice", 42L);
        String tampered = tamperSignature(token);

        assertFalse(jwtUtil.validateToken(tampered));
        assertThrows(RuntimeException.class, () -> jwtUtil.getUsernameFromToken(tampered));
    }

    /**
     * 改签名段的第一个字符。
     *
     * <p>不能改最后两个字符：base64url 末位有几个字符只有低位是冗余位，
     * 换成别的字符解出来的签名字节可能一模一样，token 照样验签通过——
     * 那样这个用例就成了随机失败（约千分之几会红），而不是真的在测篡改。</p>
     */
    private String tamperSignature(String token) {
        String[] parts = token.split("\\.");
        String signature = parts[2];
        char replacement = signature.charAt(0) == 'A' ? 'B' : 'A';
        return parts[0] + "." + parts[1] + "." + replacement + signature.substring(1);
    }

    @Test
    @DisplayName("别的密钥签出来的 token 不认")
    void tokenSignedWithOtherSecretRejected() {
        String token = new JwtUtilImpl("another-secret-key-that-is-long-enough-32b", EXPIRATION)
                .generateToken("alice", 42L);

        assertFalse(newJwtUtil().validateToken(token));
    }

    @Test
    @DisplayName("已过期的 token 校验不通过")
    void expiredTokenRejected() {
        JwtUtil jwtUtil = new JwtUtilImpl(SECRET, -1000L);
        String token = jwtUtil.generateToken("alice", 42L);

        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("验证码凭证和登录 token 不能混用")
    void verifyTokenTypeIsChecked() {
        JwtUtil jwtUtil = newJwtUtil();
        String verifyToken = jwtUtil.generateVerifyToken("a@b.com");

        assertEquals("a@b.com", jwtUtil.getEmailFromVerifyToken(verifyToken));
        // 登录 token 没有 type=verify，取邮箱时必须返回 null 而不是 subject
        assertNull(jwtUtil.getEmailFromVerifyToken(jwtUtil.generateToken("alice", 42L)));
    }

    @Test
    @DisplayName("密钥没配时启动就报错")
    void blankSecretFailsFast() {
        JwtUtilImpl jwtUtil = new JwtUtilImpl("   ", EXPIRATION);

        IllegalStateException e = assertThrows(IllegalStateException.class, jwtUtil::validate);
        assertTrue(e.getMessage().contains("JWT_SECRET"));
    }

    @Test
    @DisplayName("短密钥（默认的 change-me）也能签发 token")
    void shortSecretStillWorks() {
        // HS256 要求密钥至少 256 bit。桌面端默认 JWT_SECRET 是 "change-me"，
        // 只有 72 bit，直接当密钥会在签发时抛 WeakKeyException，
        // 表现是注册接口直接失败 —— 用户根本建不了号。
        JwtUtil jwtUtil = new JwtUtilImpl("change-me", EXPIRATION);

        String token = jwtUtil.generateToken("alice", 42L);

        assertNotNull(token);
        assertEquals("alice", jwtUtil.getUsernameFromToken(token));
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("同一个短密钥派生出的签名稳定，重启后旧 token 依然有效")
    void shortSecretDerivationIsStable() {
        String first = new JwtUtilImpl("change-me", EXPIRATION).generateToken("alice", 42L);
        String second = new JwtUtilImpl("change-me", EXPIRATION).generateToken("alice", 42L);

        // 同密钥同载荷 ⇒ 同一秒内签出的 token 签名一致
        assertEquals(first, second);
        // 换个实例（模拟重启）仍能验签
        assertTrue(new JwtUtilImpl("change-me", EXPIRATION).validateToken(first));
    }

    @Test
    @DisplayName("短密钥派生的结果与长密钥互不通用")
    void shortAndLongSecretsAreIndependent() {
        String shortToken = new JwtUtilImpl("change-me", EXPIRATION).generateToken("alice", 42L);
        String longToken = new JwtUtilImpl(SECRET, EXPIRATION).generateToken("alice", 42L);

        assertFalse(newJwtUtil().validateToken(shortToken));
        assertFalse(new JwtUtilImpl("change-me", EXPIRATION).validateToken(longToken));
    }
}
