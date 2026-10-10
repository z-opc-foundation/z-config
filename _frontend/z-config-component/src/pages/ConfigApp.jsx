import {Navigate, Route, Routes} from 'react-router-dom'
import Dashboard from './Dashboard'
import ConfigList from './ConfigList'
import ConfigEdit from './ConfigEdit'
import ConfigHistory from './ConfigHistory'
import Cluster from './cluster'
import ServiceList from './ServiceList'
import ServiceDetail from './ServiceDetail'

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
        </Routes>
    )
}
