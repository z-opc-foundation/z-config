import {Navigate, Route, Routes} from 'react-router-dom'
import {AppLayout} from '@yuku123/z-frontend-common'
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
import './App.css'

// 简单的登录检查
const isAuthenticated = () => {
    return localStorage.getItem('zconfig_token') || sessionStorage.getItem('zconfig_token')
}

// 受保护的路由
const PrivateRoute = ({children}) => {
    return isAuthenticated() ? children : <Navigate to="/login" replace/>
}

const menuItems = [
    {key: '/', icon: <DashboardOutlined/>, label: '概览'},
    {key: '/config/list', icon: <FileTextOutlined/>, label: '配置管理 / 列表'},
    {key: '/config/history', icon: <FileTextOutlined/>, label: '配置管理 / 变更历史'},
    {key: '/service/list', icon: <CloudServerOutlined/>, label: '服务管理 / 服务列表'},
    {key: '/namespace', icon: <ApartmentOutlined/>, label: '命名空间'},
    {key: '/system', icon: <SettingOutlined/>, label: '系统设置'},
]

function App() {
    return (
        <div className="app">
            <Routes>
                <Route path="/login" element={<Login/>}/>
                <Route path="/" element={
                    <PrivateRoute>
                        <AppLayout menuItems={menuItems} appTitle="Z-Config 配置中心" appShort="CFG"/>
                    </PrivateRoute>
                }>
                    <Route index element={<Dashboard/>}/>
                    <Route path="dashboard" element={<Dashboard/>}/>
                    <Route path="config/list" element={<ConfigList/>}/>
                    <Route path="config/edit" element={<ConfigEdit/>}/>
                    <Route path="config/history" element={<ConfigHistory/>}/>
                    <Route path="service/list" element={<ServiceList/>}/>
                </Route>
            </Routes>
        </div>
    )
}

export default App
