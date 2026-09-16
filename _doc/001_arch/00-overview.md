# 项目总览

## 1. 项目简介

z-config 是一个轻量级的配置管理与服务发现平台，类似于 Apollo、Nacos 的功能定位。该平台提供配置的集中管理、版本控制、灰度发布、多环境/多租户隔离，以及服务注册与发现能力。

**技术栈：** Spring Boot + MyBatis-Plus + Netty + Druid

## 2. 模块架构

```
z-config (父项目)
├── z-config-admin          # 管理后台应用（Spring Boot 启动入口）
├── z-config-web             # Web API 层（REST 接口）
├── z-config-core            # 核心业务逻辑层（服务层、实体类、Mapper）
├── z-config-common          # 公共模块（DTO、请求/响应模型）
├── z-config-client          # 客户端 SDK（供业务应用集成使用）
├── z-config-sdk             # SDK 封装层
└── z-config-spring-boot-starter  # Spring Boot 自动配置-starter
```

## 3. 核心功能模块

### 3.1 配置管理 (Config)

**主要实体：** `ZConfigInfo`

| 字段               | 说明          |
|------------------|-------------|
| dataId           | 配置 ID       |
| group            | 配置分组        |
| content          | 配置内容        |
| md5              | 内容 MD5 摘要   |
| namespace        | 命名空间（多租户隔离） |
| appName          | 应用名称        |
| configDesc       | 配置描述        |
| configUsage      | 使用说明        |
| configEnableRule | 生效规则（灰度策略）  |

**核心服务：** `ConfigService`

- `saveConfig` - 保存配置
- `getConfig` - 获取配置
- `pageConfig` - 分页查询配置
- `listConfig` - 列表查询配置
- `deleteConfig` - 删除配置

**历史版本：** `ZConfigInfoHistory` 支持配置变更历史记录

### 3.2 集群管理 (Cluster)

**主要实体：** `ZCluster`

### 3.3 服务发现 (Naming)

**主要实体：** `ZServiceInfo`、`ZInstance`

**核心服务：** `ZNamingService`

- 服务注册
- 服务发现
- 服务监听

### 3.4 用户权限

**实体：** `ZUsers`、`ZRoles`、`ZPermissions`

## 4. 客户端集成

客户端 SDK (`z-config-client`) 提供：

- `ZConfigService` - 配置订阅与获取
- `ZNamingService` - 服务注册与发现
- 监听机制：`ZConfigListener`、`ZNamingListener`
- 通信支持：HTTP / Netty

## 5. 技术特点

- **多租户隔离**：通过 namespace 实现租户级配置隔离
- **配置推送**：基于 Netty 长连接实现配置变更实时推送
- **Spring Boot 集成**：提供 `z-config-spring-boot-starter` 简化业务接入
- **API 文档**：集成 Knife4j 提供 Swagger API 文档

## 6. 项目结构规范

```
com.zifang.z.config
├── admin          # 管理后台模块
├── web            # Web API 模块
│   └── api        # REST Controller
│   └── config     # Web 配置类（拦截器、异常处理等）
├── core           # 核心业务模块
│   └── domain
│       ├── entity    # 实体类
│       ├── mapper    # MyBatis Mapper
│       └── service   # 业务服务接口与实现
│   └── service       # 核心服务接口
├── common         # 公共模块
│   └── model       # DTO 模型
└── client         # 客户端 SDK
```
