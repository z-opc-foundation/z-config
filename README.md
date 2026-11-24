# z-config

> 分布式配置中心 + 服务注册中心 —— 类 Nacos 定位，自研 Netty 4 长连接推送 + MyBatis-Plus 持久化

一人公司基座里"配置"与"服务发现"这两件事的自研实现：配置按 `namespace + group + dataId` 三元组存储，
带历史版本、回滚、版本对比、`cipher-` 前缀自动加密、Beta 灰度读路径；服务侧提供实例注册/注销、健康标记、
按集群/分组查询与订阅变更。业务方引 `z-config-spring-boot-starter` 即可注入 `ZConfigService` / `ZNamingService`，
控制面由 React 控制台（`_frontend/`）经 REST 接口操作。

---

## 📋 基本信息

| 字段 | 值 |
|------|-----|
| **仓库** | `z-config`（origin: `github.com/z-opc-foundation/z-config`） |
| **Maven 坐标** | `io.github.yuku123:z-config:${revision}`（`packaging=pom`，CI-friendly versions + flatten `oss` 模式） |
| **当前版本** | `1.0.12`（根 POM `<revision>`；`mvn` 抬版本只改这一处） |
| **父项目** | `io.github.yuku123:z-boot-parent:1.2.4`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘；第三方地板与兄弟仓权威表均已并入 parent 自身） |
| **Maven Central** | 1.0.0–1.0.11 已发布（repo1 实测）；**2026-11-06 修复版 `1.0.12` 重新发布**，含 Boot3 .imports 注册 |
| **已发布版本序列** | `1.0.1` `1.0.2` `1.0.4` `1.0.7` `1.0.8` `1.0.9` `1.0.12`（`maven-metadata.xml` 实测 `latest=release=1.0.12`）；**`1.0.0` 不存在（404）** |
| **不进 Central** | `z-config-admin`（实测 `1.0.12` 路径 404，且已移出 reactor）、`_frontend`、`bootstrap-generate` |
| **默认端口** | HTTP `8080`（全仓无 `server.port` 配置，取 Spring Boot 默认）；Netty 长连接 `12888`（`Constance.serveBindPort`，`NettyServerComponent` 实际 bind 值） |
| **运行口径** | Java 8（`compile.version=8` / parent `java.version=1.8`）· Spring Boot 2.7.18（由地板下发） |
| **最近更新** | 2026-10-20 |

> 版本口径全部由 `z-boot-parent` 下发：第三方地板与兄弟仓权威表**都已并进 parent 自身**
> （`z-boot-dependencies` / `z-boot-fleet` 两个文件夹随 2026-10-08 火车制拓扑退役；其 `z-config.version` 已同步到 `1.0.12`）。
> 模块 POM 里不再写字面版本钉。

---

## 🎯 能力清单

每条都能落到具体类或端点，写不出实现的能力不列：

| 能力 | 实现位置 | 说明 |
|------|----------|------|
| 配置读写 | `ConfigServiceImpl`（core）· `ZConfigController` 的 `/api/config/getConfig`、`/saveConfig`、`/pageConfig`、`/listConfig`、`/delete` | 按 `namespace + group + dataId` 三元组，`namespace` 空时服务端回退 `DEFAULT_NAMESPACE` |
| 历史版本 + 回滚 | `ZConfigInfoHistory` / `/api/config/history/page`、`/rollback`、`/rollback/preview`、`/version/timeline`、`/version/compare`、`/history/diff` | 每次写都落一条 history；版本对比与回滚有 REST 入口 |
| 配置加密 | `core/crypto/ConfigEncryptor` + `AESEncryptor` | `dataId` 以 `cipher-` 前缀开头即自动加密存储、读时解密；内容层 AES/GCM 已委托 `z-util AesUtil`（2026-09-28），DataKey 封装留在本仓 |
| 灰度（Beta）读路径 | `ConfigServiceImpl#getConfig` → `IZConfigInfoBetaService#queryBetaConfig` + `matchBetaIps` | 读时先查 `z_config_info_beta`，命中客户端 IP 即返回灰度内容 |
| 命名空间隔离与授权 | `ZNamespaceController`（`/api/namespace`）、`ZNamespacePermissionController`（`/api/namespace/permission`） | 命名空间增删改查 + 按用户的命名空间权限 |
| 服务注册 / 注销 | `ZNamingServiceImpl` · `/api/naming/registerInstance{,/simple,/withCluster}`、`/deregisterInstance{,/simple,/withCluster}` | 实例 = service + ip + port + cluster + namespace |
| 服务发现 | `/api/naming/getAllInstances`、`/selectInstances/healthy`、`/selectOneHealthyInstance`、`/listServices` | 按健康标记过滤 |
| 订阅与变更推送 | `ZSubscription` + `/api/naming/subscribe`、`/unsubscribe`；客户端 `ZNamingService#addListener` | 服务端记录订阅关系，客户端长连接回调 `ZNamingListener` |
| 长连接配置监听 | `ZConfigServiceListenerManager`（client）↔ `NettyServerComponent` + `ServerBusinessHandler`（core） | 自定义协议编解码 + `IdleStateHandler` 心跳 + 指数退避重连（上限 10 次后切换下一个 server 地址） |
| 多级缓存与故障转移 | `ZConfigServiceImpl#getConfig` | 内存 → 本地快照文件 → 服务端拉取；`serverAddr` 支持逗号分隔多地址轮询切换 |
| 控制台鉴权 | `ConfigAuthController`（`/api/config-auth/login`、`/logout`、`/current`）+ `TokenInterceptor` | token 走名为 `token` 的 cookie；开关 `z.config.auth.enabled`（`@Value` 默认 `false`） |
| 运维视图 | `DashboardController`、`ConfigMetricsController`（`/api/metrics/core`、`/api/metrics/prometheus`）、`ClusterController`、`ZConfigAuditController`、`ZConfigApprovalController`、`ZConfigPushHistoryController`、`ZConfigTagController`、`ZE18nController` | 面板统计、集群节点、审计、发布审批、推送历史、标签、国际化文案 |

控制面前端（`_frontend/`）实测在用的接口：`/api/dashboard/node`、`/api/dashboard/stats`、`/api/cluster/list`、
`/api/config/{saveConfig,pageConfig,groupList,namespaceList,history/page,delete,import}`、`/api/namespace`、
`/api/naming/listServices`。

---

## 🏗️ 项目结构

```
z-config/
├── pom.xml                          # 聚合根：parent z-boot-parent:1.2.4，<revision>1.0.12
├── z-config-common/                 # 33 个类：协议常量/编解码器 + DTO + enum（Constance、ProtocolConstant、Custom*Codec、ZConfig*Request…）
├── z-config-client/                 # 12 个类：Java SDK（ZConfigFactory、ZConfigService(Impl)、ZNamingService(Impl)、两级 CallClient、长连接监听管理器）
├── z-config-spring-boot-starter/    # 3 个类：ZConfigAutoConfiguration + ZConfigProperties（前缀 z.config）
├── z-config-core/                   # 69 个类：Netty 服务端 + MyBatis-Plus 实体/Mapper/Service + 加密器
├── z-config-web/                    # 23 个类：REST Controller（15 个）+ 自动装配 + 拦截器 + 打包进 static/ 的控制台产物
├── z-config-sdk/                    # ⚠ 只有 1 个源文件，且是 IDE 模板 Main（打印 "Hello and welcome!"），未依赖 z-config-client
│
├── z-config-admin/                  # 【已废弃·不在 reactor】根 POM `<modules>` 已注释掉；唯一 @SpringBootApplication 在这里
├── _frontend/                       # React 19 + antd 6 + Vite 6 控制台（独立 npm 工程，不进 Maven）
├── bootstrap-generate/              # MyBatis-Plus + Freemarker 代码生成器（不进 reactor）
├── Dockerfile                       # Temurin 21 镜像，COPY z-config-admin/target/*.jar
├── pnpm-workspace.yaml              # 只声明 nodeLinker: hoisted（根目录没有 package.json）
├── LICENSE                          # MIT
└── _doc/                            # 见文末「文档目录」
```

参与 reactor 的模块 = `z-config-common`、`z-config-client`、`z-config-spring-boot-starter`、`z-config-sdk`、
`z-config-core`、`z-config-web`（6 个，根 POM `<modules>` 实测；加聚合根共 7 个坐标，全部已在 Central）。
`z-config-admin`、`_frontend`、`bootstrap-generate` 三个目录**不在** `<modules>` 里：
根 POM 对 admin 的注释写着「已废弃」，`_frontend` 与 `bootstrap-generate` 的 POM `<parent>` 还钉着
`z-config:1.0.0-SNAPSHOT`（早已不存在的版本），从根构建碰不到它们。

没有任何模块设置 `maven.deploy.skip`；对外发布/不发布完全由 reactor 成员关系决定，
Central 上传由根 POM 的 `central` profile（source + javadoc + gpg + central-publishing 0.8.0，`autoPublish`）承担。

---

## 🔧 技术栈

| 层级 | 技术（实测口径） |
|------|------------------|
| 语言 / 运行时 | Java 8（本仓 `compile.version=8`，parent `java.version=1.8`；Docker 镜像跑在 Temurin 21 JDK 上） |
| 框架 | Spring Boot 2.7.18（由 `z-boot-parent` 自带地板下发，本仓不再自钉） |
| 长连接 | Netty `netty-all` 4.1.138.Final（`CustomProtocolEncoder/Decoder` 自定义协议 + `IdleStateHandler`） |
| 持久层 | MyBatis 3.5.16 + MyBatis-Plus 3.5.7（`BaseMapper` / `ServiceImpl`，由 `z-boot-datasource-starter` 供给） |
| 连接池 | Druid 1.2.23（`spring.datasource.type` 指向 Druid） |
| 数据库 | MySQL 8（库名 `z_config`） |
| 序列化 | Jackson 2.18.6 + `z-util-parser-json`；SDK 侧统一 `Result` / `Pageable` 来自 `z-util-core` |
| 加密 | AES/GCM/NoPadding（内容层委托 `z-util AesUtil`），DataKey 封装见 `core/crypto/AESEncryptor` |
| 日志 | Log4j2（各模块显式引 `log4j-api`，并从 `z-boot-web-starter` 排除 `spring-boot-starter-logging`） |
| 接口文档 | `Knife4jConfig` 只声明 `OpenAPI` bean（swagger-models），依赖面由 `z-boot-web-starter` 侧提供 |
| 前端 | React 19.2 + Ant Design 6.3 + Vite 6.2 + axios + react-router 7（`_frontend/`，pnpm 工程） |
| 构建 | Maven（reactor + flatten `oss`）· pnpm（前端）· Docker（单镜像） |

---

## 🚀 快速开始

### 编译

```bash
mvn clean install -DskipTests
```

构建需要能解析到 `io.github.yuku123:z-boot-parent:1.2.4`（在 repo1，不在磁盘；`<relativePath/>` 已留空）。
第三方与兄弟仓版本一律由 parent 链下发，模块 POM 不应出现字面版本钉。

### 起服务端

实测只有 `z-config-admin` 里有 `@SpringBootApplication`（`ZConfigApplication`，`scanBasePackages = "com.zifang.z.config"`），
但它已不在 reactor 里 —— **根构建不会产出 `z-config-admin/target/*.jar`**。要跑服务端得单独把它造出来：

```bash
mvn clean install -DskipTests                              # 先造 reactor 里的 6 个模块
mvn -f z-config-admin/pom.xml clean package -DskipTests    # 再单造可执行 jar（parent 从 repo1 解析）
java -jar z-config-admin/target/z-config-admin-*.jar
```

起来后：HTTP 在 `8080`（无 `server.port` 配置），Netty 长连接在 `12888`（`Constance.serveBindPort`，
启动日志会打印「Spring Boot集成的Netty服务已启动，监听 12888」）。控制台静态产物已提交在
`z-config-web/src/main/resources/static/` 与 `z-config-admin/src/main/resources/static/`，
`admin` 的 `application.properties` 也提示可临时用 `--server.port=18086` 覆盖端口做 standalone 联调。

### 数据源与凭据

`z-config-admin/src/main/resources/application.properties` 目前把 `spring.datasource.url` /
`spring.datasource.username` / `spring.datasource.password` 全部写成**字面值**（指向一台公网 MySQL），
仓库里没有为运行期定义任何环境变量名。这是已知问题：真实部署必须把这三项改为经环境变量注入，
不要把口令落在 yml/properties/jar/镜像层里（本 README 不复述其值）。根目录 `.env` 只承载**发布**用途的
`CENTRAL_USERNAME`、`CENTRAL_TOKEN`、`CENTRAL_GPG_PASSPHRASE`、`GPG_USER_NAME`、`GPG_KEY_ID`。

建库脚本见 [`_doc/002_deploy/init.sql`](_doc/002_deploy/init.sql)（建库 `z_config` + 16 张表）
与 [`_doc/002_deploy/z-config-upgrade.sql`](_doc/002_deploy/z-config-upgrade.sql)
（补 `z_config_approval`、`z_i18n_message` 两张表，并给 `z_config_info` 加 `encrypted_data_key` 列）。

### 起前端控制台

```bash
cd _frontend
pnpm install
pnpm dev        # Vite 监听 3000，/api 代理到 http://localhost:8080（见 vite.config.js）
pnpm build      # 产物 dist/，需手工拷进 web/admin 的 resources/static
```

---

## 🔌 API 一览

服务端 REST 前缀由 `@RequestMapping` 实测得出（`z-config-web/src/main/java/com/zifang/z/config/web/api/`）：

| 路径 | Controller |
|------|------------|
| `/api/config` | 配置 CRUD、分组/命名空间列表、分页与搜索、导入导出、克隆、监听者与订阅者统计 |
| `/api/naming` | 服务注册/注销（含 simple/withCluster 变体）、实例查询、订阅/取消订阅、服务列表 |
| `/api/namespace` · `/api/namespace/permission` | 命名空间与其权限 |
| `/api/config-auth` | 控制台登录 / 登出 / 当前用户 |
| `/api/config/audit` · `/api/config/approval` · `/api/config/push` | 审计、发布审批、推送历史 |
| `/api/config/tag` · `/api/config/tag/relation` | 标签与标签关联 |
| `/api/i18n` | 国际化文案 |
| `/api/cluster` · `/api/dashboard` · `/api/metrics` | 集群节点、面板统计、指标 |
| `/actuator` | 健康端点 |

鉴权由 `TokenInterceptor` 拦 `/**`，放行清单在 `WebConfig`（`/api/config-auth/login`、`/api/config-auth/logout`、
`/auth/**`、`/doc.html`、`/swagger-ui/**`、`/v3/api-docs/**` 及静态资源）。服务端鉴权总开关是
`z.config.auth.enabled`（默认 `false`，向后兼容），token 从名为 `token` 的 cookie 读取。

### ⛔ 已下线：Nacos 兼容面（`/nacos/v1/**`）

**2026-10-22 起下线**（用户拍板 T1 = ②「直接下线」，不是"保留 + 强制 accessToken"，也不是"收进内网"）。

- 兼容面**从未**在本 README 的 API 一览里对外承诺过（下面那张表里没有它），但代码里曾经**真的**挂着
  `GET/POST/DELETE /nacos/v1/cs/configs`、`GET /nacos/v1/console/namespaces`、`GET /nacos/v1/ns/operator/servers`
  五个端点，且一度**对公网完全免鉴权**（匿名可读 + 可发布 + 可删配置）。
- 下线口径 = **端点在应用层不存在** ⇒ `NacosCompatibleController` 与选项①的整套预备件
  （`NacosCompatProperties` / `NacosAccessTokenVerifier` / `NacosAccessTokenAuthFilter` / `NacosCompatAuthConfig`）
  **已整个删除**，`application.properties` 里的 `nacos.compat.*` 四个开关**也已删除** ⇒ **没有任何开关可开**。
- 现在的对外读数：兼容面路径一律 **404**（不是 401/403 —— 401/403 意味着"端点存在只是没权限"，
  那是**假装下线**；404 才是"没有这个端点"）。落地由两层保证：
  ① MVC 层没有 handler（`WebConfig` 的 SPA 回落也把 `nacos/` 排除，不会回落成 200 + 首页）；
  ② `NacosCompatRetiredPathFilter` 把该前缀的所有方法统一挡成 404。
- 回归钉：`z-config-web/src/test/java/com/zifang/z/config/web/offline/NacosCompatOfflinePinTest.java`
  （钉 a 正向 404 / 钉 b 尺子牙口自证 / 钉 c 自有 API 未误伤）。
- 下线**不影响**任何现存调用方：集群 20 个 Deployment 的全部 env 对 Nacos 零引用、集群内无真 Nacos 服务、
  z-config-web 日志里 `POST /nacos/v1/cs/configs` 出现 0 次。自有 API 仍是 `/api/config/**`（POST + JSON body + `Result` 包装）。

---

## ⚙️ 客户端配置（`z.config.*`）

`ZConfigProperties` 绑定前缀 `z.config`（键名实测于该类）：

```yaml
z:
  config:
    enabled: true                 # 总开关；ZConfigAutoConfiguration 是 @ConditionalOnProperty(havingValue="true")，默认关
    server-addr: 127.0.0.1:8080   # host:port，支持逗号分隔多地址做故障转移
    namespace: dev                # 命名空间（Factory 侧 requireNonNull，缺省即抛）
    auto-refresh: true
```

⚠ 实测 `ZConfigAutoConfiguration#toProperties` 只把 `serverAddr` / `namespace` / `username` / `password`
四个键转成 SDK 的 `Properties`；`connect-timeout-ms`、`long-poll-timeout-ms`、`auto-refresh`
虽在 `ZConfigProperties` 里有字段和 getter，但**没有任何消费点**，配了也不生效。
`username` / `password` 是可选鉴权位，值一律经环境变量注入、不要写进 yml。

纯 Java 应用（不经 starter）走 `ZConfigFactory`：

```java
Properties props = new Properties();
props.setProperty("serverAddr", "127.0.0.1:8080");   // 必填
props.setProperty("namespace", "dev");               // 必填，Validator 会 requireNonNull
ZConfigService config = ZConfigFactory.createConfigService(props);
Result<String> r = config.getConfig("DEFAULT_GROUP", "app.properties", 5000);
config.addListener("DEFAULT_GROUP", "app.properties", newConfig -> System.out.println(newConfig));
```

---

## ⚠️ 实测坑（接入前先看）

1. **SDK 的 HTTP 路径与服务端对不上。** `ConfigCallClient` 打 `http://host:port/config/...`、
   `NamingCallClient` 打 `.../naming/...`，而服务端 Controller 挂在 `/api/config`、`/api/naming`；
   全仓没有任何 `context-path` 或 rewrite 把两者接起来。控制台前端走的是 `/api/**`（能被服务），
   SDK 的 HTTP 读写分支在默认部署下会拿不到数据（客户端会退到本地快照）。
2. **没有 `shutDown()`。** `ZConfigService` / `ZNamingService` 接口与两个 Impl 都不含 `shutDown` 或 `shutdown`
   方法（关闭逻辑只在内部 `ZConfigServiceListenerManager#shutdown`），而 starter 的
   `@Bean(destroyMethod = "shutDown")` 指的是这个方法名 —— 老 README 里 `configService.shutDown()` 的示例编译不过。
3. **端口口径三处不一致。** 真正 bind 的是 `12888`（`Constance.serveBindPort`）；
   `ProtocolConstant.SERVER_PORT = 8888` 只被两个 test 作用域的手工 runner 引用；
   而 [`Dockerfile`](Dockerfile) `EXPOSE 8080 8888`、`start.sh` 映射 `8888`（旧口径），
   只有 `build.sh` 映射了 `12888:12888`。
4. **`z-config-sdk` 不是 demo。** 它唯一的源文件是 IDE 模板 `Main`（打印 "Hello and welcome!"），
   没有依赖 `z-config-client`，但已作为 `1.0.12` 发到 Central（空壳产物）。
5. **`z-boot-config-starter` 落后一格。** 本地实测其 `1.0.19` POM 直接钉 `z-config-spring-boot-starter:1.0.8`，
   而 `z-boot-parent` 的 `z-config.version` 已是 `1.0.12`；用它请自行覆盖版本。
6. **发布/打包脚本按现状跑不通。** `_doc/003_script/package.sh` 里引用的 `z-config-admin-frontend/` 目录不存在
   （前端实际在 `_frontend/`）；`_doc/003_script/deploy_maven_center.sh` 仍是从 `z-util` 拷来的版本
   （`cd "$(dirname "$0")"` 后要求同目录有 `pom.xml`，在 [`_doc/003_script/`](_doc/003_script/) 下必然 die 并提示"请在 z-util 仓库根目录运行"）。

---

## 🧪 测试

```bash
mvn test          # 实测在本地/sandbox 环境跑不过，见下
mvn -DskipTests install
```

如实口径（按源码实测，不是"应当通过"）：

- 全仓 JUnit 用例 **29 个**，`@Test` 全部集中在 `z-config-client/src/test`（JUnit 4）：
  `NamingCallClientTest` 13 + `ConfigCallClientTest` 8 + `ZNamingServiceTest` 5 + `ZConfigTest` 2 + `ZNamingTest` 1。
- 其中 **21 个是自洽的**：`ConfigCallClientTest` / `NamingCallClientTest` 在 `@Before` 里用
  `com.sun.net.httpserver.HttpServer` 起本机随机端口假服务端，断言打到 `/config/*`、`/naming/*`。
- 另外 **8 个是活体联调脚本**：`ZConfigTest` / `ZNamingServiceTest` / `ZNamingTest` 把 `serverAddr`
  写成常量（分别指向一台外部公网机器与 `127.0.0.1:8084`），`@Before` 直接构造服务实例打真连接；
  `NamingServiceTestSuite` 又是 `@RunWith(Suite.class)` 把其中两个聚合起来。没有可用服务端时它们必然红。
- `z-config-core/src/test` 里**没有** `@Test`：只有手工 `NettyServer` main runner 和
  `AESEncryptorCompatCrossCheck`（与 `z-util AesUtil` 的密文布局对账，需手工触发）。
- `z-config-admin/src/test` 只有一个占位类 `A.java`。

结论：`mvn test` 在当前环境**不通**是这套测试写法决定的，不是构建坏了；要绿灯得先把
依赖真服务端的那 8 个用例挪出默认 surefire 范围，或起一台 z-config server（HTTP + `12888`）再跑。

---

## 🐳 部署

镜像与脚本都在本仓，`docker-compose` / `k8s` / `Makefile` **不存在**（实测根目录无这些资产）。

```bash
bash _doc/003_script/build.sh     # docker build + 滚动起停 + 健康检查（宿主机 8084→容器 8080，另映射 12888）
bash _doc/003_script/start.sh     # 单机 docker run（映射 8080 与 8888 —— 8888 是旧口径，Netty 实听 12888）
```

[`Dockerfile`](Dockerfile) 实测内容：`FROM eclipse-temurin:21-jdk`、
`COPY z-config-admin/target/z-config-admin-*.jar app.jar`、`EXPOSE 8080 8888`、
`HEALTHCHECK` 打 `http://127.0.0.1:8080/doc.html`。
注意它 COPY 的是已废弃、且不在 reactor 里的 `z-config-admin` 产物 —— 必须先按上面「起服务端」那一节
单独 `-f z-config-admin/pom.xml` 造出 jar，`docker build` 才有东西可 COPY。
`start.sh` 支持在存在 `./application-prod.yml` 时把它挂进容器 `/app/application-prod.yml`。

发布到 Maven Central 走根 POM 的 `central` profile（`-Pcentral`，凭证见上文 `.env` 变量名 +
`_doc/003_script/install-settings.sh` 把 `settings.xml` 的 `<server id="central">` 写成 `${env.*}` 占位）。

---

## 📄 License

[MIT](LICENSE)（根 `LICENSE` 为 MIT 原文，版权行写 `Copyright (c) 2026 z-opc-foundation`；根 POM `<licenses>` 同样声明 MIT License）。

---

## 🔗 相关项目

| 项目 | 实测关系 |
|---|---|
| [z-boot](https://github.com/z-opc-foundation/z-boot) | `z-boot-parent` 提供本仓 Java 8 + Spring Boot 口径；`z-boot-config-starter` 是对 `z-config-spring-boot-starter` 的一行聚合 |
| [z-rpc](https://github.com/z-opc-foundation/z-rpc) | 示例配置里出现 `z.rpc.registry.type: z-config`，但 `z-rpc` 的 `ZConfigRegistry` import 与其 `z-config-*` 依赖目前仍是注释状态（注释理由是"z-config 未上 Central"，与本仓实测已不符）—— 集成尚未打通 |
| [z-util](https://github.com/z-opc-foundation/z-util) | `Result` / `Pageable` / `HttpExecutor` / `JsonUtil` / `AesUtil` 由它提供（版本走 `z-boot-parent` 的兄弟仓权威表） |

_Maintained by the z-opc-foundation organization._

---

## 文档目录

- [`_doc/008_troubleshooting/`](_doc/008_troubleshooting/) — 踩坑与失败要点（控制台点击普查实录）
本项目文档统一收口在 `_doc/` 下：

- [`_doc/001_arch/`](_doc/001_arch/) — 架构文档：
  - [`00-overview.md`](_doc/001_arch/00-overview.md) — 项目总览与实体/服务清单
  - [`01-module-structure.md`](_doc/001_arch/01-module-structure.md) — 模块结构与分包规范
  - [`02-api.md`](_doc/001_arch/02-api.md) — 接口清单（注意其中 `/api/auth/*` 一节与当前代码的 `/api/config-auth/*` 不同名，以代码为准）

- [`_doc/002_deploy/`](_doc/002_deploy/) — 部署 SQL：
  - [`init.sql`](_doc/002_deploy/init.sql) — 建库 `z_config` + 16 张表
  - [`z-config-upgrade.sql`](_doc/002_deploy/z-config-upgrade.sql) — 增量：`z_config_approval`、`z_i18n_message`、`encrypted_data_key` 列

- [`_doc/003_script/`](_doc/003_script/) — 运维脚本：
  - [`build.sh`](_doc/003_script/build.sh) — 打包 + 镜像构建 + 平滑发布
  - [`package.sh`](_doc/003_script/package.sh) — 前端构建 + `mvn install`（其前端目录名已过期，见「实测坑」）
  - [`start.sh`](_doc/003_script/start.sh) — 单机 `docker run`
  - [`install-settings.sh`](_doc/003_script/install-settings.sh) — 写入 `~/.m2/settings.xml` 的 central server（占位引用环境变量）
  - [`deploy_maven_center.sh`](_doc/003_script/deploy_maven_center.sh) — Central 发布（当前为 z-util 拷贝版，目录假设不成立，见「实测坑」）

- `_doc/004_skill/` — AI skill 定义（目前为空目录，暂无 skill）

各文档详细说明见各子目录。

---

<!-- z-opc-foundation-sibling-registry:v1 -->
## 🔗 兄弟仓全景（z-opc-foundation 组织）

本仓隶属于 [z-opc-foundation](https://github.com/orgs/z-opc-foundation/repositories) 组织——一人公司基座的全部自研件（现存 61 仓，含私有仓；口径 2026-11-24 GitHub org API 实读）。

| 仓 | 一句话说明 |
|---|---|
| [z-agent](https://github.com/z-opc-foundation/z-agent) | 云端 Agent 平台的库与 starter——ReAct/Plan 实现 + 注册中心 + REST 网关 + 只读控制面 |
| [z-agent-kernel](https://github.com/z-opc-foundation/z-agent-kernel) | Agent 框架基座——消息/工具/记忆/provider/会话 SPI + 轻量默认实现（纯 POJO 无 Spring） |
| [z-asset](https://github.com/z-opc-foundation/z-asset) | 企业资产台账——实物设备全生命周期 + 域名/证书/许可到期提醒 |
| [z-audit](https://github.com/z-opc-foundation/z-audit) | 审计与合规留痕——事件模型 + 哈希链 + 事件出口 SPI + 留存期策略 |
| [z-biz](https://github.com/z-opc-foundation/z-biz) | 商务外壳域——客户/合同/票据/对账四聚合 |
| [z-biz-erp](https://github.com/z-opc-foundation/z-biz-erp) | 进销存 ERP——商品/客商/采购/销售/库存流水/应收应付核销（z-biz 业务线） |
| [z-biz-security](https://github.com/z-opc-foundation/z-biz-security) | 业务数据安全防护入口——场景化策略注册 + 安全线四段流水线对外编排 |
| [z-boot](https://github.com/z-opc-foundation/z-boot) | Spring Boot Starter 聚合仓 + 版本权威（火车制）——唯一消费入口 z-boot-parent |
| [z-bot](https://github.com/z-opc-foundation/z-bot) | 本地 agent 应用——终端 CLI + HTTP/SSE web 控制台 + 多通道 gateway（飞书/钉钉/webhook） |
| [z-cache](https://github.com/z-opc-foundation/z-cache) | RESP2 协议兼容的分布式内存缓存——Java 8 + Netty，Redis 客户端零修改直连 |
| [z-camuda](https://github.com/z-opc-foundation/z-camuda) | 【已退役·归档】基于 Camunda 的工作流封装层（原 z-wf 改名，与现行 z-wf 无共同历史） |
| [z-cognition](https://github.com/z-opc-foundation/z-cognition) | 业务认知域/认知舱——术语/概念/证据链 + memory/kb/ontology 渐进式模块 |
| [z-config](https://github.com/z-opc-foundation/z-config) | 分布式配置中心 + 服务注册中心（类 Nacos）——Netty 长连接推送 + React 控制台 **（本仓）** |
| [z-ctc](https://github.com/z-opc-foundation/z-ctc) | 4A 中心——认证/授权/账户/审计，多租户 JWT + SSO 跨子域 |
| [z-dms](https://github.com/z-opc-foundation/z-dms) | 数据源管控底座——统一登记、统一建池、统一探活、变更统一推送 |
| [z-env](https://github.com/z-opc-foundation/z-env) | 本地中间件与基础设施环境——31 个 docker-compose 场景 + make 一键运维 |
| [z-etl](https://github.com/z-opc-foundation/z-etl) | ETL 引擎——DataX 式线性作业 + Flink 式 DAG 流管道（键控状态/水位/窗口） |
| [z-ext](https://github.com/z-opc-foundation/z-ext) | Java 扩展点框架——@ExtPoint/@ExtImpl 运行期选实现，五档侵入方式 |
| [z-frontend-common](https://github.com/z-opc-foundation/z-frontend-common) | 全仓共享前端基座——AppLayout/LoginPage 等前端规范的唯一承载 |
| [z-frontend-markdown](https://github.com/z-opc-foundation/z-frontend-markdown) | Markdown 通用共享组件包（npm）——MarkdownLiveEditor/NoteMarkdown/MermaidBlock 单一真源 |
| [z-frontend-render](https://github.com/z-opc-foundation/z-frontend-render) | 平台共享前端渲染引擎——后端 schema 是唯一事实源，报表与低代码共用 |
| [z-graph](https://github.com/z-opc-foundation/z-graph) | 独立图数据库——MVCC 版本链存储 + OpenCypher 子集 + Bolt 风格协议 + React 控制台 |
| [z-gw](https://github.com/z-opc-foundation/z-gw) | 自研高性能 API 网关——Netty 手写运行时，Route/Predicate/Filter 模型可嵌 Spring Boot |
| [z-indexer](https://github.com/z-opc-foundation/z-indexer) | 代码索引服务——仓库注册表 + 定时重建索引 + 符号/调用链查询 |
| [z-introduce](https://github.com/z-opc-foundation/z-introduce) | 组织仓库展示站——全仓实测元数据汇总成可搜索静态站点 |
| [z-lc](https://github.com/z-opc-foundation/z-lc) | 低代码平台——DB 驱动通用 CRUD 引擎 + 报表/模型设计框架 |
| [z-llm-agent](https://github.com/z-opc-foundation/z-llm-agent) | CLI Agent 服务化——把本机 CLI agent（CC/Hermes/Qoder 等）暴露成 OpenAI 兼容对话 API |
| [z-llm-gateway](https://github.com/z-opc-foundation/z-llm-gateway) | LLM 网关——OpenAI/Anthropic 双协议面 + 多 vendor 凭据池、限流与 token 记账 |
| [z-llm-msghub](https://github.com/z-opc-foundation/z-llm-msghub) | msgHub 中心消息路由——WebSocket 中心枢纽 + Agent 侧客户端 SDK |
| [z-mask](https://github.com/z-opc-foundation/z-mask) | 脱敏与遮蔽引擎——按敏感命中位置遮蔽，策略即数据 |
| [z-mcp](https://github.com/z-opc-foundation/z-mcp) | MCP 服务端 + 上游聚合网关——Java 8 手写协议层，Spring Boot 自动装配 |
| [z-meta](https://github.com/z-opc-foundation/z-meta) | 元数据管理平台——租户/应用/接口清单登记 + @BizModel 业务模型采集 |
| [z-mill](https://github.com/z-opc-foundation/z-mill) | 资料磨坊——全格式文档转 Markdown/JSON/纯文本/HTML（对标 MinerU） |
| [z-mist](https://github.com/z-opc-foundation/z-mist) | KMS 分布式密钥/秘密管理平台——集中加密存储 + Netty 协议 + starter 嵌入 |
| [z-mq](https://github.com/z-opc-foundation/z-mq) | 自研分布式消息队列——NameServer+Broker+Store，形状对标 RocketMQ、自有 Netty 协议 |
| [z-msg](https://github.com/z-opc-foundation/z-msg) | 多通道消息 + WebSocket 实时下发基础设施——短信/邮件/站内信/Webhook/IM 平台 |
| [z-observer](https://github.com/z-opc-foundation/z-observer) | 可观测性域——指标/日志/链路 + 告警引擎 |
| [z-opc](https://github.com/z-opc-foundation/z-opc) | 一人公司统一后端+统一前端——单 JVM all-in-one 承载协作/Agent/RPA/交易/资产等业务域与自研中间件 |
| [z-opc-foundation-lead](https://github.com/z-opc-foundation/z-opc-foundation-lead) | 组织治理大脑——事实表、强制规范、跨仓工具集、新机器恢复钥匙（本清单权威持有处） |
| [z-opcs](https://github.com/z-opc-foundation/z-opcs) | 任务撮合/服务市场——用户付费发布诉求，持有者接单完成，金额自动瓜分 |
| [z-ops](https://github.com/z-opc-foundation/z-ops) | 个人公司运维平台——部署单元/镜像构建/WebShell/K8s 巡检 + Ops Agent 执行器 |
| [z-oss](https://github.com/z-opc-foundation/z-oss) | 对象存储服务——自持 local 后端 + aliyun/s3 对接层，starter 注入 OssTemplate |
| [z-product](https://github.com/z-opc-foundation/z-product) | 产品设计信息汇总仓（开发域 PM 工件） |
| [z-qa](https://github.com/z-opc-foundation/z-qa) | 步骤式 API 链路测试平台——用例/计划/执行/报告/质量看板（对标 TestRail·MeterSphere） |
| [z-rpa](https://github.com/z-opc-foundation/z-rpa) | RPA 产品皮——应用市场/需求中心（执行归 z-script，触发归 z-schedule） |
| [z-rpc](https://github.com/z-opc-foundation/z-rpc) | Java 8 + Netty RPC 框架——微内核 + SPI 插件化，形状参照 Dubbo/gRPC |
| [z-schedule](https://github.com/z-opc-foundation/z-schedule) | 分布式任务调度中枢（对标 XXL-Job）——选主 + 时间环排期，starter 嵌入宿主 |
| [z-script](https://github.com/z-opc-foundation/z-script) | 多 DSL 脚本平台 + Mock 平台——沙箱执行/版本灰度/可注册 MCP Tool，starter 内嵌 |
| [z-security](https://github.com/z-opc-foundation/z-security) | 数据安全域编排层——场景→策略→识别→脱敏→水印→审计 四段流水线编排 |
| [z-sense](https://github.com/z-opc-foundation/z-sense) | 敏感识别与分类分级引擎——数据安全线底座（规则目录 + 可插拔识别器 + 分类分级） |
| [z-skill](https://github.com/z-opc-foundation/z-skill) | Skill 平台聚合器——归一化各家 Skill 形状，再按各家 API 形状发回 |
| [z-surl](https://github.com/z-opc-foundation/z-surl) | 短网址服务——短码生成/解析/命中统计，starter 一行集成 |
| [z-task](https://github.com/z-opc-foundation/z-task) | 任务协作平台——项目空间/看板/任务卡片（看板范式，对标 Trello·Teambition） |
| [z-team](https://github.com/z-opc-foundation/z-team) | 数字员工协同域——全栈独立仓，自 z-opc 毕业 |
| [z-trade](https://github.com/z-opc-foundation/z-trade) | 交易域独立仓——自 z-opc reactor 迁出 |
| [z-util](https://github.com/z-opc-foundation/z-util) | 底层 Java 工具库——89 个模块 POM，Java 8/17 口径，按需引用按需升级 |
| [z-vector](https://github.com/z-opc-foundation/z-vector) | 自研 Java 向量数据库——嵌入式+可独立部署，ANN+Payload 过滤，兼容 Qdrant REST |
| [z-voice-demo](https://github.com/z-opc-foundation/z-voice-demo) | 语音链路 demo——流式识别→kernel LLM→流式 TTS→可打断 的契约验证实现 |
| [z-watermark](https://github.com/z-opc-foundation/z-watermark) | 水印引擎——显式文字/图片隐写/盲水印，三种技术成对「打+验」 |
| [z-webide](https://github.com/z-opc-foundation/z-webide) | 企业级 WebIDE 后端——Git 仓/共享目录秒变 code-server 实例，全生命周期管理 |
| [z-wf](https://github.com/z-opc-foundation/z-wf) | 自研流程引擎——不依赖 Camunda/Flowable，定义层与运行时全自研 |

**已退役仓**（迁至 [z-trash](https://github.com/z-trash) 组织，archived 只读）：

| 仓 | 说明 |
|---|---|
| [z-agent-proxy](https://github.com/z-trash/z-agent-proxy) | 三方 coding agent 适配代理（Aider/Cursor → kernel Agent SPI），2026-10-26 退役 |
| [z-kb](https://github.com/z-trash/z-kb) | 自研知识库系统（对标 LightRAG/RAGFlow），2026-11 并入 z-cognition 认知舱后退役 |
| [z-ontology](https://github.com/z-trash/z-ontology) | 本体层（TBox/ABox/RDFS 推理），并入 z-cognition 后退役 |

另有 [z-report](https://github.com/z-opc-foundation/z-report)（报表域，2026-10-11 撤销并入 z-lc（远端已删除））。

总清单权威持有处在 [z-opc-foundation-lead](https://github.com/z-opc-foundation/z-opc-foundation-lead) `008_组织规范/005_兄弟仓清单.md`。
