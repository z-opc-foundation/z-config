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
    `encrypted_data_key` varchar(128) DEFAULT NULL COMMENT '加密数据密钥（RSA加密后的AES密钥，用于cipher-前缀配置的加密存储）',
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

-- 10. 配置管理：独立命名空间表（对齐 Nacos 的 Namespace 管理）
DROP TABLE IF EXISTS `z_namespace`;
CREATE TABLE IF NOT EXISTS `z_namespace`
(
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id` varchar(128) NOT NULL COMMENT '命名空间ID（唯一标识，如 dev/test/prod）',
    `namespace_name` varchar(128) NOT NULL COMMENT '命名空间名称（显示名，如 开发环境/测试环境）',
    `namespace_desc` varchar(512) DEFAULT NULL COMMENT '命名空间描述',
    `config_count` int(11) DEFAULT 0 COMMENT '该命名空间下的配置数量',
    `max_config_count` int(11) DEFAULT 200 COMMENT '最大配置数量限制（对齐 Nacos 容量管理）',
    `enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用（1=启用，0=禁用）',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_z_namespace_id` (`namespace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='命名空间管理表（对齐 Nacos Namespace）';

-- 初始化默认命名空间（对齐 Nacos 的 public 命名空间）
INSERT IGNORE INTO `z_namespace` (`namespace_id`, `namespace_name`, `namespace_desc`, `config_count`, `max_config_count`)
VALUES ('DEFAULT_NAMESPACE', '默认命名空间', '系统默认命名空间，未指定 namespace 时使用', 0, 10000);

-- 11. 配置管理：审计日志表（对齐 Nacos 的审计系统）
DROP TABLE IF EXISTS `z_config_audit`;
CREATE TABLE IF NOT EXISTS `z_config_audit`
(
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `data_id` varchar(255) NOT NULL COMMENT '配置ID',
    `group_name` varchar(128) DEFAULT NULL COMMENT '配置分组',
    `namespace` varchar(128) DEFAULT '' COMMENT '命名空间',
    `action` varchar(32) NOT NULL COMMENT '操作类型（CREATE/UPDATE/DELETE/ROLLBACK/IMPORT/EXPORT/CLONE）',
    `old_content` longtext COMMENT '变更前内容',
    `new_content` longtext COMMENT '变更后内容',
    `old_md5` varchar(32) DEFAULT NULL COMMENT '变更前MD5',
    `new_md5` varchar(32) DEFAULT NULL COMMENT '变更后MD5',
    `src_user` varchar(128) DEFAULT NULL COMMENT '操作人',
    `src_ip` varchar(50) DEFAULT NULL COMMENT '操作IP',
    `result` varchar(16) DEFAULT 'SUCCESS' COMMENT '操作结果（SUCCESS/FAILED）',
    `error_msg` varchar(1024) DEFAULT NULL COMMENT '错误信息',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_z_audit_dataid` (`data_id`, `group_name`, `namespace`),
    KEY `idx_z_audit_time` (`gmt_create`),
    KEY `idx_z_audit_user` (`src_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置审计日志表（对齐 Nacos 审计系统）';

-- 12. 配置管理：配置标签表（对齐 Nacos 的 Tag 管理）
DROP TABLE IF EXISTS `z_config_tag`;
CREATE TABLE IF NOT EXISTS `z_config_tag`
(
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tag_name` varchar(128) NOT NULL COMMENT '标签名称',
    `tag_desc` varchar(512) DEFAULT NULL COMMENT '标签描述',
    `tag_color` varchar(32) DEFAULT '#1890ff' COMMENT '标签颜色',
    `config_count` int(11) DEFAULT 0 COMMENT '关联配置数量',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_z_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置标签表（对齐 Nacos Tag 管理）';

-- 初始化默认标签
INSERT IGNORE INTO `z_config_tag` (`tag_name`, `tag_desc`, `tag_color`) VALUES
('生产环境', '生产环境配置', '#f5222d'),
('测试环境', '测试环境配置', '#fa8c16'),
('开发环境', '开发环境配置', '#52c41a'),
('敏感配置', '包含敏感信息的配置', '#722ed1');

-- 13. 配置管理：推送通知历史表（对齐 Nacos 的推送轨迹）
DROP TABLE IF EXISTS `z_config_push_history`;
CREATE TABLE IF NOT EXISTS `z_config_push_history`
(
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `data_id` varchar(255) NOT NULL COMMENT '配置ID',
    `group_name` varchar(128) DEFAULT NULL COMMENT '配置分组',
    `namespace` varchar(128) DEFAULT '' COMMENT '命名空间',
    `client_ip` varchar(128) NOT NULL COMMENT '客户端IP',
    `push_type` varchar(32) NOT NULL COMMENT '推送类型（CONFIG_CHANGE/HEARTBEAT_TIMEOUT）',
    `push_result` varchar(16) DEFAULT 'SUCCESS' COMMENT '推送结果（SUCCESS/FAILED/TIMEOUT）',
    `new_md5` varchar(32) DEFAULT NULL COMMENT '推送的配置MD5',
    `push_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
    PRIMARY KEY (`id`),
    KEY `idx_z_push_dataid` (`data_id`, `group_name`, `namespace`),
    KEY `idx_z_push_time` (`push_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置推送通知历史表（对齐 Nacos 推送轨迹）';

-- 14. 权限控制：命名空间权限表（对齐 Nacos 的命名空间级 RBAC）
DROP TABLE IF EXISTS `z_namespace_permission`;
CREATE TABLE IF NOT EXISTS `z_namespace_permission`
(
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `namespace_id` varchar(128) NOT NULL COMMENT '命名空间ID',
    `permission` varchar(32) NOT NULL DEFAULT 'read' COMMENT '权限类型（read/write/admin）',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_z_ns_perm` (`username`, `namespace_id`, `permission`),
    KEY `idx_z_ns_perm_user` (`username`),
    KEY `idx_z_ns_perm_ns` (`namespace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='命名空间权限表（对齐 Nacos 命名空间级 RBAC）';

-- 15. 配置管理：配置标签关联表（对齐 Nacos 的 Tag 灰度发布）
DROP TABLE IF EXISTS `z_config_tag_relation`;
CREATE TABLE IF NOT EXISTS `z_config_tag_relation`
(
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `data_id` varchar(255) NOT NULL COMMENT '配置ID',
    `group_name` varchar(128) DEFAULT NULL COMMENT '配置分组',
    `namespace` varchar(128) DEFAULT '' COMMENT '命名空间',
    `tag_name` varchar(128) NOT NULL COMMENT '标签名称',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_z_config_tag` (`data_id`, `group_name`, `namespace`, `tag_name`),
    KEY `idx_z_config_tag_dataid` (`data_id`, `group_name`, `namespace`),
    KEY `idx_z_config_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置标签关联表（对齐 Nacos Tag 灰度发布）';

-- 初始化默认管理员对所有命名空间的权限
INSERT IGNORE INTO `z_namespace_permission` (`username`, `namespace_id`, `permission`)
VALUES ('admin', 'DEFAULT_NAMESPACE', 'admin');

-- 初始化默认用户（用户名：nacos，密码：nacos，BCrypt加密后的值）
INSERT
IGNORE INTO `z_users` (`username`, `password`, `enabled`)
VALUES ('nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', 1);

-- 初始化默认角色（nacos用户关联管理员角色）
INSERT
IGNORE INTO `z_roles` (`username`, `role`)
VALUES ('nacos', 'ROLE_ADMIN');