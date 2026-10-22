# z-config-component

z-config（配置中心）跨仓唯一前端面：把「配置项 CRUD / 变更历史 / 服务与实例治理 / 命名空间 / 审计 / 推送通知」
这一整套配置中心页面收敛成一个可复用的 React 组件包，由 `z-config-suit` 独立壳或平台主壳挂载。

- **npm 包名**：`@yuku123/z-config-component`
- **宿主接入**：`@yuku123/z-config-component/pages`（manifest + 请求层），主壳可只取 `ComponentDocPage`
- **后端**：`z-config-core` 的配置中心 / 服务发现接口，前缀由 `configureConfig({apiBase})` 注入
- **技术栈**：React 19 + antd 6 + react-router-dom 7 + vite（library mode，双入口 `.` / `./pages`）

---

## 一、能力概览

路由前缀 `/z-config`，菜单与路由都在 `src/pages-manifest.jsx` 里声明，宿主（suit 或主壳）读数组自己组装。

| 页面 | 路由（相对 `/z-config/*`） | 源文件 | 能力 |
|------|--------------------------|--------|------|
| 首页 | `home` | `pages/HomePage.jsx` | 读 `userInfo` 显示登录人，按 `menuItems` 渲染功能入口卡片 |
| 概览 | `dashboard` | `pages/Dashboard.jsx` | 拉 `dashboard/stats`，展示配置数 / 服务数 / 实例数 / 命名空间数 |
| 配置管理 / 列表 | `config/list` | `pages/config/ConfigList.jsx` | 按命名空间 + 分组分页查配置项，支持删除 |
| 配置管理 / 编辑 | `config/edit` | `pages/config/ConfigEdit.jsx` | 新建 / 编辑配置项，内容大小上限 100KB（对齐 Nacos） |
| 配置管理 / 变更历史 | `config/history` | `pages/config/ConfigHistory.jsx` | 分页查看历史版本并支持回滚 |
| 服务管理 / 服务列表 | `service/list` | `pages/service/ServiceList.jsx` | 服务列表 + 实例健康态拉取 |
| 命名空间 | `namespace` | `pages/namespace/NamespaceList.jsx` | 命名空间增删改查（走独立 `/api/namespace`） |
| 审计日志 | `audit` | `pages/audit/AuditLog.jsx` | 按操作类型着色展示审计事件 |
| 推送通知 | `notification` | `pages/notification/NotificationHistory.jsx` | 推送记录分页 + 结果状态（成功 / 失败 / 超时） |
| 组件文档 | `docs` | `pages/ComponentDoc.jsx` | 本文件渲染页（左侧 240px TOC + 右侧正文） |

除上表的具名路由外，`src/pages/ConfigApp.jsx` 是一组子路由的挂载点（`list` / `edit` / `history` /
`dashboard` / `cluster` / `service` / `service/detail`），由 `routes` 里的 `/z-config/:rest*` 兜底。

## 二、导出面

```js
// 主入口（. 桶）：请求层 + 域挂载点
import {configureConfig, setToken, clearToken} from '@yuku123/z-config-component'

// 宿主组装面（./pages 桶）：manifest + 页面组件
import {menuItems, routes, HomePage, LoginPage} from '@yuku123/z-config-component/pages'

// 组件文档页（主壳接线用，名字全仓统一）
import {ComponentDocPage} from '@yuku123/z-config-component'
```

`./pages` 桶是 `export * from './pages-manifest.jsx'` 的整份转发，**不要再手挑名字** ——
上一版手挑导致 manifest 新增的 `HomePage` / `LoginPage` 没被转发，rollup 直接报
`LoginPage is not exported by dist/pages.js`。

## 三、API 端点

请求实例由基座 `createRequest` 工厂创建（`src/services/request.js`），`baseURL` 默认为空串，
token 走 `Authorization: Bearer`，后端返回 `{code, data, message}` 由基座统一解包。
下表路径是 `src/services/api.js` 与页面内直接调用里出现的**实际路径**。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/config/list` | 配置项列表（query 传参） |
| POST | `/config/pageConfig` | 配置项分页（body 键名 snake_case） |
| POST | `/config/getConfig` | 取单个配置项 |
| POST | `/config/saveConfig` | 保存配置项 |
| DELETE | `/config/delete` | 删除配置项（走 body，非 query） |
| POST | `/config/history/page` | 配置变更历史分页 |
| POST | `/config/rollback` | 回滚到历史版本 |
| GET | `/config/namespaceList` | 命名空间列表 |
| GET | `/config/groupList` | 分组列表 |
| GET | `/config/page` | 兼容用分页接口 |
| GET | `/config` | 兼容用单条读取 |
| POST | `/config` | 兼容用新建 |
| PUT | `/config` | 兼容用更新 |
| DELETE | `/config` | 兼容用删除 |
| GET | `/cluster/list` | 集群列表（⚠ 与命名空间不是一回事） |
| POST | `/cluster/save` | 保存集群 |
| POST | `/cluster/delete` | 删除集群 |
| GET | `/dashboard/stats` | 首页概览统计 |
| GET | `/naming/listServices` | 服务列表 |
| GET | `/naming/getAllInstances` | 全部实例 |
| GET | `/naming/selectInstances/healthy` | 健康实例列表 |
| GET | `/naming/selectOneHealthyInstance` | 单个健康实例 |
| POST | `/naming/registerInstance` | 注册实例 |
| POST | `/naming/deregisterInstance` | 注销实例 |
| POST | `/naming/subscribe` | 订阅变更 |
| POST | `/naming/unsubscribe` | 取消订阅 |
| GET | `/api/naming/listServices` | 页面内直连的服务列表（同上，路径前缀不同） |
| GET | `/api/namespace/list` | 命名空间列表（页面内 `fetch` 直连） |
| POST | `/auth/login` | 登录 |

## 四、页面说明

### 4.1 配置管理 / 列表

按命名空间 + 分组分页查询配置项；行操作提供编辑与删除。删除走 `deleteConfig`，**必须发 body** ——
后端 handler 是 `deleteConfig(ZConfigQueryRequest)`，早期发 query 导致三个字段全 null，只回「配置不存在」。

### 4.2 配置管理 / 编辑

新建 / 编辑表单。内容类型支持 `text` / `json` / `yaml` / `properties` / `xml`，
内容大小上限 100KB（`MAX_CONTENT_SIZE`，对齐 Nacos 默认限制）。

### 4.3 配置管理 / 变更历史

分页展示历史版本，可回滚。⚠ 历史请求只有 `search` / `group` 两个可用过滤条件，
`namespace` 在后端是 `Long` 类型，传字符串会 400；分页字段继承 `PageRequest{current,size}`，
传 `pageNum` / `pageSize` 会被忽略并整表返回。

### 4.4 服务管理 / 服务列表

展示服务列表并可拉取各服务实例的健康态。

### 4.5 命名空间

命名空间的增删改查。该页**不走共享 request**，用原生 `fetch` 直连 `/api/namespace`，
因此没有统一解包与 token 注入。

### 4.6 审计日志

按操作类型（CREATE / UPDATE / DELETE / ROLLBACK / IMPORT / EXPORT / CLONE）着色展示审计事件。

### 4.7 推送通知

推送记录分页，按结果状态（成功 / 失败 / 超时）着色。

### 4.8 组件文档

本文件。内容以 `?raw` 在编译期内联，零额外请求；左侧 240px TOC 随滚动联动。

## 五、配置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `configureConfig({apiBase})` | `''` | 改写请求实例的 `baseURL`，用于宿主换挂载前缀 |
| token 存储键 | `zconfig_token` | 与历史实现兼容，未走基座默认 `token` |
| userInfo 存储键 | `zconfig_user` | 同上 |
| `isAuthenticated()` | — | 读 localStorage / sessionStorage 的 token |

## 六、已知限制

- 菜单里的 `/z-config/overview` 与 `/z-config/system` **没有对应路由**，点击会落到 404。
- `src/pages/Overview.jsx`、`src/pages/ConfigList.jsx`、`src/pages/ServiceList.jsx`、
  `src/pages/ServiceDetail.jsx`、`src/pages/cluster.jsx` 是根目录下的历史副本，
  现行实现走 `pages/config/`、`pages/service/` 子目录下的版本，根目录副本多数未被引用。
- **body 键名 snake_case、query 键名 camelCase**：后端全局 Jackson 配了 `SNAKE_CASE`，
  `@RequestBody` 的多词属性会被改名（`nameSpace` → `name_space`），发 camelCase 会**静默失效**；
  而 `@RequestParam` 的名字是字面量，query 仍要 camelCase。这两条同时成立，别混。
- `/config/pageConfig` 只用 `nameSpace` / `group` / `search` 三个字段，
  声明里的 `dataId` / `appName` 服务端不消费，发了也**不会过滤**；按 dataId 过滤要靠 `search`（LIKE）。
- `/cluster/list` 返回 200 但可能 0 行，不能拿它填命名空间下拉。
- 命名空间页用原生 `fetch`，没有统一解包 / token / 401 处理。

## 七、变更记录

| 版本 | 变更 |
|---|---|
| 0.1.2 | 新增组件文档页 `ComponentDocPage`（lead 008 §7）：`src/DOC.md` + 基座 `ComponentDoc` 薄包装 |
| 0.1.2 | 删除配置走 body、请求体键名 snake_case 口径落到注释与文档 |
| 0.1.1 | `./pages` 桶改为整份转发 `export *`，修复 `LoginPage is not exported` 构建失败 |
| 0.1.0 | 首版：配置中心页面集 + manifest + 自洽 request |
