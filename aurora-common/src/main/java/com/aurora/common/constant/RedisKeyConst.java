package com.aurora.common.constant;

/**
 * Redis Key 常量
 *
 * <p>统一管理所有 Redis Key 前缀，避免散落各处导致冲突。</p>
 */
public final class RedisKeyConst {

    private RedisKeyConst() {
    }

    /** 验证码: captcha:{uuid} */
    public static final String CAPTCHA = "captcha:";

    /** Refresh Token: refresh:{userId} */
    public static final String REFRESH_TOKEN = "refresh:";

    /** 登录失败次数: login:fail:{username} */
    public static final String LOGIN_FAIL = "login:fail:";

    /** 接口幂等: idem:{idempotentId} */
    public static final String IDEM = "idem:";

    /** 选课剩余名额（Phase 3）: course:remain:{courseId} */
    public static final String COURSE_REMAIN = "course:remain:";

    /** 商品库存预扣（Phase 5）: stock:product:{productId} */
    public static final String STOCK_PRODUCT = "stock:product:";

    /** 下单分布式锁（Phase 5）: lock:order:user:{userId} */
    public static final String LOCK_ORDER_USER = "lock:order:user:";

    /** 考勤打卡锁（Phase 2）: lock:attendance:{empId}:{date} */
    public static final String LOCK_ATTENDANCE = "lock:attendance:";

    /** 排行榜 ZSet（Phase 4）: leaderboard:oj（旧版聚合，保留兼容） */
    public static final String LEADERBOARD_OJ = "leaderboard:oj";

    /** OJ 通过数排行榜 ZSet: leaderboard:accept */
    public static final String LEADERBOARD_ACCEPT = "leaderboard:accept";

    /** OJ 提交数排行榜 ZSet: leaderboard:submit */
    public static final String LEADERBOARD_SUBMIT = "leaderboard:submit";

    /** 购物车 Hash（Phase 5）: cart:{userId}（field=productId, value=quantity） */
    public static final String CART = "cart:";

    /** 秒杀限购 SETNX（Phase 5）: seckill:user:{activityId}:{userId} */
    public static final String SECKILL_USER = "seckill:user:";

    /** AI 会话最近 10 轮上下文（Phase 6）: ai:session:{sessionId} */
    public static final String AI_SESSION = "ai:session:";

    /** 选课分布式锁（Phase 3）: lock:course:select:{studentId} */
    public static final String LOCK_SELECT = "lock:course:select:";
}
