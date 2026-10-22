# 003_接口清单

## 3.1 AuthController - 认证管理

| 方法   | 路径                  | 说明       |
|------|---------------------|----------|
| POST | `/api/auth/login`   | 用户登录     |
| POST | `/api/auth/logout`  | 用户登出     |
| GET  | `/api/auth/current` | 获取当前用户信息 |

## 3.2 ZConfigController - 配置管理

| 方法   | 路径                          | 说明          |
|------|-----------------------------|-------------|
| POST | `/api/config/saveConfig`    | 保存配置        |
| POST | `/api/config/getConfig`     | 获取配置        |
| POST | `/api/config/pageConfig`    | 分页获取配置信息    |
| POST | `/api/config/listConfig`    | 列表获取配置信息    |
| POST | `/api/config/delete`        | 删除配置        |
| GET  | `/api/config/groupList`     | 获取所有Group列表 |
| POST | `/api/config/history/page`  | 分页查询配置变更历史  |
| GET  | `/api/config/namespaceList` | 获取所有命名空间列表  |

## 3.3 ZNamingController - 命名服务

| 方法   | 路径                                           | 说明            |
|------|----------------------------------------------|---------------|
| POST | `/api/naming/registerInstance`               | 注册服务实例（完整参数）  |
| POST | `/api/naming/registerInstance/simple`        | 注册服务实例（简化参数）  |
| POST | `/api/naming/registerInstance/withCluster`   | 注册服务实例（指定集群）  |
| GET  | `/api/naming/getAllInstances`                | 查询服务所有实例      |
| GET  | `/api/naming/selectInstances/healthy`        | 查询服务指定健康状态的实例 |
| GET  | `/api/naming/selectOneHealthyInstance`       | 查询服务单个健康实例    |
| POST | `/api/naming/deregisterInstance`             | 注销服务实例（完整参数）  |
| POST | `/api/naming/deregisterInstance/simple`      | 注销服务实例（简化参数）  |
| POST | `/api/naming/deregisterInstance/withCluster` | 注销服务实例（指定集群）  |
| POST | `/api/naming/subscribe`                      | 消费实例订阅服务      |
| POST | `/api/naming/unsubscribe`                    | 消费实例取消订阅服务    |
| GET  | `/api/naming/listServices`                   | 获取服务列表        |

## 3.4 ClusterController - 集群(命名空间)管理

| 方法   | 路径                         | 说明             |
|------|----------------------------|----------------|
| GET  | `/api/cluster/list`        | 获取所有集群(命名空间)列表 |
| POST | `/api/cluster/save`        | 新增/更新集群(命名空间)  |
| POST | `/api/cluster/{id}/delete` | 删除集群(命名空间)     |

## 3.5 DashboardController - 仪表盘

| 方法  | 路径                     | 说明                            |
|-----|------------------------|-------------------------------|
| GET | `/api/dashboard/stats` | 获取统计数据（配置总数、服务总数、实例总数、命名空间总数） |

## 3.6 BaseHealthController - 监控端点

| 方法  | 路径                 | 说明   |
|-----|--------------------|------|
| GET | `/actuator/health` | 健康检查 |

## 3.7 NacosCompatibleController - Nacos 兼容面 —— ⛔ **已下线**（2026-10-22）

用户 2026-10-22 拍板 T1 = ②「直接下线」，本节所列端点**已随控制器一起删除**，
调用方一律得到 **404**（端点不存在），不是 401/403（那会显得"存在但没权限"= 假装下线）。

| 方法     | 路径                                | 原说明                  | 现状 |
|--------|-----------------------------------|---------------------|----|
| GET     | `/nacos/v1/cs/configs`            | 获取配置内容 / 配置列表      | ⛔ 404 |
| POST    | `/nacos/v1/cs/configs`            | 发布配置                | ⛔ 404 |
| DELETE | `/nacos/v1/cs/configs`            | 删除配置                | ⛔ 404 |
| GET     | `/nacos/v1/console/namespaces`    | 命名空间列表             | ⛔ 404 |
| GET     | `/nacos/v1/ns/operator/servers`   | 集群节点元信息             | ⛔ 404 |

配套删除的还有选项①（保留兼容面 + 强制 accessToken）的整套预备件：
`NacosCompatProperties` / `NacosAccessTokenVerifier` / `NacosAccessTokenAuthFilter` / `NacosCompatAuthConfig`，
以及 `application.properties` 里的 `nacos.compat.*` 四个键 ⇒ **不存在任何可打开兼容面的开关**。

替代路径：自有 API 在 `/api/config/**`（POST + JSON body + `Result` 包装），见 3.2 节。
回归钉见 `z-config-web/src/test/java/com/zifang/z/config/web/offline/NacosCompatOfflinePinTest.java`。
