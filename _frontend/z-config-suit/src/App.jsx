import {Navigate, Route, Routes} from 'react-router-dom'
import {AppLayout} from '../../../../_shared/z-frontend-common-local/dist/z-frontend-common.es.js'
import {
    Dashboard, Login,
    menuItems, routeTable, isAuthenticated,
} from '@yuku123/z-config-component/pages'

// 壳只做组装（lead 005 §9.1）：鉴权 + AppLayout + manifest 路由。
const PrivateRoute = ({children}) => {
    return isAuthenticated() ? children : <Navigate to="/login" replace/>
}

export default function App() {
    return (
        <Routes>
            <Route path="/login" element={<Login/>}/>
            <Route path="/" element={
                <PrivateRoute>
                    <AppLayout menuItems={menuItems} appTitle="Z-Config 配置中心" appShort="CFG" appIcon={{icon: <img src="/icon.png" alt="CFG" style={{width: "100%", height: "100%", objectFit: "cover", borderRadius: 8}}/>, color: '#115e59', label: 'CFG'}}/>
                </PrivateRoute>
            }>
                <Route index element={<Dashboard/>}/>
                {routeTable.map((r) => (
                    <Route key={r.path} path={r.path} element={<r.Component/>}/>
                ))}
            </Route>
        </Routes>
    )
}
