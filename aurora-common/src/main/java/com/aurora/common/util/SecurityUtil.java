package com.aurora.common.util;

import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;

/**
 * 当前登录用户上下文工具
 *
 * <p>由 JWT 拦截器（Phase 1 实现）在请求开始时调用 {@link #setCurrent} 写入 ThreadLocal，
 * 请求结束时调用 {@link #clear} 清理。</p>
 *
 * <p>{@link com.aurora.common.handler.MetaObjectHandlerImpl} 依赖此工具填充 createUser/updateUser。</p>
 */
public final class SecurityUtil {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private SecurityUtil() {
    }

    public static void setCurrent(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser current() {
        return HOLDER.get();
    }

    /**
     * 获取当前登录用户 ID；未登录返回 null（用于审计字段自动填充）
     */
    public static Long currentUserId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    /**
     * 获取当前登录用户 ID；未登录直接抛 401（用于业务接口强校验登录）
     */
    public static Long requireUserId() {
        Long uid = currentUserId();
        if (uid == null) {
            throw new BizException(BizCode.UNAUTHORIZED);
        }
        return uid;
    }

    public static String currentUsername() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.username();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 登录用户信息载体
     *
     * @param userId    用户 ID
     * @param username  用户名
     * @param roles     角色编码列表（如 "admin"）
     */
    public record LoginUser(Long userId, String username, java.util.List<String> roles) {
    }
}
