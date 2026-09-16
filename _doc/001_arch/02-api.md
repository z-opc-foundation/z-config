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
