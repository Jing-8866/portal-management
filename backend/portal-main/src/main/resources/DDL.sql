-- =============================================
-- 系统门户管理 - 多数据库DDL及初始化脚本
-- MySQL 5.7
-- 每个子系统使用独立数据库
-- =============================================

-- =============================================
-- 数据库1: portal_main（主库 - 用户/角色/权限/子系统）
-- 对应模块: portal-main
-- =============================================
CREATE DATABASE IF NOT EXISTS portal_main DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE portal_main;

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(200) NOT NULL COMMENT '密码(BCrypt加密)',
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

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
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

DROP TABLE IF EXISTS `sys_subsystem`;
CREATE TABLE `sys_subsystem` (
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

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

DROP TABLE IF EXISTS `sys_user_subsystem`;
CREATE TABLE `sys_user_subsystem` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `subsystem_id` BIGINT NOT NULL COMMENT '子系统ID',
    `permission_type` VARCHAR(50) NOT NULL DEFAULT 'login' COMMENT '权限类型: login/query/admin',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_subsystem_perm` (`user_id`, `subsystem_id`, `permission_type`),
    KEY `idx_subsystem_id` (`subsystem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户子系统权限表';


DROP TABLE IF EXISTS `sys_role_subsystem`;
CREATE TABLE `sys_role_subsystem` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `subsystem_id` BIGINT NOT NULL COMMENT '子系统ID',
    `permission_type` VARCHAR(50) NOT NULL DEFAULT 'login' COMMENT '权限类型: login/query/admin',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_subsystem_perm` (`role_id`, `subsystem_id`, `permission_type`),
    KEY `idx_subsystem_id` (`subsystem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色子系统权限表';

-- 初始化：平台管理员拥有所有子系统管理权限
INSERT INTO `sys_role_subsystem` (`role_id`, `subsystem_id`, `permission_type`) VALUES
(1, 1, 'admin'), (1, 2, 'admin'),
(1, 3, 'admin'), (1, 4, 'admin'),
(2, 1, 'login'), (2, 1, 'query'),
(2, 2, 'login'), (2, 2, 'query'),
(2, 3, 'login'), (2, 3, 'query'),
(2, 4, 'login'), (2, 4, 'query');


-- portal_main 初始化数据
INSERT INTO `sys_role` (`role_name`, `role_code`, `description`) VALUES
('平台管理员', 'PLATFORM_ADMIN', '拥有所有系统的最高权限'),
('子系统管理员', 'SUBSYSTEM_ADMIN', '负责管理特定子系统的用户和权限'),
('普通用户', 'USER', '普通用户，仅具有被分配的权限');

INSERT INTO `sys_subsystem` (`system_name`, `system_code`, `description`, `icon`, `color`, `url`, `sort_order`) VALUES
('用户管理系统', 'USER_MGMT', '管理整个系统的登录用户及其权限', 'fas fa-users-cog', '#4A90D9', '/systems/user-management/index.html', 1),
('订单管理系统', 'ORDER_MGMT', '管理系统订单的创建、处理和追踪', 'fas fa-shopping-cart', '#E8854A', '/systems/order-system/index.html', 2),
('数据库管理系统', 'DB_MGMT', '数据库状态监控与管理', 'fas fa-database', '#50C878', '/systems/db-management/index.html', 3),
('日志分析平台', 'LOG_ANALYSIS', '系统日志收集、分析与告警', 'fas fa-chart-line', '#9B59B6', '/systems/log-analysis/index.html', 4);

-- 密码: admin123，启动后由PasswordInitializer自动加密
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `email`, `status`) VALUES
('admin', 'INIT:admin', '系统管理员', 'admin@portal.com', 1),
('user1', 'INIT:123456', '测试用户', 'user1@portal.com', 1);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1), (2, 3);

INSERT INTO `sys_user_subsystem` (`user_id`, `subsystem_id`, `permission_type`) VALUES
(1, 1, 'admin'), (1, 2, 'admin'), (1, 3, 'admin'), (1, 4, 'admin'),
(2, 2, 'login'), (2, 2, 'query'), (2, 4, 'login'), (2, 4, 'query');


-- =============================================
-- 数据库2: portal_order（订单管理系统）
-- 对应模块: portal-order
-- =============================================
CREATE DATABASE IF NOT EXISTS portal_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE portal_order;

DROP TABLE IF EXISTS `biz_order`;
CREATE TABLE `biz_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单号',
    `customer_name` VARCHAR(100) NOT NULL COMMENT '客户名称',
    `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/processing/completed/cancelled',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_status` (`status`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单初始化数据
INSERT INTO `biz_order` (`order_no`, `customer_name`, `amount`, `status`, `remark`, `created_by`, `created_time`) VALUES
('ORD-2024-001', '张三', 1280.00, 'completed', '办公用品采购', 1, '2024-01-15 10:30:00'),
('ORD-2024-002', '李四', 3560.00, 'pending', '设备采购申请', 1, '2024-01-16 14:20:00'),
('ORD-2024-003', '王五', 890.00, 'processing', '软件许可证续费', 1, '2024-01-17 09:15:00'),
('ORD-2024-004', '赵六', 12500.00, 'completed', '服务器采购', 1, '2024-01-18 11:00:00'),
('ORD-2024-005', '孙七', 450.00, 'cancelled', '耗材采购-已取消', 2, '2024-01-19 16:45:00'),
('ORD-2024-006', '周八', 6780.00, 'completed', '网络设备采购', 1, '2024-01-20 08:30:00'),
('ORD-2024-007', '吴九', 2340.00, 'pending', '显示器采购申请', 2, '2024-01-21 13:00:00'),
('ORD-2024-008', '郑十', 980.00, 'processing', '打印机维修', 1, '2024-01-22 15:30:00'),
('ORD-2024-009', '钱某', 15600.00, 'pending', '年度维保服务', 1, '2024-01-23 10:00:00'),
('ORD-2024-010', '陈某', 4200.00, 'completed', '云服务费用', 1, '2024-01-24 09:00:00');


-- =============================================
-- 数据库3: portal_dbmgmt（数据库管理系统）
-- 对应模块: portal-dbmgmt
-- =============================================
CREATE DATABASE IF NOT EXISTS portal_dbmgmt DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE portal_dbmgmt;

DROP TABLE IF EXISTS `biz_db_instance`;
CREATE TABLE `biz_db_instance` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '实例ID',
    `instance_name` VARCHAR(100) NOT NULL COMMENT '实例名称',
    `host` VARCHAR(100) NOT NULL COMMENT '主机地址',
    `port` INT NOT NULL DEFAULT 3306 COMMENT '端口',
    `db_name` VARCHAR(100) DEFAULT NULL COMMENT '数据库名',
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

-- 数据库实例初始化数据
INSERT INTO `biz_db_instance` (`instance_name`, `host`, `port`, `db_name`, `db_username`, `db_password`, `db_type`, `charset`, `table_count`, `storage_size`, `active_connections`, `max_connections`, `status`, `description`) VALUES
('portal_main', 'localhost', 3306, 'portal_main', 'root', 'root', 'MySQL', 'utf8mb4', 5, '128 MB', 12, 100, 1, '门户管理主库'),
('portal_order', 'localhost', 3306, 'portal_order', 'root', 'root', 'MySQL', 'utf8mb4', 1, '256 MB', 8, 200, 1, '订单业务库'),
('portal_dbmgmt', 'localhost', 3306, 'portal_dbmgmt', 'root', 'root', 'MySQL', 'utf8mb4', 1, '64 MB', 3, 100, 1, '数据库管理库'),
('portal_log', 'localhost', 3306, 'portal_log', 'root', 'root', 'MySQL', 'utf8mb4', 1, '512 MB', 5, 150, 1, '日志存储库'),
('backup_db', '192.168.1.20', 3306, 'backup_db', 'root', 'root', 'MySQL', 'utf8mb4', 8, '2.1 GB', 0, 50, 0, '备份库(维护中)');


-- =============================================
-- 数据库4: portal_log（日志分析系统）
-- 对应模块: portal-log
-- =============================================
CREATE DATABASE IF NOT EXISTS portal_log DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE portal_log;

DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
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

-- 日志初始化数据
INSERT INTO `sys_operation_log` (`user_id`, `username`, `subsystem_code`, `level`, `operation`, `method`, `ip`, `status`, `duration`, `created_time`) VALUES
(1, 'admin', 'USER_MGMT', 'INFO', '用户登录成功', 'POST /api/auth/login', '192.168.1.100', 1, 45, NOW()),
(1, 'admin', 'ORDER_MGMT', 'INFO', '创建订单 ORD-2024-010', 'POST /api/orders', '192.168.1.100', 1, 120, NOW()),
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




-- =============================================
-- portal_main 补充: 用户有效权限视图
-- 合并直接分配权限 + 角色继承权限
-- =============================================
USE portal_main;

DROP VIEW IF EXISTS `v_user_effective_permission`;
CREATE VIEW `v_user_effective_permission` AS
SELECT user_id, subsystem_id, permission_type, '直接分配' AS source
FROM sys_user_subsystem
UNION
SELECT ur.user_id, rs.subsystem_id, rs.permission_type, '角色继承' AS source
FROM sys_role_subsystem rs
INNER JOIN sys_user_role ur ON rs.role_id = ur.role_id;


-- =============================================
-- portal_order 升级: 商城订单子系统（追加执行）
-- 商品、购物车、订单明细及订单表扩展
-- =============================================
USE portal_order;

-- 商品表
CREATE TABLE IF NOT EXISTS `biz_product` (
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

-- 购物车表
CREATE TABLE IF NOT EXISTS `biz_cart_item` (
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

-- 订单明细表
CREATE TABLE IF NOT EXISTS `biz_order_item` (
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

-- 扩展订单表（若列已存在请跳过对应 ALTER）
ALTER TABLE `biz_order` ADD COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '下单用户ID' AFTER `order_no`;
ALTER TABLE `biz_order` ADD COLUMN `receiver_name` VARCHAR(100) DEFAULT NULL COMMENT '收货人' AFTER `customer_name`;
ALTER TABLE `biz_order` ADD COLUMN `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '收货电话' AFTER `receiver_name`;
ALTER TABLE `biz_order` ADD COLUMN `receiver_address` VARCHAR(500) DEFAULT NULL COMMENT '收货地址' AFTER `receiver_phone`;
ALTER TABLE `biz_order` ADD COLUMN `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间' AFTER `remark`;
ALTER TABLE `biz_order` ADD COLUMN `ship_time` DATETIME DEFAULT NULL COMMENT '发货时间' AFTER `pay_time`;
ALTER TABLE `biz_order` ADD COLUMN `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间' AFTER `ship_time`;
ALTER TABLE `biz_order` ADD KEY `idx_user_id` (`user_id`);

-- 商品示例数据
INSERT INTO `biz_product` (`name`, `description`, `price`, `stock`, `category`, `status`, `created_by`) VALUES
('无线鼠标', '人体工学设计，2.4G无线连接，续航12个月', 89.00, 200, '办公用品', 'on_shelf', 1),
('机械键盘', '青轴机械键盘，RGB背光，全键无冲', 399.00, 80, '办公用品', 'on_shelf', 1),
('27寸显示器', '2K IPS屏，75Hz刷新率，低蓝光护眼', 1299.00, 50, '电子设备', 'on_shelf', 1),
('USB-C扩展坞', '七合一扩展坞，支持4K输出和PD充电', 199.00, 150, '电子设备', 'on_shelf', 1),
('A4打印纸', '70g A4复印纸，500张/包', 25.00, 500, '办公用品', 'on_shelf', 1),
('笔记本电脑支架', '铝合金折叠支架，可调节高度', 68.00, 120, '办公用品', 'on_shelf', 1),
('降噪耳机', '主动降噪，40小时续航，蓝牙5.3', 599.00, 60, '电子设备', 'off_shelf', 1),
('移动硬盘 1TB', 'USB3.0接口，轻薄便携', 459.00, 90, '电子设备', 'on_shelf', 1);

-- 更新历史订单关联用户（示例）
UPDATE `biz_order` SET `user_id` = `created_by` WHERE `user_id` IS NULL AND `created_by` IS NOT NULL;
UPDATE `biz_order` SET `status` = 'completed' WHERE `status` = 'processing';


-- =============================================
-- portal_order 升级: 用户收货地址
-- =============================================
USE portal_order;

CREATE TABLE IF NOT EXISTS `biz_user_address` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `label` VARCHAR(50) DEFAULT NULL COMMENT '地址标签，如：家、公司',
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


-- =============================================
-- portal_order 升级: 商品分类字典
-- =============================================
USE portal_order;

CREATE TABLE IF NOT EXISTS `biz_product_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

INSERT IGNORE INTO `biz_product_category` (`name`, `sort_order`) VALUES
('办公用品', 1),
('电子设备', 2),
('学习用品', 3);

-- 修正历史商品中不规范的分类
UPDATE `biz_product` SET `category` = '电子设备'
WHERE `category` IS NULL OR `category` = '' OR `category` NOT IN ('办公用品', '电子设备', '学习用品');
UPDATE `biz_product` SET `category` = '电子设备'
WHERE `name` LIKE '%U盘%' OR `name` LIKE '%硬盘%' OR `name` LIKE '%移动硬盘%';


-- =============================================
-- portal_order 升级: 订单物流轨迹表
-- =============================================
USE portal_order;

CREATE TABLE IF NOT EXISTS `biz_order_logistics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '轨迹ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `title` VARCHAR(100) NOT NULL COMMENT '节点标题',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '节点说明',
    `location` VARCHAR(500) NOT NULL COMMENT '物流位置',
    `event_time` DATETIME DEFAULT NULL COMMENT '发生时间（未发生节点为空）',
    `state` VARCHAR(20) NOT NULL DEFAULT 'done' COMMENT '节点状态: done/active/wait/failed',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_sort` (`order_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单物流轨迹表';

-- 补充示例订单收货信息及物流时间节点（便于物流轨迹展示）
UPDATE `biz_order` SET
    `receiver_name` = `customer_name`,
    `receiver_phone` = '13800001001',
    `receiver_address` = '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室',
    `pay_time` = DATE_ADD(`created_time`, INTERVAL 15 MINUTE),
    `ship_time` = DATE_ADD(`created_time`, INTERVAL 1 DAY),
    `complete_time` = DATE_ADD(`created_time`, INTERVAL 2 DAY)
WHERE `id` IN (1, 3, 4, 6, 8, 10) AND `status` = 'completed';

UPDATE `biz_order` SET
    `receiver_name` = `customer_name`,
    `receiver_phone` = '13800001002',
    `receiver_address` = '北京市海淀区中关村大街1号海龙大厦15层'
WHERE `id` IN (2, 7, 9) AND `status` = 'pending';

UPDATE `biz_order` SET
    `receiver_name` = `customer_name`,
    `receiver_phone` = '13800001003',
    `receiver_address` = '广东省广州市天河区天河路228号广晟大厦8楼'
WHERE `id` = 5 AND `status` = 'cancelled';

-- 物流轨迹初始化数据（覆盖 order_id 1-10 的示例轨迹）
DELETE FROM `biz_order_logistics` WHERE `order_id` BETWEEN 1 AND 10;

-- 订单1 ORD-2024-001 已完成
INSERT INTO `biz_order_logistics` (`order_id`, `title`, `description`, `location`, `event_time`, `state`, `sort_order`) VALUES
(1, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-15 10:30:00', 'done', 1),
(1, '付款成功', '买家已付款，等待发货', '上海市浦东新区华东仓储中心 · 备货区', '2024-01-15 10:45:00', 'done', 2),
(1, '快件已揽收', '快递已从仓库发出', '上海市浦东新区华东揽收站（张江镇）', '2024-01-16 10:30:00', 'done', 3),
(1, '运输中', '快件途经转运中心', '浙江省杭州市萧山转运中心', '2024-01-16 18:00:00', 'done', 4),
(1, '到达派送站', '快件到达末端配送站', '杭州市配送站（西湖区）', '2024-01-17 08:30:00', 'done', 5),
(1, '已签收', '买家已确认收货', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', '2024-01-17 10:30:00', 'done', 6);

-- 订单2 ORD-2024-002 待付款
INSERT INTO `biz_order_logistics` (`order_id`, `title`, `description`, `location`, `event_time`, `state`, `sort_order`) VALUES
(2, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-16 14:20:00', 'done', 1),
(2, '待付款', '等待买家完成付款', '上海市浦东新区华东仓储中心 · 备货区', NULL, 'active', 2),
(2, '商家发货', '等待商家发货', '上海市浦东新区华东仓储中心（金科路288号）', NULL, 'wait', 3),
(2, '确认收货', '等待买家确认收货', '北京市海淀区中关村大街1号海龙大厦15层', NULL, 'wait', 4);

-- 订单3 ORD-2024-003 已完成
INSERT INTO `biz_order_logistics` (`order_id`, `title`, `description`, `location`, `event_time`, `state`, `sort_order`) VALUES
(3, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-17 09:15:00', 'done', 1),
(3, '付款成功', '买家已付款，等待发货', '上海市浦东新区华东仓储中心 · 备货区', '2024-01-17 09:30:00', 'done', 2),
(3, '快件已揽收', '快递已从仓库发出', '上海市浦东新区华东揽收站（张江镇）', '2024-01-18 09:15:00', 'done', 3),
(3, '运输中', '快件途经转运中心', '浙江省杭州市萧山转运中心', '2024-01-18 15:00:00', 'done', 4),
(3, '到达派送站', '快件到达末端配送站', '杭州市配送站（西湖区）', '2024-01-19 08:00:00', 'done', 5),
(3, '已签收', '买家已确认收货', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', '2024-01-19 09:15:00', 'done', 6);

-- 订单4 ORD-2024-004 已完成（大额服务器采购）
INSERT INTO `biz_order_logistics` (`order_id`, `title`, `description`, `location`, `event_time`, `state`, `sort_order`) VALUES
(4, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-18 11:00:00', 'done', 1),
(4, '付款成功', '买家已付款，等待发货', '上海市浦东新区华东仓储中心 · 备货区', '2024-01-18 11:20:00', 'done', 2),
(4, '快件已揽收', '快递已从仓库发出', '上海市浦东新区华东揽收站（张江镇）', '2024-01-19 11:00:00', 'done', 3),
(4, '运输中', '快件途经转运中心', '上海市青浦区华新转运中心', '2024-01-19 20:00:00', 'done', 4),
(4, '到达派送站', '快件到达末端配送站', '杭州市配送站（西湖区）', '2024-01-20 09:00:00', 'done', 5),
(4, '已签收', '买家已确认收货', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', '2024-01-20 11:00:00', 'done', 6);

-- 订单5 ORD-2024-005 已取消
INSERT INTO `biz_order_logistics` (`order_id`, `title`, `description`, `location`, `event_time`, `state`, `sort_order`) VALUES
(5, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-19 16:45:00', 'done', 1),
(5, '订单已取消', '未付款，订单已关闭', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-19 17:15:00', 'failed', 2);

-- 订单6 ORD-2024-006 已完成
INSERT INTO `biz_order_logistics` (`order_id`, `title`, `description`, `location`, `event_time`, `state`, `sort_order`) VALUES
(6, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-20 08:30:00', 'done', 1),
(6, '付款成功', '买家已付款，等待发货', '上海市浦东新区华东仓储中心 · 备货区', '2024-01-20 08:45:00', 'done', 2),
(6, '快件已揽收', '快递已从仓库发出', '上海市浦东新区华东揽收站（张江镇）', '2024-01-21 08:30:00', 'done', 3),
(6, '运输中', '快件途经转运中心', '浙江省杭州市萧山转运中心', '2024-01-21 16:00:00', 'done', 4),
(6, '到达派送站', '快件到达末端配送站', '杭州市配送站（西湖区）', '2024-01-22 08:30:00', 'done', 5),
(6, '已签收', '买家已确认收货', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', '2024-01-22 08:30:00', 'done', 6);

-- 订单7 ORD-2024-007 待付款
INSERT INTO `biz_order_logistics` (`order_id`, `title`, `description`, `location`, `event_time`, `state`, `sort_order`) VALUES
(7, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-21 13:00:00', 'done', 1),
(7, '待付款', '等待买家完成付款', '上海市浦东新区华东仓储中心 · 备货区', NULL, 'active', 2),
(7, '商家发货', '等待商家发货', '上海市浦东新区华东仓储中心（金科路288号）', NULL, 'wait', 3),
(7, '确认收货', '等待买家确认收货', '北京市海淀区中关村大街1号海龙大厦15层', NULL, 'wait', 4);

-- 订单8 ORD-2024-008 已完成
INSERT INTO `biz_order_logistics` (`order_id`, `title`, `description`, `location`, `event_time`, `state`, `sort_order`) VALUES
(8, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-22 15:30:00', 'done', 1),
(8, '付款成功', '买家已付款，等待发货', '上海市浦东新区华东仓储中心 · 备货区', '2024-01-22 15:45:00', 'done', 2),
(8, '快件已揽收', '快递已从仓库发出', '上海市浦东新区华东揽收站（张江镇）', '2024-01-23 15:30:00', 'done', 3),
(8, '运输中', '快件途经转运中心', '浙江省杭州市萧山转运中心', '2024-01-23 22:00:00', 'done', 4),
(8, '到达派送站', '快件到达末端配送站', '杭州市配送站（西湖区）', '2024-01-24 09:00:00', 'done', 5),
(8, '已签收', '买家已确认收货', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', '2024-01-24 15:30:00', 'done', 6);

-- 订单9 ORD-2024-009 待付款
INSERT INTO `biz_order_logistics` (`order_id`, `title`, `description`, `location`, `event_time`, `state`, `sort_order`) VALUES
(9, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-23 10:00:00', 'done', 1),
(9, '待付款', '等待买家完成付款', '上海市浦东新区华东仓储中心 · 备货区', NULL, 'active', 2),
(9, '商家发货', '等待商家发货', '上海市浦东新区华东仓储中心（金科路288号）', NULL, 'wait', 3),
(9, '确认收货', '等待买家确认收货', '北京市海淀区中关村大街1号海龙大厦15层', NULL, 'wait', 4);

-- 订单10 ORD-2024-010 已完成
INSERT INTO `biz_order_logistics` (`order_id`, `title`, `description`, `location`, `event_time`, `state`, `sort_order`) VALUES
(10, '提交订单', '订单已提交，等待付款', '上海市浦东新区华东仓储中心（金科路288号）', '2024-01-24 09:00:00', 'done', 1),
(10, '付款成功', '买家已付款，等待发货', '上海市浦东新区华东仓储中心 · 备货区', '2024-01-24 09:15:00', 'done', 2),
(10, '快件已揽收', '快递已从仓库发出', '上海市浦东新区华东揽收站（张江镇）', '2024-01-25 09:00:00', 'done', 3),
(10, '运输中', '快件途经转运中心', '浙江省杭州市萧山转运中心', '2024-01-25 17:00:00', 'done', 4),
(10, '到达派送站', '快件到达末端配送站', '杭州市配送站（西湖区）', '2024-01-26 08:00:00', 'done', 5),
(10, '已签收', '买家已确认收货', '浙江省杭州市西湖区文三路478号华星科技大厦A座1201室', '2024-01-26 09:00:00', 'done', 6);
