-- =====================================================
-- z-config 数据库升级脚本（对齐 Nacos 功能改进）
-- 版本：2.0
-- 日期：2026-09-18
-- 说明：包含所有新增表和字段变更
-- =====================================================

-- 使用目标数据库
USE z_config;

-- =====================================================
-- 1. 字段变更
-- =====================================================

-- 1.1 主配置表新增加密密钥字段
ALTER TABLE z_config_info ADD COLUMN IF NOT EXISTS `encrypted_data_key` varchar(128) DEFAULT NULL
COMMENT '加密数据密钥（RSA加密后的AES密钥，用于cipher-前缀配置的加密存储）';

-- =====================================================
-- 2. 新增表
-- =====================================================

-- 2.1 命名空间管理表（对齐 Nacos Namespace 管理）
CREATE TABLE IF NOT EXISTS `z_namespace` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id` varchar(128) NOT NULL COMMENT '命名空间ID（唯一标识，如 dev/test/prod）',
    `namespace_name` varchar(128) NOT NULL COMMENT '命名空间名称（显示名）',
    `namespace_desc` varchar(512) DEFAULT NULL COMMENT '命名空间描述',
    `config_count` int(11) DEFAULT 0 COMMENT '该命名空间下的配置数量',
    `max_config_count` int(11) DEFAULT 200 COMMENT '最大配置数量限制',
    `max_content_size` int(11) DEFAULT 102400 COMMENT '最大配置内容大小（字节），默认100KB',
    `enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_z_namespace_id` (`namespace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='命名空间管理表';

INSERT IGNORE INTO `z_namespace` (`namespace_id`, `namespace_name`, `namespace_desc`, `config_count`, `max_config_count`)
VALUES ('DEFAULT_NAMESPACE', '默认命名空间', '系统默认命名空间，未指定 namespace 时使用', 0, 10000);

-- 2.2 审计日志表（对齐 Nacos 审计系统）
CREATE TABLE IF NOT EXISTS `z_config_audit` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置审计日志表';

-- 2.3 配置标签表（对齐 Nacos Tag 管理）
CREATE TABLE IF NOT EXISTS `z_config_tag` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tag_name` varchar(128) NOT NULL COMMENT '标签名称',
    `tag_desc` varchar(512) DEFAULT NULL COMMENT '标签描述',
    `tag_color` varchar(32) DEFAULT '#1890ff' COMMENT '标签颜色',
    `config_count` int(11) DEFAULT 0 COMMENT '关联配置数量',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_z_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置标签表';

INSERT IGNORE INTO `z_config_tag` (`tag_name`, `tag_desc`, `tag_color`) VALUES
('生产环境', '生产环境配置', '#f5222d'),
('测试环境', '测试环境配置', '#fa8c16'),
('开发环境', '开发环境配置', '#52c41a'),
('敏感配置', '包含敏感信息的配置', '#722ed1');

-- 2.4 推送通知历史表（对齐 Nacos 推送轨迹）
CREATE TABLE IF NOT EXISTS `z_config_push_history` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置推送通知历史表';

-- 2.5 命名空间权限表（对齐 Nacos 命名空间级 RBAC）
CREATE TABLE IF NOT EXISTS `z_namespace_permission` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `namespace_id` varchar(128) NOT NULL COMMENT '命名空间ID',
    `permission` varchar(32) NOT NULL DEFAULT 'read' COMMENT '权限类型（read/write/admin）',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_z_ns_perm` (`username`, `namespace_id`, `permission`),
    KEY `idx_z_ns_perm_user` (`username`),
    KEY `idx_z_ns_perm_ns` (`namespace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='命名空间权限表';

INSERT IGNORE INTO `z_namespace_permission` (`username`, `namespace_id`, `permission`)
VALUES ('admin', 'DEFAULT_NAMESPACE', 'admin');

-- 2.6 配置标签关联表（对齐 Nacos Tag 灰度发布）
CREATE TABLE IF NOT EXISTS `z_config_tag_relation` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置标签关联表';

-- 2.7 配置审批表（对齐 Nacos 的配置审批工作流）
CREATE TABLE IF NOT EXISTS `z_config_approval` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `data_id` varchar(255) NOT NULL COMMENT '配置ID',
    `group_name` varchar(128) DEFAULT NULL COMMENT '配置分组',
    `namespace` varchar(128) DEFAULT '' COMMENT '命名空间',
    `action` varchar(32) NOT NULL COMMENT '操作类型（PUBLISH/DELETE/ROLLBACK）',
    `content` longtext COMMENT '待审批的配置内容',
    `md5` varchar(32) DEFAULT NULL COMMENT '内容MD5',
    `applicant` varchar(128) NOT NULL COMMENT '申请人',
    `applicant_ip` varchar(50) DEFAULT NULL COMMENT '申请人IP',
    `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '审批状态（PENDING/APPROVED/REJECTED）',
    `approver` varchar(128) DEFAULT NULL COMMENT '审批人',
    `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
    `remark` varchar(512) DEFAULT NULL COMMENT '审批备注',
    PRIMARY KEY (`id`),
    KEY `idx_z_approval_status` (`status`),
    KEY `idx_z_approval_dataid` (`data_id`, `group_name`, `namespace`),
    KEY `idx_z_approval_time` (`apply_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置审批表（对齐 Nacos 配置审批工作流）';

-- 2.8 国际化消息表（对齐 Nacos 的多语言支持）
CREATE TABLE IF NOT EXISTS `z_i18n_message` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `message_key` varchar(255) NOT NULL COMMENT '消息键',
    `language` varchar(32) NOT NULL DEFAULT 'zh_CN' COMMENT '语言代码',
    `namespace` varchar(128) DEFAULT '' COMMENT '命名空间',
    `message_value` text NOT NULL COMMENT '消息内容',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_z_i18n_key_lang` (`message_key`, `language`, `namespace`),
    KEY `idx_z_i18n_key` (`message_key`),
    KEY `idx_z_i18n_lang` (`language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国际化消息表';

-- 初始化默认国际化消息
INSERT IGNORE INTO `z_i18n_message` (`message_key`, `language`, `message_value`) VALUES
('config.save.success', 'zh_CN', '配置保存成功'),
('config.save.failed', 'zh_CN', '配置保存失败'),
('config.delete.success', 'zh_CN', '配置删除成功'),
('config.not.found', 'zh_CN', '配置不存在'),
('namespace.quota.exceeded', 'zh_CN', '命名空间配额已满'),
('config.content.too.large', 'zh_CN', '配置内容超出大小限制');

INSERT IGNORE INTO `z_i18n_message` (`message_key`, `language`, `message_value`) VALUES
('config.save.success', 'en_US', 'Configuration saved successfully'),
('config.save.failed', 'en_US', 'Configuration save failed'),
('config.delete.success', 'en_US', 'Configuration deleted successfully'),
('config.not.found', 'en_US', 'Configuration not found'),
('namespace.quota.exceeded', 'en_US', 'Namespace quota exceeded'),
('config.content.too.large', 'en_US', 'Configuration content exceeds size limit');

-- =====================================================
-- 完成
-- =====================================================
SELECT 'z-config 升级脚本执行完成' AS result;
