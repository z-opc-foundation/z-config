import { ApartmentOutlined, CloudServerOutlined, DashboardOutlined, FileTextOutlined, HomeOutlined, SettingOutlined } from '@ant-design/icons'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import ConfigList from './pages/config/ConfigList'
import ConfigEdit from './pages/config/ConfigEdit'
import ConfigHistory from './pages/config/ConfigHistory'
import ServiceList from './pages/service/ServiceList'
import NamespaceList from './pages/namespace/NamespaceList'
import AuditLog from './pages/audit/AuditLog'
import NotificationHistory from './pages/notification/NotificationHistory'

// 简单的登录检查
export const isAuthenticated = () => {
    return localStorage.getItem('zconfig_token') || sessionStorage.getItem('zconfig_token')
}

// 受保护的路由
/** 菜单 + 路由清单（lead 005 §8.2 manifest）。App 壳在 suit/宿主侧组装。 */


export { default as Dashboard } from './pages/Dashboard'
export { default as Login } from './pages/Login'
import HomePage from './pages/HomePage'
import ConfigApp from './pages/ConfigApp.jsx'

/** 菜单 + 路由清单（lead 008 §10/§14/§16 批量落地）。App 壳在 suit 侧组装。 */
export const appMeta = { title: 'Z-Config 配置中心', short: 'z-config' }

export const menuItems = [
    { key: '/z-config/home', label: '首页', icon: <HomeOutlined /> },
    { key: '/z-config/overview', label: '概览', icon: <DashboardOutlined /> },
    { key: '/z-config/config/list', label: '配置管理 / 列表', icon: <FileTextOutlined /> },
    { key: '/z-config/config/history', label: '配置管理 / 变更历史', icon: <FileTextOutlined /> },
    { key: '/z-config/service/list', label: '服务管理 / 服务列表', icon: <CloudServerOutlined /> },
    { key: '/z-config/namespace', label: '命名空间', icon: <ApartmentOutlined /> },
    { key: '/z-config/audit', label: '审计日志', icon: <FileTextOutlined /> },
    { key: '/z-config/notification', label: '推送通知', icon: <FileTextOutlined /> },
    { key: '/z-config/system', label: '系统设置', icon: <SettingOutlined /> },
]

export const routes = [
    { path: '/z-config/home', Component: HomePage },
    { path: '/z-config/dashboard', Component: Dashboard },
    { path: '/z-config/config/list', Component: ConfigList },
    { path: '/z-config/config/edit', Component: ConfigEdit },
    { path: '/z-config/config/history', Component: ConfigHistory },
    { path: '/z-config/service/list', Component: ServiceList },
    { path: '/z-config/namespace', Component: NamespaceList },
    { path: '/z-config/audit', Component: AuditLog },
    { path: '/z-config/notification', Component: NotificationHistory },
    { path: '/z-config/:rest*', Component: ConfigApp },
]

export { default as HomePage } from './pages/HomePage'
export { default as LoginPage } from './pages/LoginPage'
