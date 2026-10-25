import {Navigate, Route, Routes} from 'react-router-dom'
import Dashboard from './Dashboard'
import ConfigList from './ConfigList'
import ConfigEdit from './ConfigEdit'
import ConfigHistory from './ConfigHistory'
import Cluster from './cluster'
import ServiceList from './ServiceList'
import ServiceDetail from './ServiceDetail'
import NamespaceList from './namespace/NamespaceList'

export default function ConfigIndex() {
    return (
        <Routes>
            <Route index element={<Navigate to="list" replace/>}/>
            <Route path="list" element={<ConfigList/>}/>
            <Route path="edit" element={<ConfigEdit/>}/>
            <Route path="history" element={<ConfigHistory/>}/>
            <Route path="dashboard" element={<Dashboard/>}/>
            <Route path="cluster" element={<Cluster/>}/>
            <Route path="service" element={<ServiceList/>}/>
            <Route path="service/detail" element={<ServiceDetail/>}/>
            {/* 命名空间（2026-10-25 接真页面）：
                之前 z-opc 主壳的「命名空间 / 灰度」菜单项是**占位**（渲染一段说明文字），
                理由写着「缺的不是数据，是路由」——
                数据源其实早就有了：GET /api/namespace/list 实测 200 且有 1 行
                （namespace_id=DEFAULT_NAMESPACE），写面 create/update/delete 也都在册
                （实测 400/510 级校验错误 ⇒ 端点在、只是参数没给对）。
                ⇒ 现在把真页面挂上；列名/请求体键名的 snake_case 口径见
                  pages/namespace/NamespaceList.jsx 头部注释。 */}
            <Route path="namespace" element={<NamespaceList/>}/>
        </Routes>
    )
}
