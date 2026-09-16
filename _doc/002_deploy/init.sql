-- 创建Nacos数据库（若需单独指定）
CREATE
DATABASE IF NOT EXISTS z_config CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE
z_config;

-- 1. 配置管理核心表：主配置表
DROP TABLE IF EXISTS `z_config_info`;
CREATE TABLE IF NOT EXISTS `z_config_info`
(
    `id`
    bigint
(
    20
) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `data_id` varchar
(
    255
) NOT NULL COMMENT '配置ID',
    `group` varchar
(
    128
) DEFAULT NULL COMMENT '配置分组',
    `content` longtext NOT NULL COMMENT '配置内容',
    `md5` varchar
(
    32
) DEFAULT NULL COMMENT '内容MD5',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    `creator_staff_no` varchar
(
    255
) COMMENT '创建人工号',
    `creator_staff_nick_nm` varchar
(
    255
) COMMENT '创建人昵称',
    `creator_staff_real_nm` varchar
(
    255
) COMMENT '创建人真名',
    `source_ip` varchar
(
    50
) DEFAULT NULL COMMENT '创建IP',
    `app_name` varchar
(
    128
) DEFAULT NULL COMMENT '应用名',
    `namespace` varchar
(
    128
) DEFAULT '' COMMENT '命名空间（多租户隔离）',
    `config_desc` varchar
(
    256
) DEFAULT NULL COMMENT '配置描述',
    `config_usage` varchar
(
    64
) DEFAULT NULL COMMENT '使用说明',
    `config_enable_rule` varchar
(
    64
) DEFAULT NULL COMMENT '生效规则',
    `config_type` varchar
(
    64
) DEFAULT NULL COMMENT '配置类型（如properties、yaml）',
    `config_schema` text COMMENT '配置JSON schema',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_z_config_info_data_group_namespace`
(
    `data_id`,
    `group`,
    `namespace`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主配置表';

-- 2. 配置管理：Beta环境配置表
DROP TABLE IF EXISTS `z_config_info_beta`;
CREATE TABLE IF NOT EXISTS `z_config_info_beta`
(
    `id`
    bigint
(
    20
) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `data_id` varchar
(
    255
) NOT NULL COMMENT '配置ID',
    `group` varchar
(
    128
) DEFAULT NULL COMMENT '配置分组',
    `app_name` varchar
(
    128
) DEFAULT NULL COMMENT '应用名',
    `content` longtext NOT NULL COMMENT '配置内容',
    `beta_ips` varchar
(
    1024
) DEFAULT NULL COMMENT 'Beta环境IP列表',
    `md5` varchar
(
    32
) DEFAULT NULL COMMENT '内容MD5',
    `gmt_create` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    `src_user` text COMMENT '创建人',
    `src_ip` varchar
(
    50
) DEFAULT NULL COMMENT '创建IP',
    `namespace` varchar
(
    128
) DEFAULT '' COMMENT '命名空间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_z_configinfobeta_datagroupnamespace`
(
    `data_id`,
    `group`,
    `namespace`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Beta环境配置表';

-- 3. 配置管理：配置历史表
DROP TABLE IF EXISTS `z_config_info_history`;
CREATE TABLE IF NOT EXISTS `z_config_info_history`
(
    `id`
    bigint
(
    20
) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `nid` bigint
(
    20
) NOT NULL COMMENT '配置历史ID',
    `data_id` varchar
(
    255
) NOT NULL COMMENT '配置ID',
    `group` varchar
(
    128
) DEFAULT NULL COMMENT '配置分组',
    `app_name` varchar
(
    128
) DEFAULT NULL COMMENT '应用名',
    `content` longtext NOT NULL COMMENT '配置内容',
    `md5` varchar
(
    32
) DEFAULT NULL COMMENT '内容MD5',
    `gmt_create` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    `src_user` text COMMENT '操作人',
    `src_ip` varchar
(
    50
) DEFAULT NULL COMMENT '操作IP',
    `op_type` char
(
    10
) DEFAULT NULL COMMENT '操作类型（新增/修改/删除）',
    `namespace` varchar
(
    128
) DEFAULT '' COMMENT '命名空间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_z_gmt_modified`
(
    `gmt_modified`
),
    KEY `idx_z_did`
(
    `data_id`,
    `group`,
    `namespace`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置历史版本表';

-- 4. 服务发现：服务表
DROP TABLE IF EXISTS `z_service_info`;
CREATE TABLE IF NOT EXISTS `z_service_info`
(
    `id`
    bigint
(
    20
) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `service_name` varchar
(
    255
) NOT NULL COMMENT '服务名（格式：group@@name）',
    `group` varchar
(
    128
) DEFAULT NULL COMMENT '服务分组',
    `namespace` varchar
(
    128
) DEFAULT '' COMMENT '命名空间ID',
    `cluster_map` text COMMENT '集群映射（JSON格式）',
    `cache_millis` int
(
    10
) DEFAULT 10000 COMMENT '缓存毫秒数',
    `health_check_mode` varchar
(
    50
) DEFAULT NULL COMMENT '健康检查模式',
    `health_check_timeout` int
(
    10
) DEFAULT NULL COMMENT '健康检查超时时间',
    `ip_delete_timeout` int
(
    10
) DEFAULT 30000 COMMENT 'IP删除超时时间',
    `gmt_create` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_z_serviceinfo_servicename_namespaceid`
(
    `service_name`,
    `namespace`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务元数据表';

-- 5. 服务发现：实例表
DROP TABLE IF EXISTS `z_instance`;
CREATE TABLE IF NOT EXISTS `z_instance`
(
    `id`
    bigint
(
    20
) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `service_id` bigint
(
    20
) NOT NULL COMMENT '关联z_service_info.id',
    `instance_id` varchar
(
    255
) NOT NULL COMMENT '实例唯一ID（格式：serviceId@@ip:port）',
    `ip` varchar
(
    64
) NOT NULL COMMENT '实例IP',
    `port` int
(
    11
) NOT NULL COMMENT '实例端口',
    `weight` double
(
    10,
    2
) DEFAULT 1.0 COMMENT '权重',
    `healthy` tinyint
(
    1
) DEFAULT 1 COMMENT '健康状态（1=健康，0=不健康）',
    `enabled` tinyint
(
    1
) DEFAULT 1 COMMENT '是否启用（1=启用，0=禁用）',
    `ephemeral` tinyint
(
    1
) DEFAULT 1 COMMENT '是否临时实例',
    `cluster_name` varchar
(
    128
) DEFAULT 'DEFAULT' COMMENT '集群名',
    `metadata` text COMMENT '元数据（JSON格式）',
    `gmt_create` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_z_service_id`
(
    `service_id`
),
    KEY `idx_z_ip_port`
(
    `ip`,
    `port`
),
    KEY `uk_z_instanceid`
(
    `instance_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务实例表';

-- 6. 服务发现：集群表
DROP TABLE IF EXISTS `z_cluster`;
CREATE TABLE IF NOT EXISTS `z_cluster`
(
    `id`
    bigint
(
    20
) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `service_id` bigint
(
    20
) NOT NULL COMMENT '关联z_service_info.id',
    `name` varchar
(
    128
) NOT NULL COMMENT '集群名',
    `health_check_type` varchar
(
    50
) DEFAULT NULL COMMENT '健康检查类型',
    `health_check_url` varchar
(
    512
) DEFAULT NULL COMMENT '健康检查URL',
    `health_check_interval` int
(
    10
) DEFAULT 5000 COMMENT '健康检查间隔（毫秒）',
    `metadata` text COMMENT '集群元数据',
    `gmt_create` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_z_cluster_serviceid_name`
(
    `service_id`,
    `name`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集群表';

CREATE TABLE IF NOT EXISTS `z_subscription`
(
    `id`
    bigint
(
    20
) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `consumer_instance_id` varchar
(
    255
) NOT NULL COMMENT '消费实例ID（同instance_id格式）',
    `consumer_ip` varchar
(
    64
) NOT NULL COMMENT '消费实例IP',
    `consumer_port` int
(
    11
) NOT NULL COMMENT '消费实例端口',
    `subscribe_service_id` bigint
(
    20
) NOT NULL COMMENT '订阅的服务ID（关联z_service_info.id）',
    `subscribe_namespace` varchar
(
    128
) DEFAULT '' COMMENT '订阅服务的命名空间',
    `subscribe_cluster` varchar
(
    128
) DEFAULT 'DEFAULT' COMMENT '订阅的集群名',
    `subscribe_time` datetime NOT NULL COMMENT '订阅时间',
    `unsubscribe_time` datetime DEFAULT NULL COMMENT '取消订阅时间',
    `status` tinyint
(
    1
) DEFAULT 1 COMMENT '订阅状态（1=有效，0=取消）',
    `metadata` text COMMENT '消费端自定义元数据（JSON格式）',
    `gmt_create` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_consumer_service`
(
    `consumer_instance_id`,
    `subscribe_service_id`
),
    KEY `idx_subscribe_serviceid`
(
    `subscribe_service_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费实例订阅关系表';

-- 7. 权限控制：用户表
DROP TABLE IF EXISTS `z_users`;
CREATE TABLE IF NOT EXISTS `z_users`
(
    `username`
    varchar
(
    50
) NOT NULL COMMENT '用户名',
    `password` varchar
(
    500
) NOT NULL COMMENT '密码（BCrypt加密）',
    `enabled` tinyint
(
    1
) NOT NULL DEFAULT 1 COMMENT '是否启用（1=启用，0=禁用）',
    PRIMARY KEY
(
    `username`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 8. 权限控制：角色表
DROP TABLE IF EXISTS `z_roles`;
CREATE TABLE IF NOT EXISTS `z_roles`
(
    `username`
    varchar
(
    50
) NOT NULL COMMENT '关联z_users.username',
    `role` varchar
(
    50
) NOT NULL COMMENT '角色名',
    PRIMARY KEY
(
    `username`,
    `role`
),
    KEY `idx_z_username`
(
    `username`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色映射表';

-- 9. 权限控制：权限表
DROP TABLE IF EXISTS `z_permissions`;
CREATE TABLE IF NOT EXISTS `z_permissions`
(
    `role`
    varchar
(
    50
) NOT NULL COMMENT '关联z_roles.role',
    `resource` varchar
(
    255
) NOT NULL COMMENT '资源标识（如配置data_id:group）',
    `action` varchar
(
    8
) NOT NULL COMMENT '权限操作（read/write/delete）',
    PRIMARY KEY
(
    `role`,
    `resource`,
    `action`
),
    KEY `idx_z_role`
(
    `role`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限映射表';

-- 初始化默认用户（用户名：nacos，密码：nacos，BCrypt加密后的值）
INSERT
IGNORE INTO `z_users` (`username`, `password`, `enabled`)
VALUES ('nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', 1);

-- 初始化默认角色（nacos用户关联管理员角色）
INSERT
IGNORE INTO `z_roles` (`username`, `role`)
VALUES ('nacos', 'ROLE_ADMIN');