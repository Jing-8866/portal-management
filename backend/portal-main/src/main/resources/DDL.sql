-- =============================================
-- 系统门户管理 - 数据库初始化脚本
-- MySQL 5.7+
-- 用法: mysql -u root -p < DDL.sql
-- 说明: 会 DROP 并重建四个库的全部表及示例数据，请勿在生产环境直接执行
-- 约定: 所有表均使用 库名.表名 全限定名
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- =============================================
-- 1. portal_main（主库：用户 / 角色 / 权限 / 子系统）
-- =============================================
CREATE DATABASE IF NOT EXISTS portal_main DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP VIEW IF EXISTS portal_main.v_user_effective_permission;
DROP TABLE IF EXISTS portal_main.sys_role_subsystem;
DROP TABLE IF EXISTS portal_main.sys_user_subsystem;
DROP TABLE IF EXISTS portal_main.sys_user_role;
DROP TABLE IF EXISTS portal_main.sys_subsystem;
DROP TABLE IF EXISTS portal_main.sys_role;
DROP TABLE IF EXISTS portal_main.sys_user;

CREATE TABLE portal_main.sys_user (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(200) NOT NULL COMMENT '密码(BCrypt加密，INIT:前缀为明文待迁移)',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE portal_main.sys_role (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE portal_main.sys_subsystem (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '子系统ID',
    `system_name` VARCHAR(100) NOT NULL COMMENT '系统名称',
    `system_code` VARCHAR(50) NOT NULL COMMENT '系统编码',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标class',
    `color` VARCHAR(20) DEFAULT NULL COMMENT '卡片颜色',
    `url` VARCHAR(255) DEFAULT NULL COMMENT '系统入口URL',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_system_code` (`system_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='子系统表';

CREATE TABLE portal_main.sys_user_role (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE portal_main.sys_user_subsystem (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `subsystem_id` BIGINT NOT NULL COMMENT '子系统ID',
    `permission_type` VARCHAR(50) NOT NULL DEFAULT 'login' COMMENT '权限类型: login/query/admin',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_subsystem_perm` (`user_id`, `subsystem_id`, `permission_type`),
    KEY `idx_subsystem_id` (`subsystem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户子系统权限表';

CREATE TABLE portal_main.sys_role_subsystem (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `subsystem_id` BIGINT NOT NULL COMMENT '子系统ID',
    `permission_type` VARCHAR(50) NOT NULL DEFAULT 'login' COMMENT '权限类型: login/query/admin',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_subsystem_perm` (`role_id`, `subsystem_id`, `permission_type`),
    KEY `idx_subsystem_id` (`subsystem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色子系统权限表';

INSERT INTO portal_main.sys_role (`role_name`, `role_code`, `description`) VALUES
('平台管理员', 'PLATFORM_ADMIN', '拥有所有系统的最高权限'),
('子系统管理员', 'SUBSYSTEM_ADMIN', '负责管理特定子系统的用户和权限'),
('普通用户', 'USER', '普通用户，仅具有被分配的权限');

INSERT INTO portal_main.sys_subsystem (`system_name`, `system_code`, `description`, `icon`, `color`, `url`, `sort_order`) VALUES
('用户管理系统', 'USER_MGMT', '管理整个系统的登录用户及其权限', 'fas fa-users-cog', '#4A90D9', '/systems/user-management/index.html', 1),
('订单商城', 'ORDER_MGMT', '在线商城：商品浏览、购物车、订单与物流', 'fas fa-shopping-cart', '#E8854A', '/systems/order-system/index.html', 2),
('数据库管理系统', 'DB_MGMT', '数据库状态监控与管理', 'fas fa-database', '#50C878', '/systems/db-management/index.html', 3),
('日志分析平台', 'LOG_ANALYSIS', '系统日志收集、分析与告警', 'fas fa-chart-line', '#9B59B6', '/systems/log-analysis/index.html', 4);

INSERT INTO portal_main.sys_role_subsystem (`role_id`, `subsystem_id`, `permission_type`) VALUES
(1, 1, 'admin'), (1, 2, 'admin'),
(1, 3, 'admin'), (1, 4, 'admin'),
(2, 1, 'login'), (2, 1, 'query'),
(2, 2, 'login'), (2, 2, 'query'),
(2, 3, 'login'), (2, 3, 'query'),
(2, 4, 'login'), (2, 4, 'query');

INSERT INTO portal_main.sys_user (`username`, `password`, `real_name`, `email`, `status`) VALUES
('admin', 'INIT:admin', '系统管理员', 'admin@portal.com', 1),
('user1', 'INIT:123456', '测试用户', 'user1@portal.com', 1),
('orderadmin', 'INIT:orderadmin', '订单子系统管理员', 'orderadmin@portal.com', 1);

INSERT INTO portal_main.sys_user_role (`user_id`, `role_id`) VALUES
(1, 1), (2, 3), (3, 3);

INSERT INTO portal_main.sys_user_subsystem (`user_id`, `subsystem_id`, `permission_type`) VALUES
(1, 1, 'admin'), (1, 2, 'admin'), (1, 3, 'admin'), (1, 4, 'admin'),
(2, 2, 'login'), (2, 2, 'query'), (2, 4, 'login'), (2, 4, 'query'),
(3, 2, 'login'), (3, 2, 'query'), (3, 2, 'admin');

CREATE VIEW portal_main.v_user_effective_permission AS
SELECT user_id, subsystem_id, permission_type, '直接分配' AS source
FROM portal_main.sys_user_subsystem
UNION
SELECT ur.user_id, rs.subsystem_id, rs.permission_type, '角色继承' AS source
FROM portal_main.sys_role_subsystem rs
INNER JOIN portal_main.sys_user_role ur ON rs.role_id = ur.role_id;


-- =============================================
-- 2. portal_order（订单商城库）
-- =============================================
CREATE DATABASE IF NOT EXISTS portal_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TABLE IF EXISTS portal_order.biz_order_logistics;
DROP TABLE IF EXISTS portal_order.biz_order_item;
DROP TABLE IF EXISTS portal_order.biz_cart_item;
DROP TABLE IF EXISTS portal_order.biz_order;
DROP TABLE IF EXISTS portal_order.biz_product;
DROP TABLE IF EXISTS portal_order.biz_user_address;
DROP TABLE IF EXISTS portal_order.biz_product_category;

CREATE TABLE portal_order.biz_product_category (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE portal_order.biz_product (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `description` TEXT COMMENT '商品描述',
    `price` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
    `image_url` VARCHAR(500) DEFAULT NULL COMMENT '商品图片URL',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
    `status` VARCHAR(20) NOT NULL DEFAULT 'on_shelf' COMMENT '状态: on_shelf/off_shelf',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_name` (`name`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE portal_order.biz_cart_item (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

CREATE TABLE portal_order.biz_order (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单号',
    `user_id` BIGINT DEFAULT NULL COMMENT '下单用户ID',
    `customer_name` VARCHAR(100) NOT NULL COMMENT '客户名称',
    `receiver_name` VARCHAR(100) DEFAULT NULL COMMENT '收货人',
    `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '收货电话',
    `receiver_address` VARCHAR(500) DEFAULT NULL COMMENT '收货地址',
    `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/paid/shipped/completed/cancelled/refunding/refunded/returning/returned',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `ship_time` DATETIME DEFAULT NULL COMMENT '发货时间',
    `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_status` (`status`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE portal_order.biz_order_item (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称快照',
    `product_price` DECIMAL(12,2) NOT NULL COMMENT '单价快照',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `subtotal` DECIMAL(12,2) NOT NULL COMMENT '小计',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

CREATE TABLE portal_order.biz_user_address (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `label` VARCHAR(50) DEFAULT NULL COMMENT '地址标签',
    `receiver_name` VARCHAR(100) NOT NULL COMMENT '收货人',
    `receiver_phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
    `province` VARCHAR(50) DEFAULT NULL COMMENT '省',
    `city` VARCHAR(50) DEFAULT NULL COMMENT '市',
    `district` VARCHAR(50) DEFAULT NULL COMMENT '区/县',
    `detail_address` VARCHAR(300) NOT NULL COMMENT '详细地址',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认: 0否 1是',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址表';

CREATE TABLE portal_order.biz_order_logistics (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '轨迹ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `title` VARCHAR(100) NOT NULL COMMENT '节点标题',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '节点说明',
    `location` VARCHAR(500) NOT NULL COMMENT '物流位置',
    `event_time` DATETIME DEFAULT NULL COMMENT '发生时间',
    `state` VARCHAR(20) NOT NULL DEFAULT 'done' COMMENT '节点状态: done/active/wait/failed',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_sort` (`order_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单物流轨迹表';

INSERT INTO portal_order.biz_product_category (`name`, `sort_order`) VALUES
('办公用品', 1),
('电子设备', 2),
('学习用品', 3);

INSERT INTO portal_order.biz_product (`name`, `description`, `price`, `stock`, `category`, `status`, `created_by`) VALUES
('无线鼠标', '人体工学设计，2.4G无线连接，续航12个月', 89.00, 200, '办公用品', 'on_shelf', 1),
('机械键盘', '青轴机械键盘，RGB背光，全键无冲', 399.00, 80, '办公用品', 'on_shelf', 1),
('27寸显示器', '2K IPS屏，75Hz刷新率，低蓝光护眼', 1299.00, 50, '电子设备', 'on_shelf', 1),
('USB-C扩展坞', '七合一扩展坞，支持4K输出和PD充电', 199.00, 150, '电子设备', 'on_shelf', 1),
('A4打印纸', '70g A4复印纸，500张/包', 25.00, 500, '办公用品', 'on_shelf', 1),
('笔记本电脑支架', '铝合金折叠支架，可调节高度', 68.00, 120, '办公用品', 'on_shelf', 1),
('降噪耳机', '主动降噪，40小时续航，蓝牙5.3', 599.00, 60, '电子设备', 'off_shelf', 1),
('移动硬盘 1TB', 'USB3.0接口，轻薄便携', 459.00, 90, '电子设备', 'on_shelf', 1);

INSERT INTO portal_order.biz_order (`order_no`, `user_id`, `customer_name`, `receiver_name`, `receiver_phone`, `receiver_address`, `amount`, `status`, `remark`, `pay_time`, `ship_time`, `complete_time`, `created_by`, `created_time`) VALUES
('ORD-2024-001', 1, '张三', '张三', '13800001001', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', 1280.00, 'completed', '办公用品采购', '2024-01-15 10:45:00', '2024-01-16 10:30:00', '2024-01-17 10:30:00', 1, '2024-01-15 10:30:00'),
('ORD-2024-002', 1, '李四', '李四', '13800001002', '北京市海淀区中关村大街1号海龙大厦15层', 3560.00, 'pending', '设备采购申请', NULL, NULL, NULL, 1, '2024-01-16 14:20:00'),
('ORD-2024-003', 1, '王五', '王五', '13800001001', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', 890.00, 'completed', '软件许可证续费', '2024-01-17 09:30:00', '2024-01-18 09:15:00', '2024-01-19 09:15:00', 1, '2024-01-17 09:15:00'),
('ORD-2024-004', 1, '赵六', '赵六', '13800001001', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', 12500.00, 'completed', '服务器采购', '2024-01-18 11:20:00', '2024-01-19 11:00:00', '2024-01-20 11:00:00', 1, '2024-01-18 11:00:00'),
('ORD-2024-005', 2, '孙七', '孙七', '13800001003', '广东省广州市天河区天河路228号广晟大厦8楼', 450.00, 'cancelled', '耗材采购-已取消', NULL, NULL, NULL, 2, '2024-01-19 16:45:00'),
('ORD-2024-006', 1, '周八', '周八', '13800001001', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', 6780.00, 'completed', '网络设备采购', '2024-01-20 08:45:00', '2024-01-21 08:30:00', '2024-01-22 08:30:00', 1, '2024-01-20 08:30:00'),
('ORD-2024-007', 2, '吴九', '吴九', '13800001002', '北京市海淀区中关村大街1号海龙大厦15层', 2340.00, 'pending', '显示器采购申请', NULL, NULL, NULL, 2, '2024-01-21 13:00:00'),
('ORD-2024-008', 1, '郑十', '郑十', '13800001001', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', 980.00, 'completed', '打印机维修', '2024-01-22 15:45:00', '2024-01-23 15:30:00', '2024-01-24 15:30:00', 1, '2024-01-22 15:30:00'),
('ORD-2024-009', 1, '钱某', '钱某', '13800001002', '北京市海淀区中关村大街1号海龙大厦15层', 15600.00, 'pending', '年度维保服务', NULL, NULL, NULL, 1, '2024-01-23 10:00:00'),
('ORD-2024-010', 1, '陈某', '陈某', '13800001001', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', 4200.00, 'completed', '云服务费用', '2024-01-24 09:15:00', '2024-01-25 09:00:00', '2024-01-26 09:00:00', 1, '2024-01-24 09:00:00');

INSERT INTO portal_order.biz_order_logistics (`order_id`, `title`, `description`, `location`, `event_time`, `state`, `sort_order`) VALUES
(1, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-15 10:30:00', 'done', 1),
(1, '付款成功', '买家已付款，等待发货', '上海市浦东新区华东仓储中心 · 备货区', '2024-01-15 10:45:00', 'done', 2),
(1, '快件已揽收', '快递已从仓库发出', '上海市浦东新区华东揽收站（张江镇）', '2024-01-16 10:30:00', 'done', 3),
(1, '运输中', '快件途经转运中心', '浙江省杭州市萧山转运中心', '2024-01-16 18:00:00', 'done', 4),
(1, '到达派送站', '快件到达末端配送站', '杭州市配送站（西湖区）', '2024-01-17 08:30:00', 'done', 5),
(1, '已签收', '买家已确认收货', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', '2024-01-17 10:30:00', 'done', 6),
(2, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-16 14:20:00', 'done', 1),
(2, '待付款', '等待买家完成付款', '上海市浦东新区华东仓储中心 · 备货区', NULL, 'active', 2),
(2, '商家发货', '等待商家发货', '上海市浦东新区华东仓储中心（金科路288号）', NULL, 'wait', 3),
(2, '确认收货', '等待买家确认收货', '北京市海淀区中关村大街1号海龙大厦15层', NULL, 'wait', 4),
(3, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-17 09:15:00', 'done', 1),
(3, '付款成功', '买家已付款，等待发货', '上海市浦东新区华东仓储中心 · 备货区', '2024-01-17 09:30:00', 'done', 2),
(3, '快件已揽收', '快递已从仓库发出', '上海市浦东新区华东揽收站（张江镇）', '2024-01-18 09:15:00', 'done', 3),
(3, '运输中', '快件途经转运中心', '浙江省杭州市萧山转运中心', '2024-01-18 15:00:00', 'done', 4),
(3, '到达派送站', '快件到达末端配送站', '杭州市配送站（西湖区）', '2024-01-19 08:00:00', 'done', 5),
(3, '已签收', '买家已确认收货', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', '2024-01-19 09:15:00', 'done', 6),
(4, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-18 11:00:00', 'done', 1),
(4, '付款成功', '买家已付款，等待发货', '上海市浦东新区华东仓储中心 · 备货区', '2024-01-18 11:20:00', 'done', 2),
(4, '快件已揽收', '快递已从仓库发出', '上海市浦东新区华东揽收站（张江镇）', '2024-01-19 11:00:00', 'done', 3),
(4, '运输中', '快件途经转运中心', '上海市青浦区华新转运中心', '2024-01-19 20:00:00', 'done', 4),
(4, '到达派送站', '快件到达末端配送站', '杭州市配送站（西湖区）', '2024-01-20 09:00:00', 'done', 5),
(4, '已签收', '买家已确认收货', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', '2024-01-20 11:00:00', 'done', 6),
(5, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-19 16:45:00', 'done', 1),
(5, '订单已取消', '未付款，订单已关闭', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-19 17:15:00', 'failed', 2),
(6, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-20 08:30:00', 'done', 1),
(6, '付款成功', '买家已付款，等待发货', '上海市浦东新区华东仓储中心 · 备货区', '2024-01-20 08:45:00', 'done', 2),
(6, '快件已揽收', '快递已从仓库发出', '上海市浦东新区华东揽收站（张江镇）', '2024-01-21 08:30:00', 'done', 3),
(6, '运输中', '快件途经转运中心', '浙江省杭州市萧山转运中心', '2024-01-21 16:00:00', 'done', 4),
(6, '到达派送站', '快件到达末端配送站', '杭州市配送站（西湖区）', '2024-01-22 08:30:00', 'done', 5),
(6, '已签收', '买家已确认收货', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', '2024-01-22 08:30:00', 'done', 6),
(7, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-21 13:00:00', 'done', 1),
(7, '待付款', '等待买家完成付款', '上海市浦东新区华东仓储中心 · 备货区', NULL, 'active', 2),
(7, '商家发货', '等待商家发货', '上海市浦东新区华东仓储中心（金科路288号）', NULL, 'wait', 3),
(7, '确认收货', '等待买家确认收货', '北京市海淀区中关村大街1号海龙大厦15层', NULL, 'wait', 4),
(8, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-22 15:30:00', 'done', 1),
(8, '付款成功', '买家已付款，等待发货', '上海市浦东新区华东仓储中心 · 备货区', '2024-01-22 15:45:00', 'done', 2),
(8, '快件已揽收', '快递已从仓库发出', '上海市浦东新区华东揽收站（张江镇）', '2024-01-23 15:30:00', 'done', 3),
(8, '运输中', '快件途经转运中心', '浙江省杭州市萧山转运中心', '2024-01-23 22:00:00', 'done', 4),
(8, '到达派送站', '快件到达末端配送站', '杭州市配送站（西湖区）', '2024-01-24 09:00:00', 'done', 5),
(8, '已签收', '买家已确认收货', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', '2024-01-24 15:30:00', 'done', 6),
(9, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-23 10:00:00', 'done', 1),
(9, '待付款', '等待买家完成付款', '上海市浦东新区华东仓储中心 · 备货区', NULL, 'active', 2),
(9, '商家发货', '等待商家发货', '上海市浦东新区华东仓储中心（金科路288号）', NULL, 'wait', 3),
(9, '确认收货', '等待买家确认收货', '北京市海淀区中关村大街1号海龙大厦15层', NULL, 'wait', 4),
(10, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-24 09:00:00', 'done', 1),
(10, '付款成功', '买家已付款，等待发货', '上海市浦东新区华东仓储中心 · 备货区', '2024-01-24 09:15:00', 'done', 2),
(10, '快件已揽收', '快递已从仓库发出', '上海市浦东新区华东揽收站（张江镇）', '2024-01-25 09:00:00', 'done', 3),
(10, '运输中', '快件途经转运中心', '浙江省杭州市萧山转运中心', '2024-01-25 17:00:00', 'done', 4),
(10, '到达派送站', '快件到达末端配送站', '杭州市配送站（西湖区）', '2024-01-26 08:00:00', 'done', 5),
(10, '已签收', '买家已确认收货', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', '2024-01-26 09:00:00', 'done', 6);


-- =============================================
-- 3. portal_dbmgmt（数据库管理库）
-- =============================================
CREATE DATABASE IF NOT EXISTS portal_dbmgmt DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TABLE IF EXISTS portal_dbmgmt.biz_db_instance;

CREATE TABLE portal_dbmgmt.biz_db_instance (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '实例ID',
    `instance_name` VARCHAR(100) NOT NULL COMMENT '实例名称',
    `host` VARCHAR(100) NOT NULL COMMENT '主机地址',
    `port` INT NOT NULL DEFAULT 3306 COMMENT '端口',
    `db_name` VARCHAR(100) DEFAULT NULL COMMENT '数据库名',
    `schema_name` VARCHAR(100) DEFAULT NULL COMMENT 'Schema名称(PostgreSQL/Oracle等)',
    `db_username` VARCHAR(50) DEFAULT NULL COMMENT '连接用户名',
    `db_password` VARCHAR(200) DEFAULT NULL COMMENT '连接密码',
    `db_type` VARCHAR(20) NOT NULL DEFAULT 'MySQL' COMMENT '数据库类型',
    `charset` VARCHAR(20) DEFAULT 'utf8mb4' COMMENT '字符集',
    `table_count` INT DEFAULT 0 COMMENT '表数量',
    `storage_size` VARCHAR(20) DEFAULT '0 MB' COMMENT '存储大小',
    `active_connections` INT DEFAULT 0 COMMENT '活跃连接数',
    `max_connections` INT DEFAULT 100 COMMENT '最大连接数',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-离线 1-在线',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库实例表';

DROP TABLE IF EXISTS portal_dbmgmt.biz_db_table_snapshot;

CREATE TABLE portal_dbmgmt.biz_db_table_snapshot (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `instance_name` VARCHAR(100) NOT NULL COMMENT '实例名称',
    `table_name` VARCHAR(200) NOT NULL COMMENT '表名',
    `schema_name` VARCHAR(100) DEFAULT NULL COMMENT 'Schema名称',
    `table_comment` VARCHAR(500) DEFAULT NULL COMMENT '表注释',
    `engine` VARCHAR(50) DEFAULT NULL COMMENT '存储引擎/类型',
    `data_bytes` BIGINT DEFAULT 0 COMMENT '数据大小(字节)',
    `data_length` VARCHAR(20) DEFAULT NULL COMMENT '数据大小(展示)',
    `create_time` VARCHAR(50) DEFAULT NULL COMMENT '创建时间',
    `synced_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
    PRIMARY KEY (`id`),
    KEY `idx_instance_name` (`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库实例表清单快照';

-- 已有环境增量升级:
-- ALTER TABLE portal_dbmgmt.biz_db_instance ADD COLUMN `schema_name` VARCHAR(100) DEFAULT NULL COMMENT 'Schema名称(PostgreSQL/Oracle等)' AFTER `db_name`;
-- CREATE TABLE portal_dbmgmt.biz_db_table_snapshot (...同上...);

INSERT INTO portal_dbmgmt.biz_db_instance (`instance_name`, `host`, `port`, `db_name`, `db_username`, `db_password`, `db_type`, `charset`, `table_count`, `storage_size`, `active_connections`, `max_connections`, `status`, `description`) VALUES
('portal_main', 'localhost', 3306, 'portal_main', 'root', 'root', 'MySQL', 'utf8mb4', 7, '128 MB', 12, 100, 1, '门户管理主库'),
('portal_order', 'localhost', 3306, 'portal_order', 'root', 'root', 'MySQL', 'utf8mb4', 7, '256 MB', 8, 200, 1, '订单商城业务库'),
('portal_dbmgmt', 'localhost', 3306, 'portal_dbmgmt', 'root', 'root', 'MySQL', 'utf8mb4', 1, '64 MB', 3, 100, 1, '数据库管理库'),
('portal_log', 'localhost', 3306, 'portal_log', 'root', 'root', 'MySQL', 'utf8mb4', 1, '512 MB', 5, 150, 1, '日志存储库'),
('backup_db', '192.168.1.20', 3306, 'backup_db', 'root', 'root', 'MySQL', 'utf8mb4', 8, '2.1 GB', 0, 50, 0, '备份库(维护中)');


-- =============================================
-- 4. portal_log（日志分析库）
-- =============================================
CREATE DATABASE IF NOT EXISTS portal_log DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TABLE IF EXISTS portal_log.sys_operation_log;

CREATE TABLE portal_log.sys_operation_log (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
    `subsystem_code` VARCHAR(50) DEFAULT NULL COMMENT '来源子系统编码',
    `level` VARCHAR(10) NOT NULL DEFAULT 'INFO' COMMENT '日志级别: INFO/WARN/ERROR/FATAL',
    `operation` VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
    `method` VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
    `params` TEXT DEFAULT NULL COMMENT '请求参数',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-失败 1-成功',
    `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
    `duration` BIGINT DEFAULT NULL COMMENT '执行时长(ms)',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_time` (`created_time`),
    KEY `idx_level` (`level`),
    KEY `idx_subsystem` (`subsystem_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

INSERT INTO portal_log.sys_operation_log (`user_id`, `username`, `subsystem_code`, `level`, `operation`, `method`, `ip`, `status`, `duration`, `created_time`) VALUES
(1, 'admin', 'USER_MGMT', 'INFO', '用户登录成功', 'POST /api/auth/login', '192.168.1.100', 1, 45, NOW()),
(1, 'admin', 'ORDER_MGMT', 'INFO', '创建订单 ORD-2024-010', 'POST /api/orders/checkout', '192.168.1.100', 1, 120, NOW()),
(2, 'user1', 'USER_MGMT', 'WARN', '登录失败-密码错误', 'POST /api/auth/login', '192.168.1.101', 0, 30, NOW()),
(1, 'admin', 'DB_MGMT', 'INFO', '数据库备份完成: portal_main (128MB)', 'POST /api/db/backup', '192.168.1.100', 1, 5200, NOW()),
(1, 'admin', 'ORDER_MGMT', 'ERROR', '连接池耗尽: max connections (100) reached', 'GET /api/orders', '192.168.1.100', 0, 30000, NOW()),
(NULL, 'system', 'DB_MGMT', 'WARN', '数据库连接数过高: active=95/100', 'SYSTEM_MONITOR', '127.0.0.1', 1, 5, NOW()),
(1, 'admin', 'LOG_ANALYSIS', 'INFO', '日志归档完成: 2024-01-16', 'POST /api/logs/archive', '192.168.1.100', 1, 8500, NOW()),
(2, 'user1', 'ORDER_MGMT', 'INFO', '查询订单列表', 'GET /api/orders', '192.168.1.101', 1, 35, NOW()),
(1, 'admin', 'USER_MGMT', 'ERROR', '发送验证邮件失败: SMTP连接超时', 'POST /api/users/verify-email', '192.168.1.100', 0, 60000, NOW()),
(1, 'admin', 'DB_MGMT', 'FATAL', 'backup_db主从同步中断', 'SYSTEM_MONITOR', '127.0.0.1', 0, 0, NOW()),
(NULL, 'system', 'LOG_ANALYSIS', 'INFO', '系统健康检查: 所有服务正常', 'HEALTH_CHECK', '127.0.0.1', 1, 15, NOW()),
(1, 'admin', 'USER_MGMT', 'INFO', '新增用户: user1', 'POST /api/users', '192.168.1.100', 1, 85, NOW());


SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 初始化完成
-- 测试账号:
--   admin      / admin123   平台管理员（全部子系统 admin）
--   user1      / 123456     普通用户（订单商城、日志 login/query）
--   orderadmin / order123   订单子系统管理员（仅 ORDER_MGMT admin）
-- 首次启动后端后 PasswordInitializer 会自动加密 INIT: 前缀密码
-- =============================================
