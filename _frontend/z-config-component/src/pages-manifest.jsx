import {
    ApartmentOutlined,
    CloudServerOutlined,
    DashboardOutlined,
    FileTextOutlined,
    SettingOutlined,
} from '@ant-design/icons'
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
export const menuItems = [
    {key: '/', icon: <DashboardOutlined/>, label: '概览'},
    {key: '/config/list', icon: <FileTextOutlined/>, label: '配置管理 / 列表'},
    {key: '/config/history', icon: <FileTextOutlined/>, label: '配置管理 / 变更历史'},
    {key: '/service/list', icon: <CloudServerOutlined/>, label: '服务管理 / 服务列表'},
    {key: '/namespace', icon: <ApartmentOutlined/>, label: '命名空间'},
    {key: '/audit', icon: <FileTextOutlined/>, label: '审计日志'},
    {key: '/notification', icon: <FileTextOutlined/>, label: '推送通知'},
    {key: '/system', icon: <SettingOutlined/>, label: '系统设置'},
]


const routeTable = [
    {path: 'dashboard', Component: Dashboard},
    {path: 'config/list', Component: ConfigList},
    {path: 'config/edit', Component: ConfigEdit},
    {path: 'config/history', Component: ConfigHistory},
    {path: 'service/list', Component: ServiceList},
    {path: 'namespace', Component: NamespaceList},
    {path: 'audit', Component: AuditLog},
    {path: 'notification', Component: NotificationHistory},
]
export { routeTable }
export { default as Dashboard } from './pages/Dashboard'
export { default as Login } from './pages/Login'
