-- liquibase formatted sql

-- changeset aurora:create_sys_user dbms:mysql
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT         COMMENT '主键',
    username        VARCHAR(50)  NOT NULL                           COMMENT '用户名',
    password        VARCHAR(100) NOT NULL                           COMMENT '密码（BCrypt 加密）',
    nickname        VARCHAR(50)                                     COMMENT '昵称',
    avatar          VARCHAR(255)                                    COMMENT '头像 URL',
    email           VARCHAR(100)                                    COMMENT '邮箱',
    phone           VARCHAR(20)                                     COMMENT '手机号',
    gender          TINYINT      DEFAULT 0                          COMMENT '性别 0未知 1男 2女',
    status          TINYINT      DEFAULT 1                          COMMENT '状态 1启用 0禁用',
    dept_id         BIGINT                                          COMMENT '部门 ID（HR 模块外键）',
    last_login_time DATETIME                                        COMMENT '最后登录时间',
    create_user     BIGINT                                          COMMENT '创建人 ID',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    update_user     BIGINT                                          COMMENT '更新人 ID',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP       COMMENT '更新时间',
    deleted         TINYINT      DEFAULT 0                          COMMENT '逻辑删除 0未删 1已删',
    UNIQUE KEY uk_username_deleted (username, deleted),
    INDEX idx_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
-- rollback DROP TABLE IF EXISTS sys_user;

-- changeset aurora:create_sys_role dbms:mysql
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT             COMMENT '主键',
    name        VARCHAR(50)  NOT NULL                               COMMENT '角色名称',
    code        VARCHAR(50)  NOT NULL                               COMMENT '角色编码',
    data_scope  TINYINT      DEFAULT 1                              COMMENT '数据范围 1全部 2本部门 3本人',
    sort        INT          DEFAULT 0                              COMMENT '排序',
    status      TINYINT      DEFAULT 1                              COMMENT '状态 1启用 0禁用',
    remark      VARCHAR(255)                                        COMMENT '备注',
    create_user BIGINT,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_user BIGINT,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      DEFAULT 0,
    UNIQUE KEY uk_code_deleted (code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';
-- rollback DROP TABLE IF EXISTS sys_role;

-- changeset aurora:create_sys_menu dbms:mysql
CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT              COMMENT '主键',
    parent_id   BIGINT       DEFAULT 0                              COMMENT '父菜单 ID（0 为根）',
    title       VARCHAR(50)  NOT NULL                               COMMENT '标题',
    type        TINYINT      NOT NULL                               COMMENT '类型 1目录 2菜单 3按钮',
    name        VARCHAR(50)                                          COMMENT '前端路由 name',
    path        VARCHAR(200)                                         COMMENT '路由 path',
    component   VARCHAR(200)                                         COMMENT '前端组件路径',
    icon        VARCHAR(50)                                          COMMENT '图标',
    permission  VARCHAR(100)                                         COMMENT '权限标识（如 user:add）',
    sort        INT          DEFAULT 0,
    visible     TINYINT      DEFAULT 1                              COMMENT '是否可见 1是 0否',
    status      TINYINT      DEFAULT 1                              COMMENT '状态 1启用 0禁用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      DEFAULT 0,
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';
-- rollback DROP TABLE IF EXISTS sys_menu;

-- changeset aurora:create_sys_user_role dbms:mysql
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    INDEX idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关系';
-- rollback DROP TABLE IF EXISTS sys_user_role;

-- changeset aurora:create_sys_role_menu dbms:mysql
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id),
    INDEX idx_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单关系';
-- rollback DROP TABLE IF EXISTS sys_role_menu;

-- changeset aurora:create_sys_log dbms:mysql
CREATE TABLE IF NOT EXISTS sys_log (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT          COMMENT '主键',
    user_id        BIGINT                                            COMMENT '操作用户 ID',
    username       VARCHAR(50)                                       COMMENT '操作用户名（冗余）',
    description    VARCHAR(200)                                      COMMENT '操作描述',
    request_uri    VARCHAR(255)                                      COMMENT '请求 URI',
    request_method VARCHAR(10)                                       COMMENT '请求方法',
    request_params TEXT                                              COMMENT '请求参数',
    response_time  INT                                               COMMENT '响应耗时 ms',
    ip             VARCHAR(50)                                       COMMENT 'IP',
    location       VARCHAR(100)                                      COMMENT '归属地',
    status         TINYINT                                           COMMENT '状态 1成功 0失败',
    error_msg      TEXT                                              COMMENT '错误信息',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';
-- rollback DROP TABLE IF EXISTS sys_log;