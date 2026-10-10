/**
 * config 模块的 API 客户端。
 *
 * 2026-10-04 瘦身：原先本文件是 `src/common/services/api.js` 的一份完整复制品
 * （28 个 makeApi 产物 + 项目/任务/鉴权一组函数 + 4 个假数据桩），
 * 全仓逐条解析 import 后确认**只有 configApi / namingApi 两个导出被引用**
 * （导入方 7 个，全在 config/pages/ 下）。其余 48 个导出零引用。
 *
 * 真正在用的那份是 `src/common/services/api.js`——`src/App.jsx` 等引的是它。
 * 同名的 `mcpApi` / `ctcAcOrgApi` / `getDomainByTenantCode` 等在 common、
 * agent/api、ctc/components 各有自己的定义，删这里不影响它们。
 *
 * 判定口径：按 bundler 规则解析每个 import 的实际路径（@/ → src/，
 * ./ 与 ../ 逐级上溯，扩展名顺序沿用 Vite 默认），而不是按名字 grep——
 * 早期用「名字是否在全仓出现过」判断过一轮，结果 147 个导出全部误判为「活」，
 * 因为这三个文件的导出名与其他模块大量重名。
 */
import {request} from '@/common'

export const configApi = {
    // === Cluster (/api/cluster/*) ===
    // ⚠️ 集群 ≠ 命名空间，且 /api/cluster/list 实测 200 但 0 行 (z_cluster 空表) —— 不要再拿它填命名空间下拉。
    clusterList: () => request.get('/cluster/list'),
    clusterSave: (data) => request.post('/cluster/save', data),
    clusterDelete: (id) => request.post('/cluster/delete', null, {params: {id}}),

    // === Namespace / Group ===
    // 实测: namespaceList => ["public"], groupList => ["TEST"]。
    namespaceList: () => request.get('/config/namespaceList'),
    groupList: () => request.get('/config/groupList'),

    // === Config (/api/config/*) ===
    // 🔴 请求体键名：合并进程的全局 SNAKE_CASE 对**双向** bean 属性生效，
    //   实测 POST /api/config/pageConfig 发 {"nameSpace":"no_such_ns"} 时筛选被整个丢掉（返回全表），
    //   发 {"name_space":"no_such_ns"} 才真的过滤成 0 行。所以这里的多词键一律 snake_case。
    //   依据: ZConfigPageRequest{nameSpace,group,dataId,search,appName} + 21:0x 实测。
    listConfig: (params) => request.get('/config/list', {params}),   // handler=listAlias(String,String)，走 query，不受改名影响
    pageConfig: (params) => request.post('/config/pageConfig', params),
    getConfig: (params) => request.post('/config/getConfig', params),
    saveConfig: (data) => request.post('/config/saveConfig', data),
    // 后端 handler 是 deleteConfig(ZConfigQueryRequest)（@RequestBody），原来发 query ⇒ body 为空，
    // 三个字段全 null ⇒ 只能返回"配置不存在"。改成发 body。⚠️ 未在共享库上实测（不拿真库做删除）。
    deleteConfig: (data) => request.delete('/config/delete', {data}),

    // === History ===
    // ZConfigHistoryPageRequest 只有 search / group 可用（namespace 是 Long，传字符串 400），
    // 分页字段继承 PageRequest{current,size} —— 实测 pageNum/pageSize 被忽略，整表返回。
    historyPage: (params) => request.post('/config/history/page', params),
    rollback: (data) => request.post('/config/rollback', data),

    // === Dashboard ===
    getStats: () => request.get('/dashboard/stats'),

    // === 原始 CRUD (保留为兼用接口) ===
    list: (params) => request.get('/config/list', {params}),
    page: (params) => request.get('/config/page', {params}),
    get: (id, params) => request.get('/config', {params: {dataId: id, ...(params || {})}}),
    create: (data) => request.post('/config', data),
    update: (id, data) => request.put('/config', data, {params: {dataId: id}}),
    delete: (id) => request.delete('/config', {params: {dataId: id}}),
}

export const namingApi = {
    listServices: () => request.get('/naming/listServices'),
    getInstances: (serviceName, groupName) =>
        request.get('/naming/getAllInstances', {params: {serviceName, groupName}}),
    getAllInstances: () => request.get('/naming/getAllInstances'),
    selectInstancesHealthy: (serviceName, groupName) =>
        request.get('/naming/selectInstances/healthy', {params: {serviceName, groupName}}),
    selectOneHealthyInstance: (serviceName, groupName) =>
        request.get('/naming/selectOneHealthyInstance', {params: {serviceName, groupName}}),
    registerInstance: (data) => request.post('/naming/registerInstance', data),
    deregisterInstance: (data) => request.post('/naming/deregisterInstance', data),
    subscribe: (data) => request.post('/naming/subscribe', data),
    unsubscribe: (data) => request.post('/naming/unsubscribe', data),
}
