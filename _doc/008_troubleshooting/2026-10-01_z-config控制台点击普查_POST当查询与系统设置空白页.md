# z-config 控制台点击普查：列表接口用 POST 当查询动词，"系统设置"菜单点开是纯白页

日期：2026-10-01（本轮 15:30 起跑，读数截止 16:05 CST，均实测）
方法：本机就地起 `z-config/_frontend` 的 vite（`node_modules` 经 `git check-ignore` 现测为已忽略；跑完 `git status --porcelain _frontend` 为空），
Playwright 在 DOM 层逐控件点击。**安全护栏**：`route` 层只放行 GET/HEAD，任何非 GET 一律 `abort('failed')` 并记账；
全程不在页面打字；localStorage 里只写哨兵字面量 `SENTINEL_NOT_A_CREDENTIAL`（含本模块闸认的 `zconfig_token`），不写任何真凭证。

## 一、量到的东西

| 跑次 | URL | 点击 | 拦截的非 GET | 空白渲染 |
|---|---|---|---|---|
| pass1（静态分母） | 2 | 11 | 8 | 0 |
| pass2（+应用自己导航到的 URL 爬取） | 9 | 95 | **163** | **1**（`/system`） |

163 次拦截按接口分布（逐条来自 `click_z-config_p2.jsonl` 的 `blocked[]`）：

```
49  POST /api/config/pageConfig
43  POST /api/config/history/page
37  POST /api/config/audit/page
33  POST /api/config/push/page
 1  POST /api/config/export
```

## 二、缺陷 A：四个列表页把"查询"做成 POST，任何 GET-only 护栏都会把它们读成空

前端出处（本仓现读）：

- `_frontend/src/pages/config/ConfigList.jsx:47` — `axios.post('/api/config/pageConfig', {...})`
- `_frontend/src/pages/config/ConfigHistory.jsx:44` — `post('/api/config/history/page', ...)`
- `_frontend/src/pages/audit/AuditLog.jsx:31` — `fetch('/api/config/audit/page?...')` with `{method: 'POST'}`
- `_frontend/src/pages/notification/NotificationHistory.jsx:24` — 同上，`/api/config/push/page`
- `_frontend/src/pages/config/ConfigList.jsx:131` — `fetch('/api/config/export?<query>', {method: 'POST'})`

后端确认这是**契约而非前端笔误**（`z-config-web/src/main/java/com/zifang/z/config/web/api/ZConfigController.java`）：

- 类注释 `:36-45` 逐条广告 `POST /getConfig — 按键获取配置内容`、`POST /pageConfig — 分页查询配置列表`、
  `POST /listConfig — 列表查询配置`、`POST /history/page — 分页查询配置变更历史`
- 实现 `:96 @PostMapping("/pageConfig")`、`:154 @PostMapping("/history/page")`

后果（按严重度）：

1. **只读语义丢失**。这些接口不改状态，浏览器/CDN/网关无法缓存，无法用 URL 分享某一页筛选结果，
   前进/后退与重放的语义全错；审计与排查时"谁查了什么"在 access log 里只剩一个 POST 计数。
2. **与安全护栏天然冲突**。任何"写操作要鉴权/只放行读方法"的门（本次普查用的正是这种门，也是共享库
   `oc` 前该有的门）会把 z-config 的列表直接读成空表，看起来像"后端挂了"，实际是动词被判成写。
   以后拿真后端复验这一格时，必须把这 4 条路径显式加进读型白名单，否则会得出"控制台没有数据"的错误结论。
3. **风格自相矛盾**：`/export` 是 POST 却把参数放在 query string（`ConfigList.jsx:131`），
   而 `/pageConfig` 走 body（`:47`）——同一个页面里两种传参，`@RequestBody` 与 `@RequestParam` 两套绑定都得写。

## 三、缺陷 B：菜单项"系统设置"指向一条不存在的路由，点下去是纯白页且零报错

- 菜单声明：`_frontend/src/App.jsx:39` — `{key: '/system', icon: <SettingOutlined/>, label: '系统设置'}`
- 路由声明：`_frontend/src/App.jsx:46-60` 里有 `dashboard` / `config/list` / `config/edit` / `config/history` /
  `service/list` / `namespace` / `audit` / `notification`，**没有 `system` 那一条**

实测：pass2 爬到 `http://127.0.0.1:<port>/system` 后渲染 `controls=0`、`textLen=0`，控制台 **0 条报错**
（`click_z-config_p2.jsonl` 的 RENDER 行）。即用户点侧边栏"系统设置"→ 白屏，且开发者工具里看不出任何原因。
`App.jsx:46` 起的 `<Routes>` 没有兜底 `path="*"`，所以也不会有 404 提示。

## 四、本跑没有验证到的部分（诚实声明，别当成已覆盖）

- **删除/回滚这一臂没被点过**。`ConfigList.jsx:90` 的 `post('/api/config/delete')` 与类注释 `:43` 的
  `POST /rollback` 都是行级/操作级变更；本轮 upstream 未启动导致列表 `rows=0`，行内按钮在 DOM 里结构性不存在。
  所以上面 163 次拦截里**不含** delete/rollback —— 防火墙在这两条路径上仍未取得证据。
- **所有 500 都不是本模块的缺陷**：`_frontend/vite.config.js:14` 把 `/api` 代理到 `http://localhost:8080`，
  而 8080 上没有进程（`lsof -nP -iTCP -sTCP:LISTEN` 实测本机只有 `java *:8888` 与 `node 127.0.0.1:3000`）。
- **改指 8888 不解决**：对 `http://127.0.0.1:8888/api/**` 的 GET 实测一律 `302`（认证过滤器在路由之前拦截），
  `/health` 才回 404 —— 说明 8888 那台 JVM 根本没有这些 z-config 接口可查，需要 z-config 自己的服务起来。
- 闸的判据是 localStorage 里有没有 `zconfig_token` 这个 key（值仍是哨兵字面量），不是真登录态。

## 五、建议修法（等你点头再动代码，本文件不改任何源码）

1. 读型接口按 HTTP 语义分家：`pageConfig` / `listConfig` / `getConfig` / `history/page` / `audit/page` /
   `push/page` 改 `@GetMapping` + query 绑定，或至少保留一条 GET 影子路径，让缓存与只读护栏可用。
2. `/export` 的传参与其余接口统一（要么全 body，要么全 query），并明确它到底改不改状态。
3. `App.jsx` 菜单与路由表对齐：要么给 `/system` 补 `<Route path="system" element={<SystemSettings/>}/>`，
   要么删掉 `:39` 这条菜单项；顺带给 `<Routes>` 加 `path="*"` 兜底页，白屏变成可见的 404。
4. 行级删除/回滚按钮加二次确认层，使"只点不开层"的普查能覆盖到它（当前它们藏在 0 行的表格里，任何安全扫描都碰不到）。

## 六、复算命令

```bash
# 分母与静态路由表（不开浏览器，纯源码抽取）
cd ~/.cache/zopc_ui_sweep/sweep && python3 console_click_probe.py --routes-only | sed -n 's/^/  /p'

# 逐条拦截明细（本轮台账）
python3 - <<'PY'
import json
for l in open('/Users/zifang/.cache/zopc_ui_sweep/sweep/click_z-config_p2.jsonl', encoding='utf-8'):
    e = json.loads(l)
    if e.get('blocked'):
        print(e['route'], '/', e['label'], '->', e['blocked'])
PY

# 空白页这一条
python3 - <<'PY'
import json
for l in open('/Users/zifang/.cache/zopc_ui_sweep/sweep/click_z-config_p2.jsonl', encoding='utf-8'):
    e = json.loads(l)
    if e['ev'] == 'RENDER' and (e['controls'] == 0 or e['textLen'] == 0):
        print(e['route'], e['controls'], e['textLen'], e['errs'])
PY
```

另注：本仓工作树里 `z-config-web/.../MetricsController.java` 有一处**不属于本次普查**的未提交改动
（`git status --porcelain` 1 行，`stat` 实测 mtime=ctime=Sep 30 23:17:33，早于本轮起跑 9 小时），普查全程未触碰它。
