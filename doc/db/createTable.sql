-- 数据库操作：创建表 --
CREATE TABLE `sys_job` (
   `id` bigint(20) PRIMARY KEY NOT NULL AUTO_INCREMENT COMMENT '主键id',
   `job_name` varchar(100) NOT NULL COMMENT '任务名称',
   `job_group` varchar(100) NOT NULL COMMENT '任务组名',
   `job_class_name` varchar(255) NOT NULL COMMENT '任务执行类全路径',
   `cron_expression` varchar(50) NOT NULL COMMENT 'Cron表达式',
   `description` varchar(500) DEFAULT NULL COMMENT '任务描述',
   `job_param` varchar(1000) DEFAULT NULL COMMENT '任务参数（JSON格式字符串）',
   `status` int(11) NOT NULL COMMENT '状态：0-暂停，1-运行中',
   `start_time` datetime DEFAULT NULL COMMENT '创建时间',
   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
   `last_execution_time` datetime DEFAULT NULL COMMENT '最后执行时间',
   UNIQUE KEY `uk_job_name_group` (`job_name`, `job_group`) COMMENT '任务名+组名唯一约束'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务信息表';

-- 数据库操作：创建日志记录表 --
CREATE TABLE `sys_job_log` (
   `id` bigint(20) UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '主键id',
   `job_id` bigint(20) NOT NULL COMMENT '任务ID',
   `trigger_time` datetime NOT NULL COMMENT '任务触发时间',
   `execute_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务开始执行时间',
   `duration` bigint(20) UNSIGNED DEFAULT 0 COMMENT '执行耗时(毫秒)',
   `status` TINYINT(4) NOT NULL COMMENT '状态：0-成功，1-失败',
   `error_msg` text DEFAULT NULL COMMENT '错误信息（失败时存储异常堆栈）',
   `retry_count` tinyint(4) DEFAULT 0 COMMENT '重试次数',
   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志创建时间',
   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '日志更新时间',
   `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
   INDEX `idx_job_id` (`job_id`) USING BTREE,
   INDEX `idx_trigger_time` (`trigger_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务日志表';

-- 数据库操作：权限控制表 --
CREATE TABLE `sys_job_user` (
     `id` bigint(20) PRIMARY KEY NOT NULL AUTO_INCREMENT COMMENT '主键id',
     `username` varchar(50) NOT NULL COMMENT '用户名称',
     `password` varchar(255) NOT NULL COMMENT '用户密码（加密存储）',
     `email` varchar(100) NOT NULL COMMENT '用户邮箱（支持用邮箱重置密码）',
     `role_id` bigint(20) NOT NULL COMMENT '关联角色id',
     `status` tinyint(1) DEFAULT 1 COMMENT '状态（0:禁用，1:启用）',
     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     UNIQUE KEY `uk_username` (`username`) COMMENT '用户名唯一约束',
     INDEX `idx_role_id` (`role_id`) USING BTREE COMMENT '角色id索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE `sys_job_role` (
    `id` bigint(20) PRIMARY KEY NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `role_name` varchar(50) NOT NULL COMMENT '角色名称',
    `role_code` varchar(50) NOT NULL COMMENT '角色编号',
    `description` varchar(500) DEFAULT NULL COMMENT '角色描述',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_role_code` (`role_code`) COMMENT '角色编号唯一约束'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE `sys_job_permission` (
    `id` bigint(20) PRIMARY KEY NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `permission_name` varchar(50) NOT NULL COMMENT '权限名称',
    `permission_code` varchar(100) NOT NULL COMMENT '权限编号',
    `resource` varchar(50) NOT NULL COMMENT '允许操作的资源',
    `action` varchar(50) NOT NULL COMMENT '允许操作的动作',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_permission_code` (`permission_code`) COMMENT '权限编号唯一约束'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE `sys_job_user_role` (
    `user_id` bigint(20) NOT NULL COMMENT '用户id',
    `role_id` bigint(20) NOT NULL COMMENT '角色id',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`user_id`, `role_id`),
    INDEX `idx_role_id` (`role_id`) USING BTREE COMMENT '角色id索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE `sys_job_role_permission` (
    `role_id` bigint(20) NOT NULL COMMENT '角色id',
    `permission_id` bigint(20) NOT NULL COMMENT '权限id',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`role_id`, `permission_id`),
    INDEX `idx_permission_id` (`permission_id`) USING BTREE COMMENT '权限id索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

CREATE TABLE `sys_job_user_session` (
    `id` bigint(20) PRIMARY KEY NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `user_id` bigint(20) NOT NULL COMMENT '用户id',
    `token` varchar(255) NOT NULL COMMENT 'token',
    `expire_time` datetime NOT NULL COMMENT 'token过期时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_token` (`token`) COMMENT 'token唯一约束',
    INDEX `idx_user_id` (`user_id`) USING BTREE COMMENT '用户id索引',
    INDEX `idx_expire_time` (`expire_time`) USING BTREE COMMENT '过期时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话表';

-- 数据库操作：告警配置表 --
CREATE TABLE `sys_job_alert_config` (
    `id` bigint(20) PRIMARY KEY NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `job_id` bigint(20) NOT NULL COMMENT '任务ID',
    `alert_type` varchar(50) NOT NULL COMMENT '告警类型（EMAIL等）',
    `receiver_email` varchar(500) NOT NULL COMMENT '接收人邮箱（多个邮箱用逗号分隔）',
    `enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用（0:禁用，1:启用）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_alert_config` (`job_id`, `alert_type`) COMMENT '任务ID+告警类型唯一约束',
    INDEX `idx_job_id` (`job_id`) USING BTREE COMMENT '任务ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警配置表';

-- 数据库操作：告警记录表 --
CREATE TABLE `sys_job_alert_record` (
    `id` bigint(20) PRIMARY KEY NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `job_id` bigint(20) NOT NULL COMMENT '任务ID',
    `job_log_id` bigint(20) NOT NULL COMMENT '任务日志ID',
    `alert_type` varchar(50) NOT NULL COMMENT '告警类型（EMAIL等）',
    `receiver` varchar(500) DEFAULT NULL COMMENT '告警接收人（多个接收人用逗号分隔）',
    `alert_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '告警状态：0-待发送、1-发送成功、2-发送失败',
    `alert_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
    `error_msg` text DEFAULT NULL COMMENT '告警失败错误信息（失败时存储异常堆栈）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_job_id` (`job_id`) USING BTREE COMMENT '任务ID索引',
    INDEX `idx_job_log_id` (`job_log_id`) USING BTREE COMMENT '任务日志ID索引',
    INDEX `idx_alert_time` (`alert_time`) USING BTREE COMMENT '告警时间索引',
    INDEX `idx_alert_status` (`alert_status`) USING BTREE COMMENT '告警状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- 数据库操作：插入权限数据 --
INSERT INTO sys_job_role(role_name, role_code, description) values ( 'Admin', '000', '管理员');
INSERT INTO sys_job_role(role_name, role_code, description) values ( 'User', '001', '普通用户');
INSERT INTO sys_job_permission(permission_name, permission_code, resource, action) values ('定时任务Create', '000', 'job', 'create');
INSERT INTO sys_job_permission(permission_name, permission_code, resource, action) values ('定时任务Update', '001', 'job', 'update');
INSERT INTO sys_job_permission(permission_name, permission_code, resource, action) values ('定时任务Query', '002', 'job', 'query');
INSERT INTO sys_job_permission(permission_name, permission_code, resource, action) values ('定时任务Delete', '003', 'job', 'delete');
INSERT INTO sys_job_permission(permission_name, permission_code, resource, action) values ('任务日志Query', '004', 'log', 'query');
INSERT INTO sys_job_permission(permission_name, permission_code, resource, action) values ('任务日志Delete', '005', 'log', 'delete');
-- 数据库操作：管理员支持对job进行CRUD，普通用户只能CRU不支持删除操作 --
INSERT INTO sys_job_role_permission(role_id, permission_id) values (1, 1);
INSERT INTO sys_job_role_permission(role_id, permission_id) values (1, 2);
INSERT INTO sys_job_role_permission(role_id, permission_id) values (1, 3);
INSERT INTO sys_job_role_permission(role_id, permission_id) values (1, 4);
INSERT INTO sys_job_role_permission(role_id, permission_id) values (1, 5);
INSERT INTO sys_job_role_permission(role_id, permission_id) values (1, 6);
INSERT INTO sys_job_role_permission(role_id, permission_id) values (2, 2);
INSERT INTO sys_job_role_permission(role_id, permission_id) values (2, 3);