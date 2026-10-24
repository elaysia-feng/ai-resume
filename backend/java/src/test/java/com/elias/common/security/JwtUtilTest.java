package com.elias.common.security;

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
        return new JwtUtil(SECRET, EXPIRATION);
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
        String token = new JwtUtil("another-secret-key-that-is-long-enough-32b", EXPIRATION)
                .generateToken("alice", 42L);

        assertFalse(newJwtUtil().validateToken(token));
    }

    @Test
    @DisplayName("已过期的 token 校验不通过")
    void expiredTokenRejected() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1000L);
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
        JwtUtil jwtUtil = new JwtUtil("   ", EXPIRATION);

        IllegalStateException e = assertThrows(IllegalStateException.class, jwtUtil::validate);
        assertTrue(e.getMessage().contains("JWT_SECRET"));
    }
}
