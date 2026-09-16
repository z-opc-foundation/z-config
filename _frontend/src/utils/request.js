/**
 * z-config 请求实例
 *
 * 从 @yuku123/z-frontend-common 工厂创建。
 * z-config 后端实际是 com.zifang.util.core.meta.Result<T>，同时含 success + code 字段，
 * 与共享默认解包器（基于 code 字段）完全兼容，无需 custom unwrap。
 *
 * 配置：
 *   - baseURL ''  →  路径走 /api/cluster、/api/dashboard、/api/naming、/api/config、/api/config-auth
 *   - token key 保留 'zconfig_token'（与历史兼容）
 *   - userInfo key 保留 'zconfig_user'
 */
import {createRequest} from '@yuku123/z-frontend-common'

const request = createRequest({
    baseURL: '',
    tokenKey: 'zconfig_token',
    userInfoKey: 'zconfig_user',
})

export default request

// 兼容旧 setToken / clearToken 导出（Login 页面使用过）
export function setToken(token) {
    localStorage.setItem('zconfig_token', token)
}

export function clearToken() {
    localStorage.removeItem('zconfig_token')
    localStorage.removeItem('zconfig_user')
}
