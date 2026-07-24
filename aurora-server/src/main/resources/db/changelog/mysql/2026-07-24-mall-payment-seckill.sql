-- liquibase formatted sql

-- changeset aurora:alter_mall_order_payment_20260724 dbms:mysql
ALTER TABLE mall_order
  ADD COLUMN pay_type TINYINT NULL COMMENT '支付方式 1微信 2支付宝 3模拟',
  ADD COLUMN pay_time DATETIME NULL,
  ADD COLUMN ship_time DATETIME NULL,
  ADD COLUMN complete_time DATETIME NULL,
  ADD COLUMN cancel_time DATETIME NULL,
  ADD COLUMN cancel_reason VARCHAR(255) NULL,
  ADD COLUMN is_seckill TINYINT NOT NULL DEFAULT 0,
  ADD COLUMN activity_id BIGINT NULL;
-- rollback ALTER TABLE mall_order DROP COLUMN pay_type, DROP COLUMN pay_time, DROP COLUMN ship_time, DROP COLUMN complete_time, DROP COLUMN cancel_time, DROP COLUMN cancel_reason, DROP COLUMN is_seckill, DROP COLUMN activity_id;

-- changeset aurora:create_mall_seckill_stock_tables_20260724 dbms:mysql
CREATE TABLE IF NOT EXISTS mall_seckill_activity (
  id BIGINT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  seckill_price DECIMAL(10,2) NOT NULL,
  seckill_stock INT NOT NULL,
  available_stock INT NOT NULL,
  limit_per_user INT NOT NULL DEFAULT 1,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_product(product_id),
  INDEX idx_time(start_time, end_time),
  INDEX idx_status(status)
);

CREATE TABLE IF NOT EXISTS mall_stock_log (
  id BIGINT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  biz_type TINYINT NOT NULL COMMENT '1下单扣减 2取消回滚 3退款回滚 4人工调整',
  quantity INT NOT NULL,
  order_id BIGINT NULL,
  remark VARCHAR(255) NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_product(product_id),
  INDEX idx_order(order_id),
  INDEX idx_biz_type(biz_type)
);
-- rollback DROP TABLE IF EXISTS mall_stock_log;
-- rollback DROP TABLE IF EXISTS mall_seckill_activity;

-- changeset aurora:mall_seckill_menu_seed_20260724 dbms:mysql
INSERT INTO sys_menu (id, title, parent_id, type, path, name, component, permission, icon, sort, status, create_time, deleted)
VALUES
(560, '秒杀管理', 500, 2, '/mall/seckill', 'MallSeckill', 'mall/seckill/index', 'mall:seckill:join', 'icon-thunderbolt', 60, 1, NOW(), 0),
(561, '秒杀抢购', 500, 2, '/mall/seckill-join', 'MallSeckillJoin', 'mall/seckill-join/index', 'mall:seckill:join', 'icon-gift', 70, 1, NOW(), 0),
(562, '库存日志', 500, 2, '/mall/stock-log', 'MallStockLog', 'mall/stock-log/index', 'mall:product:list', 'icon-file', 80, 1, NOW(), 0)
ON DUPLICATE KEY UPDATE title=VALUES(title), parent_id=VALUES(parent_id), type=VALUES(type),
  path=VALUES(path), name=VALUES(name), component=VALUES(component), permission=VALUES(permission),
  icon=VALUES(icon), sort=VALUES(sort), status=VALUES(status), update_time=NOW();

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.code IN ('admin', 'mall_admin') AND m.id IN (560, 561, 562)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);
-- rollback DELETE FROM sys_role_menu WHERE menu_id IN (560,561,562);
-- rollback DELETE FROM sys_menu WHERE id IN (560,561,562);
