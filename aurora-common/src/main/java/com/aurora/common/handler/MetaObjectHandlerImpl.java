package com.aurora.common.handler;

import com.aurora.common.util.SecurityUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 *
 * <p>INSERT 时填充: createTime / updateTime / createUser / deleted
 * UPDATE 时填充: updateTime / updateUser</p>
 *
 * <p>createUser / updateUser 取自 {@link SecurityUtil#currentUserId()}，
 * 未登录场景（如登录接口本身）返回 null，数据库字段需可空。</p>
 */
@Component
public class MetaObjectHandlerImpl implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createUser", Long.class, SecurityUtil.currentUserId());
        this.strictInsertFill(metaObject, "updateUser", Long.class, SecurityUtil.currentUserId());
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateUser", Long.class, SecurityUtil.currentUserId());
    }
}
