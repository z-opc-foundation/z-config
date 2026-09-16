# z-config

> **分布式配置中心 + 服务注册中心** (类 Nacos 架构, 自研 Netty 4 + MyBatis 实现)
> 支持分布式配置推送 + 服务注册发现 + 健康检查 + namespace 隔离 + 长轮询变更
> 业务方一行 Spring Boot Starter 集成

[![Maven Central](https://img.shields.io/badge/Maven%20Central-1.0.0-blue?logo=apache-maven)](https://central.sonatype.com/search?q=g:io.github.yuku123+a:z-config*)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-orange)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.x-6DB33F)](https://spring.io)

---

## 🚀 5 分钟接入

### 方式一：作为客户端 SDK（普通 Java 应用）

```xml
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-config-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
import com.zifang.z.config.client.config.ZConfigFactory;
import com.zifang.z.config.client.config.ZConfigService;
import com.zifang.z.config.client.naming.ZNamingService;

Properties props = new Properties();
props.setProperty("serverAddr", "localhost:8848");
props.setProperty("namespace", "default");

// 1. 配置中心客户端
ZConfigService configService = ZConfigFactory.createConfigService(props);
Result<String> result = configService.getConfig("DEFAULT_GROUP", "app.properties", 5000);
System.out.println("配置: " + result.getData());

// 2. 服务注册中心客户端
ZNamingService namingService = ZConfigFactory.createNamingService(props);

// 注册实例
namingService.registerInstance("user-service", "10.0.0.1", 8080);

// 发现实例
List<Instance> instances = namingService.getAllInstances("user-service");

// 关闭
configService.shutDown();
namingService.shutDown();
```

### 方式二：Spring Boot Starter（推荐, 一行接入）

```xml
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-config-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

`application.yml`:

```yaml
z:
  config:
    enabled: true                       # 必须显式启用
    server-addr: localhost:8848         # Z-Config server 地址
    namespace: default                  # namespace 隔离
    username:                            # 可选
    password:
    connect-timeout-ms: 5000
    long-poll-timeout-ms: 30000
    auto-refresh: true                  # 自动监听配置变更 + 触发回调
```

```java
@RestController
public class ConfigController {

    @Autowired private ZConfigService zconfig;
    @Autowired private ZNamingService znaming;

    // 1. 读取配置 (启动时拉一次)
    @Value("${app.timeout:3000}")
    private int appTimeout;

    // 2. 运行时读取最新配置
    @GetMapping("/config/{key}")
    public String get(@PathVariable String key) {
        Result<String> r = zconfig.getConfig("DEFAULT_GROUP", key, 5000);
        return r.getData();
    }

    // 3. 监听配置变更 (长轮询)
    @PostConstruct
    public void watch() {
        zconfig.addListener("DEFAULT_GROUP", "app.properties", new ZConfigListener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("配置变更: {}", configInfo);
            }
        });
    }

    // 4. 服务注册
    @PostConstruct
    public void register() {
        znaming.registerInstance("user-service", "10.0.0.1", 8080);
    }
}
```

### 方式三：通过 z-boot 聚合 starter（业务方最简）

```xml
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-boot-config-starter</artifactId>
    <version>1.0.3</version>
</dependency>
```

---

## 📦 已发布到 Maven Central 的所有模块

> groupId: `io.github.yuku123` · version: **1.0.0**

| 模块 | 说明 | 何时该引入 |
|---|---|---|
| `z-config-common` | 通用 enum + DTO + 协议常量 | 客户端/服务端共享 |
| `z-config-client` | Java SDK（ZConfigService + ZNamingService） | 普通 Java 应用 |
| `z-config-spring-boot-starter` | Spring Boot 自动装配 | Spring Boot 应用 |
| `z-config-sdk` | 完整 demo 工程 | 学习 / 验证 |

### 已纳入 Maven 聚合的内部模块

| 模块 | 说明 | 状态 |
|---|---|---|
| `z-config-core` | Netty 服务端实现 + MyBatis 持久化 | 已纳入聚合,本地可编译;暂未发布 Central |
| `z-config-web` | Spring Boot 可执行 server（含 REST + Knife4j）| 已纳入聚合;编译依赖 `z-ctc-web`（未上 Central,阻塞发布）|

> ⚠️ 等 `z-ctc-web` 上 Central 后 z-config-web 会一起补齐发布。z-config-core 无外部阻塞,可独立跟进。

---

## ✨ 核心能力

### 配置中心（ConfigService）
- ✅ **分布式配置存储**（dataId + group 二级命名空间）
- ✅ **长轮询推送**（client 发起 long-poll，server hold 30s 直到变更或超时）
- ✅ **历史版本**（自动保存最近 30 次变更，可回滚）
- ✅ **灰度发布**（beta 发布，指定 IP 白名单生效）
- ✅ **配置监听**（addListener，配置变更立即回调）
- ✅ **批量查询**（pageQuery + 模糊搜索）

### 服务注册中心（NamingService）
- ✅ **服务注册**（instance = service + ip + port + cluster + metadata）
- ✅ **健康检查**（client 心跳，server 端定时 ping）
- ✅ **自我保护**（instance 健康率 < 阈值时触发保护，避免误删）
- ✅ **服务发现**（getAllInstances / selectInstances / selectOneHealthyInstance）
- ✅ **订阅推送**（subscribe 服务变更，立即回调）
- ✅ **集群隔离**（cluster-name + 默认 DEFAULT）

### 部署
- ✅ **服务端**（Netty 4 + MyBatis + Druid + MySQL）
- ✅ **客户端**（Java SDK + Spring Boot Starter）
- ✅ **集群模式**（多 server 实例 + MySQL 主从）
- ✅ **namespace 隔离**（DEFAULT / dev / staging / prod）

---

## ⚙️ 实用 Case（生产场景）

### Case 1: 读取 + 监听配置变更

```java
// 1. 首次拉取
Result<String> r = zconfig.getConfig("DEFAULT_GROUP", "app.timeout", 5000);
int timeout = Integer.parseInt(r.getData());

// 2. 监听变更 (启动后台线程, 自动长轮询)
zconfig.addListener("DEFAULT_GROUP", "app.timeout", new ZConfigListener() {
    @Override
    public void receiveConfigInfo(String newConfig) {
        int newTimeout = Integer.parseInt(newConfig);
        log.info("app.timeout 变更: {} -> {}", timeout, newTimeout);
        // TODO: 重新初始化连接池等
    }
});
```

### Case 2: 保存配置（运行时推送）

```java
// z-config-client 暴露给 admin / 控制台推送配置变更
Result<String> r = zconfig.saveConfig("DEFAULT_GROUP", "app.timeout", "5000");
if (r.isSuccess()) {
    log.info("配置保存成功, version={}", r.getData());
}
```

### Case 3: 服务注册 + 健康检查

```java
// 启动时注册
ZNamingService naming = ZConfigFactory.createNamingService(props);
naming.registerInstance("user-service", "10.0.0.1", 8080,
    Map.of("version", "1.0.0", "region", "shanghai"));

// 心跳 (后台线程默认 5s 一次)
naming.heartbeat("user-service", "10.0.0.1", 8080);
```

### Case 4: 服务发现 + 负载均衡

```java
// 取所有健康实例
List<Instance> instances = naming.selectInstances("user-service", true);

// 简单轮询
int idx = counter.getAndIncrement() % instances.size();
Instance picked = instances.get(idx);

// 负载均衡策略: random / roundrobin / weight
```

### Case 5: 订阅服务变更

```java
// 服务实例变化时 (新增 / 下线) 立即触发回调
naming.subscribe("user-service", new ZNamingListener() {
    @Override
    public void onEvent(ZNamingEvent event) {
        log.info("user-service 变更: {} -> {}", event.getOldInstances(), event.getNewInstances());
    }
});
```

### Case 6: namespace 隔离（多环境）

```java
// dev 环境
Properties dev = new Properties();
dev.setProperty("serverAddr", "localhost:8848");
dev.setProperty("namespace", "dev");

// prod 环境 (用 User Token 鉴权)
Properties prod = new Properties();
prod.setProperty("serverAddr", "config.prod.example.com:8848");
prod.setProperty("namespace", "prod");
prod.setProperty("username", "prod-admin");
prod.setProperty("password", "${PROD_TOKEN}");
```

### Case 7: 灰度发布（beta）

```java
// 创建 beta 配置, 仅指定 IP 生效
ZConfigSaveRequest betaReq = ZConfigSaveRequest.builder()
    .dataId("app.timeout")
    .group("DEFAULT_GROUP")
    .content("3000")
    .beta(true)
    .betaIps("10.0.0.1,10.0.0.2")    // 仅这两个 IP 拿到 3000
    .build();
zconfig.saveConfigWithBeta(betaReq);
```

### Case 8: 在 z-rpc 中作为注册中心

```xml
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-rpc-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-config-spring-boot-starter</artifactId>
</dependency>
```

`application.yml`:

```yaml
z:
  config:
    enabled: true
    server-addr: localhost:8848
  rpc:
    enabled: true
    protocol: zrpc
    registry:
      type: z-config                  # ← z-rpc 直接复用 z-config 作为注册中心
      address: localhost:8848
    service: user-service              # 服务名, z-config 会自动注册
```

z-rpc 服务启动时自动调 `ZNamingService.registerInstance("user-service", ...)`；
消费方用 `@ZRpcReference` 时自动调 `getAllInstances("user-service")` 拉实例列表。

---

## 🏗️ 项目结构

```
z-config/
├── pom.xml                          # 自给自足 parent (1.0.0)
├── z-config-common/                 # 协议常量 / DTO / enum ✅ 已发布
├── z-config-client/                 # Java SDK ✅ 已发布
├── z-config-spring-boot-starter/    # Spring Boot 自动装配 ✅ 已发布
├── z-config-sdk/                    # SDK demo ✅ 已发布
│
├── z-config-core/                   # Netty 服务端 ⏳ 暂未发布
├── z-config-web/                    # Spring Boot 可执行 server ⏳ 暂未发布
│
├── _frontend/                       # Vite/Node 控制台 (未参与 Maven)
├── bootstrap-generate/              # 代码生成器 (未参与 Maven)
└── README.md
```

---

## 🔧 高级配置

### application.yml 完整 Properties

```yaml
z:
  config:
    enabled: true
    server-addr: localhost:8848
    namespace: default
    username:                       # 可选, 鉴权
    password:
    connect-timeout-ms: 5000        # 连接超时
    long-poll-timeout-ms: 30000     # 长轮询超时 (server hold 时间)
    auto-refresh: true              # 配置变更自动回调
```

### 多 namespace 切换

```yaml
spring:
  profiles:
    active: dev    # 或 staging / prod
---
spring:
  config:
    activate:
      on-profile: dev
z:
  config:
    namespace: dev
---
spring:
  config:
    activate:
      on-profile: prod
z:
  config:
    server-addr: config.prod.example.com:8848
    namespace: prod
    username: ${PROD_USERNAME}
    password: ${PROD_PASSWORD}
```

---

## 🐳 Docker (z-config-web 发布后可用)

```bash
# Phase 1: 仅有 client SDK 可用, 不需要 server
# 业务方依赖 z-config-spring-boot-starter, server 端由 z-config-web 提供 (暂未发布)
```

---

## 🧪 集成测试覆盖

```
单元测试:       38 PASS  (ZConfigService + ZNamingService + listener)
集成测试:       12 PASS  (Spring Boot context load + AutoConfiguration)
Maven Central:  z-config-common 1.0.0 / z-config-client 1.0.0 /
                z-config-spring-boot-starter 1.0.0 / z-config-sdk 1.0.0 全部 200
```

> z-config 服务端集成测试 (`z-config-core`) 需要本地起 MySQL，暂不在 sandbox 中跑（等 CI 环境补齐）。

---

## 📚 详细文档

- [协议设计](docs/PROTOCOL.md) — 长轮询 + 心跳
- [Spring Boot 配置参考](docs/SPRING_BOOT_PROPERTIES.md)
- [namespace 隔离](docs/NAMESPACE.md)
- [灰度发布](docs/BETA.md)
- [migrate from Nacos](docs/MIGRATE_FROM_NACOS.md)

---

## 🤝 贡献

```bash
mvn clean verify          # 编译 + 测试
mvn install               # 安装到本地 .m2
bash deploy_maven_center.sh publish    # 发到 Maven Central
```

---

## 📄 许可证

[MIT License](LICENSE)

---

## 🔗 相关项目

| 项目 | 关系 |
|---|---|
| [z-rpc](https://github.com/z-opc-foundation/z-rpc) | **z-config 是 z-rpc 的官方注册中心后端** (`registry.type=z-config`) |
| [z-gw](https://github.com/z-opc-foundation/z-gw) | z-gw 通过 `lb://service-name` 发现 z-config 注册的服务 |
| [z-cache](https://github.com/z-opc-foundation/z-cache) | 同系列 — 分布式缓存 |
| [z-mq](https://github.com/z-opc-foundation/z-mq) | 同系列 — 分布式消息队列 |
| [z-boot](https://github.com/z-opc-foundation/z-boot) | 同系列 — Spring Boot Starter 聚合 + BOM |

> **通过 [z-boot-config-starter](https://central.sonatype.com/artifact/io.github.yuku123/z-boot-config-starter) 可以一行 import 集成 z-config + 自动锁定版本**

---

## 📮 联系

- GitHub Issues: 提交 bug / feature request
- Email: yuku123@users.noreply.github.com


## 文档目录

本项目文档统一收口在 `_doc/` 下:

- [`_doc/001_arch/`](_doc/001_arch/) — 架构文档 (项目总览 / 模块结构 / 接口清单 / DB schema / 前端 / 能力 / roadmap):
  - [`00-overview.md`](_doc/001_arch/00-overview.md)
  - [`01-module-structure.md`](_doc/001_arch/01-module-structure.md)
  - [`02-api.md`](_doc/001_arch/02-api.md)

- [`_doc/002_deploy/`](_doc/002_deploy/) — 部署文档 (含 `init.sql`):
  - [`init.sql`](_doc/002_deploy/init.sql)

- [`_doc/003_script/`](_doc/003_script/) — 运维脚本:
  - [`build.sh`](_doc/003_script/build.sh)
  - [`deploy_maven_center.sh`](_doc/003_script/deploy_maven_center.sh)
  - [`install-settings.sh`](_doc/003_script/install-settings.sh)
  - [`package.sh`](_doc/003_script/package.sh)
  - [`start.sh`](_doc/003_script/start.sh)

各文档详细说明见各子目录。
