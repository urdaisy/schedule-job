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