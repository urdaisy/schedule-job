-- 数据库操作：创建任务表 --
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