# z-config 模块规范

## 1. 模块结构

```
z-config/
├── z-config-core/                   # 核心业务
│   └── src/main/java/com/zifang/z/config/core/
│       ├── domain/
│       │   ├── entity/             ← ZConfigInfo, ZCluster, ZNamingInstance 等
│       │   └── mapper/            ← 各实体 Mapper
│       ├── service/                ← ConfigService + ZNamingService
│       │   └── impl/
│       └── server/                 ← 配置变更推送（ServerBusinessHandler）
├── z-config-common/                # 公共模型
│   └── src/.../com/zifang/z/config/common/model/
│       └── config/                 ← ZConfigSaveRequest 等通用请求模型
└── z-config-web/                   # Web 层
    └── src/main/java/com/zifang/z/config/web/api/
        ├── ConfigController        ← 配置 CRUD
        ├── ClusterController      ← 集群/命名空间管理
        ├── ZNamingController      ← 服务注册/发现
        └── DashboardController    ← 统计面板
```

## 2. Controller 规范（已统一 GET/POST）

### 2.1 ConfigController

| 方法           | 路径                          | 说明            |
|--------------|-----------------------------|---------------|
| saveConfig   | POST /api/config/saveConfig | 新增/更新配置       |
| getConfig    | POST /api/config/getConfig  | 查询配置内容        |
| pageConfig   | POST /api/config/pageConfig | 分页查询配置列表      |
| groupList    | GET /api/config/groupList   | 获取所有 Group 列表 |
| deleteConfig | POST /api/config/delete     | 删除配置          |

### 2.2 ClusterController

| 方法     | 路径                            | 说明          |
|--------|-------------------------------|-------------|
| list   | GET /api/cluster/list         | 获取所有集群/命名空间 |
| save   | POST /api/cluster/save        | 新增/更新集群     |
| delete | POST /api/cluster/{id}/delete | 删除集群        |

### 2.3 ZNamingController（服务注册与发现）

| 方法                       | 路径                                         | 说明       |
|--------------------------|--------------------------------------------|----------|
| registerInstance         | POST /api/naming/registerInstance          | 注册服务实例   |
| deregisterInstance       | POST /api/naming/deregisterInstance        | 注销服务实例   |
| registerInstanceSimple   | POST /api/naming/registerInstance/simple   | 简化版注册    |
| deregisterInstanceSimple | POST /api/naming/deregisterInstance/simple | 简化版注销    |
| getInstances             | GET /api/naming/instances                  | 查询服务实例列表 |
| listAllServiceNames      | GET /api/naming/serviceNames               | 获取所有服务名  |
| serviceDetail            | GET /api/naming/service/{serviceName}      | 服务详情     |
| beat                     | GET /api/naming/beat                       | 心跳       |

### 2.4 DashboardController

| 方法    | 路径                       | 说明                      |
|-------|--------------------------|-------------------------|
| stats | GET /api/dashboard/stats | 统计数据（配置数/服务数/实例数/命名空间数） |

## 3. 数据库表

| 表名                  | 说明      |
|---------------------|---------|
| z_conf_info         | 配置信息    |
| z_conf_info_history | 配置变更历史  |
| z_conf_cluster      | 集群/命名空间 |
| z_service_info      | 服务信息    |
| z_instance          | 服务实例    |

## 4. 前端页面

| 页面        | 文件                             | 说明                        |
|-----------|--------------------------------|---------------------------|
| 配置列表      | pages/config/ConfigList.jsx    | 分页列表 + 搜索 + 命名空间/Group 筛选 |
| 配置编辑      | pages/config/ConfigEdit.jsx    | 新建/编辑配置                   |
| 变更历史      | pages/config/ConfigHistory.jsx | 配置变更记录                    |
| 概览        | pages/config/Overview.jsx      | 首页入口                      |
| Dashboard | pages/config/Dashboard.jsx     | 统计数据                      |

## 5. 已知问题与后续优化

| 优先级 | 问题                                 | 说明                                                 |
|-----|------------------------------------|----------------------------------------------------|
| 高   | 缺少 BizService 层                    | Controller 直调 ConfigService/DomainService，越层调用     |
| 高   | ConfigController 直调 Mapper         | `configInfoMapper` 直接注入 Controller，应走 Service      |
| 中   | 路由动词冗余                             | `saveConfig`/`getConfig` → 应统一为 `/config` POST/GET |
| 中   | 前端直接用 axios                        | Config 页面不走 request.ts（但因直连 8848 端口，可接受）           |
| 低   | ZNamingController 参数校验在 Controller | 应下沉到 Service 层                                     |
| 低   | 配置变更推送                             | ServerBusinessHandler 已有但未完整验证                     |

## 6. 与 z-ctc 对齐说明

z-config 当前架构是早期版本，分层不如 z-ctc 规范。后续改造方向：

1. 新增 `ConfigBizService` 接口层
2. Controller 参数走 `*Req` 对象
3. 路由统一为 REST 风格（去掉 action 动词）
4. 参考 `02-z-ctc模块规范.md` 的分层结构
