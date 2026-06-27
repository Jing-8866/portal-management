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
