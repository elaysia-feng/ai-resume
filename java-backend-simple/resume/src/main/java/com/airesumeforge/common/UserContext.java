package com.airesumeforge.common;

/**
 * 用户上下文工具类
 * 基于 ThreadLocal 存储当前请求的用户ID
 * 在 JwtAuthFilter 中设置，Controller/Service 中获取
 *
 * <pre>
 * 用法：
 *   // 获取当前用户ID（String）
 *   String userId = UserContext.getUserId();
 *
 *   // 获取当前用户ID（Long）
 *   Long id = UserContext.getUserIdLong();
 *
 *   // 在Filter中设置（JwtAuthFilter）
 *   UserContext.setUserId(userIdStr);
 *
 *   // 请求结束后清理（重要！）
 *   UserContext.clear();
 * </pre>
 */
public final class UserContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID_LONG = new ThreadLocal<>();

    private UserContext() {}

    /**
     * 获取当前用户ID（String）
     */
    public static String getUserId() {
        return USER_ID.get();
    }

    /**
     * 获取当前用户ID（Long）
     * 注意：仅当 User.id 为 Long 时使用
     */
    public static Long getUserIdLong() {
        return USER_ID_LONG.get();
    }

    /**
     * 设置当前用户ID（String）
     */
    public static void setUserId(String userId) {
        USER_ID.set(userId);
        if (userId != null) {
            try {
                USER_ID_LONG.set(Long.parseLong(userId));
            } catch (NumberFormatException e) {
                USER_ID_LONG.set(null);
            }
        } else {
            USER_ID_LONG.set(null);
        }
    }

    /**
     * 清除上下文（每个请求结束后必须调用）
     */
    public static void clear() {
        USER_ID.remove();
        USER_ID_LONG.remove();
    }

    /**
     * 得到并检验userId是否为空
     */
    public static Long verifyGetUserId(){
        Long userIdLong = getUserIdLong();
        if (userIdLong == null) {
            throw new IllegalStateException("UserId has not been set");
        }
        return userIdLong;
    }
}
