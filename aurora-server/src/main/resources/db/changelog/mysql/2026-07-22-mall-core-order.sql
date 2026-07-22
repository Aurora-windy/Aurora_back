-- liquibase formatted sql

-- changeset aurora:create_mall_core_order_tables_20260722 dbms:mysql
CREATE TABLE IF NOT EXISTS mall_product (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500) NULL,
  price DECIMAL(12,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 0,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_status(status),
  INDEX idx_name(name)
);

CREATE TABLE IF NOT EXISTS mall_cart (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_product_deleted(user_id, product_id, deleted),
  INDEX idx_user(user_id),
  INDEX idx_product(product_id)
);

CREATE TABLE IF NOT EXISTS mall_order (
  id BIGINT PRIMARY KEY,
  order_no VARCHAR(40) NOT NULL,
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status TINYINT NOT NULL DEFAULT 0,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_order_no(order_no),
  INDEX idx_user(user_id),
  INDEX idx_status(status)
);

CREATE TABLE IF NOT EXISTS mall_order_item (
  id BIGINT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(100) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  quantity INT NOT NULL,
  subtotal_amount DECIMAL(12,2) NOT NULL,
  create_user BIGINT NULL,
  create_time DATETIME NULL,
  update_user BIGINT NULL,
  update_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_order(order_id),
  INDEX idx_product(product_id)
);
-- rollback DROP TABLE IF EXISTS mall_order_item;
-- rollback DROP TABLE IF EXISTS mall_order;
-- rollback DROP TABLE IF EXISTS mall_cart;
-- rollback DROP TABLE IF EXISTS mall_product;

-- changeset aurora:seed_mall_core_order_menu_20260722 dbms:mysql
INSERT INTO sys_menu (id, parent_id, title, type, name, path, component, icon, permission, sort, visible, status, create_time, update_time, deleted)
VALUES
    (500, 0, '电商商城', 1, 'mall', '/mall', NULL, 'IconShoppingCart', NULL, 500, 1, 1, NOW(), NOW(), 0),
    (510, 500, '商品管理', 2, 'mall-products', '/mall/products', 'mall/product/index', 'IconApps', 'mall:product:list', 510, 1, 1, NOW(), NOW(), 0),
    (511, 510, '商品查询', 3, NULL, NULL, NULL, NULL, 'mall:product:list', 511, 0, 1, NOW(), NOW(), 0),
    (512, 510, '商品新增', 3, NULL, NULL, NULL, NULL, 'mall:product:add', 512, 0, 1, NOW(), NOW(), 0),
    (513, 510, '商品编辑', 3, NULL, NULL, NULL, NULL, 'mall:product:edit', 513, 0, 1, NOW(), NOW(), 0),
    (514, 510, '商品删除', 3, NULL, NULL, NULL, NULL, 'mall:product:remove', 514, 0, 1, NOW(), NOW(), 0),
    (515, 510, '商品上下架', 3, NULL, NULL, NULL, NULL, 'mall:product:status', 515, 0, 1, NOW(), NOW(), 0),
    (520, 500, '商品购买', 2, 'mall-shop', '/mall/shop', 'mall/shop/index', 'IconShoppingCart', 'mall:product:list', 520, 1, 1, NOW(), NOW(), 0),
    (530, 500, '购物车', 2, 'mall-cart', '/mall/cart', 'mall/cart/index', 'IconList', 'mall:cart:list', 530, 1, 1, NOW(), NOW(), 0),
    (531, 530, '购物车查询', 3, NULL, NULL, NULL, NULL, 'mall:cart:list', 531, 0, 1, NOW(), NOW(), 0),
    (532, 530, '加入购物车', 3, NULL, NULL, NULL, NULL, 'mall:cart:add', 532, 0, 1, NOW(), NOW(), 0),
    (533, 530, '修改购物车', 3, NULL, NULL, NULL, NULL, 'mall:cart:edit', 533, 0, 1, NOW(), NOW(), 0),
    (534, 530, '移除购物车', 3, NULL, NULL, NULL, NULL, 'mall:cart:remove', 534, 0, 1, NOW(), NOW(), 0),
    (540, 500, '订单管理', 2, 'mall-orders', '/mall/orders', 'mall/order/index', 'IconFile', 'mall:order:list', 540, 1, 1, NOW(), NOW(), 0),
    (541, 540, '订单查询', 3, NULL, NULL, NULL, NULL, 'mall:order:list', 541, 0, 1, NOW(), NOW(), 0),
    (550, 500, '我的订单', 2, 'mall-my-orders', '/mall/my-orders', 'mall/my-order/index', 'IconHistory', 'mall:order:view-my', 550, 1, 1, NOW(), NOW(), 0),
    (551, 550, '我的订单查询', 3, NULL, NULL, NULL, NULL, 'mall:order:view-my', 551, 0, 1, NOW(), NOW(), 0),
    (552, 550, '提交订单', 3, NULL, NULL, NULL, NULL, 'mall:order:place', 552, 0, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    title = VALUES(title),
    type = VALUES(type),
    name = VALUES(name),
    path = VALUES(path),
    component = VALUES(component),
    icon = VALUES(icon),
    permission = VALUES(permission),
    sort = VALUES(sort),
    visible = VALUES(visible),
    status = VALUES(status),
    update_time = NOW(),
    deleted = 0;
-- rollback DELETE FROM sys_menu WHERE id BETWEEN 500 AND 552;

-- changeset aurora:seed_mall_core_order_role_menu_20260722 dbms:mysql
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id BETWEEN 500 AND 552
 WHERE r.code IN ('admin', 'mall_admin')
   AND r.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id IN (500, 520, 530, 531, 532, 533, 534, 550, 551, 552)
 WHERE r.code = 'student'
   AND r.deleted = 0;
-- rollback DELETE FROM sys_role_menu WHERE menu_id BETWEEN 500 AND 552;

-- changeset aurora:seed_mall_demo_products_20260722 dbms:mysql
INSERT INTO mall_product (id, name, description, price, stock, status, create_time, update_time, deleted)
VALUES
    (5001, 'AURORA 课程设计模板包', '用于演示普通商品购买和库存扣减。', 99.00, 20, 1, NOW(), NOW(), 0),
    (5002, '高并发案例资料', '后续秒杀批次会扩展为限时商品。', 129.00, 10, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    price = VALUES(price),
    stock = VALUES(stock),
    status = VALUES(status),
    update_time = NOW(),
    deleted = 0;
-- rollback DELETE FROM mall_product WHERE id IN (5001, 5002);
