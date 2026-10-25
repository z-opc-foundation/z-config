/**
 * z-config 请求实例
 *
 * 从 @yuku123/z-frontend-common 工厂创建。
 * z-config 后端实际是 com.zifang.util.core.meta.Result<T>，同时含 success + code 字段，
 * 与共享默认解包器（基于 code 字段）完全兼容，无需 custom unwrap。
 *
 * 配置：
 *   - baseURL '/api' →  路径走 /api/cluster、/api/dashboard、/api/naming、/api/config、/api/config-auth
 *   - token key 保留 'zconfig_token'（与历史兼容）
 *   - userInfo key 保留 'zconfig_user'
 *
 * ⚠ **2026-10-25 修**：baseURL 原为**空串**，而 `src/services/api.js` 里的路径是
 *   `/config/**`、`/cluster/**`、`/naming/**`、`/dashboard/**`（**不带** /api 前缀）
 *   ⇒ axios 拼出来的实际 URL 是 `/config/history/page` 这样的**裸路径**。
 *   实测（生产 opc.zopc.top，2026-10-25）：
 *     · GET  /config/groupList        → **200 + text/html**（落 SPA 兜底页 ⇒ JSON 解析失败
 *                                      ⇒ 「选择Group」下拉永远是空的）
 *     · POST /config/history/page     → **405 Method Not Allowed**（Spring 静态资源处理器
 *                                      只认 GET/HEAD ⇒ POST 直接被拒）
 *   ⇒ 这就是「配置中心」组 5 个面板在平台里点开就报错的根因。
 *   ⚠ 后端 z-config-web 的映射全在 `/api/**` 下（`/api/config`·`/api/cluster`·
 *     `/api/naming`·`/api/dashboard`），且 z-config 独立壳（z-config-suit）的 vite 代理
 *     也只代理 `/api` ⇒ baseURL 必须是 `/api`（两个宿主都对）。
 */
import {createRequest} from '@yuku123/z-frontend-common'

const request = createRequest({
    baseURL: '/api',
    tokenKey: 'zconfig_token',
    userInfoKey: 'zconfig_user',
})

export default request

// 前缀参数注入（lead 005 section 9.3）：默认 '/api'（= 后端 z-config-web 的真实挂载前缀）。
// 宿主换挂载前缀时 configureConfig({ apiBase })，直接改写实例 defaults。
export function configureConfig(config) {
    if (config && config.apiBase !== undefined) {
        request.defaults.baseURL = config.apiBase
    }
}

// 兼容旧 setToken / clearToken 导出（Login 页面使用过）
export function setToken(token) {
    localStorage.setItem('zconfig_token', token)
}

export function clearToken() {
    localStorage.removeItem('zconfig_token')
    localStorage.removeItem('zconfig_user')
}
