package com.elias.security.common;

/** JWT 签发、解析和验证契约。 */
public interface JwtUtil {

    String generateToken(String username, Long userId);

    String getUsernameFromToken(String token);

    Long getUserIdFromToken(String token);

    boolean validateToken(String token);

    String generateVerifyToken(String email);

    String getEmailFromVerifyToken(String token);
}
