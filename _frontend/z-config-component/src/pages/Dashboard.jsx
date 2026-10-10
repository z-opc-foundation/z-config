import {useEffect, useState} from 'react'
import {Card, Col, message, Row, Spin, Statistic} from 'antd'
import {ApartmentOutlined, CloudServerOutlined, ClusterOutlined, FileTextOutlined} from '@ant-design/icons'
import {configApi} from '@/config/services/api'

const Dashboard = () => {
    const [stats, setStats] = useState({configCount: 0, serviceCount: 0, instanceCount: 0, namespaceCount: 0})
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        (async () => {
            try {
                const data = await configApi.getStats()
                if (data) setStats(data)
            } catch (e) {
                message.error('获取统计数据失败')
            } finally {
                setLoading(false)
            }
        })()
    }, [])

    return (
        <Spin spinning={loading}>
            <Row gutter={[16, 16]}>
                <Col xs={24} sm={12} lg={6}><Card><Statistic title="配置总数" value={stats.configCount}
                                                             prefix={<FileTextOutlined/>}
                                                             styles={{content: {color: '#1890ff'}}}/></Card></Col>
                <Col xs={24} sm={12} lg={6}><Card><Statistic title="注册服务" value={stats.serviceCount}
                                                             prefix={<CloudServerOutlined/>}
                                                             styles={{content: {color: '#52c41a'}}}/></Card></Col>
                <Col xs={24} sm={12} lg={6}><Card><Statistic title="运行实例" value={stats.instanceCount}
                                                             prefix={<ClusterOutlined/>}
                                                             styles={{content: {color: '#fa8c16'}}}/></Card></Col>
                <Col xs={24} sm={12} lg={6}><Card><Statistic title="命名空间" value={stats.namespaceCount}
                                                             prefix={<ApartmentOutlined/>}
                                                             styles={{content: {color: '#f5222d'}}}/></Card></Col>
            </Row>
        </Spin>
    )
}

export default Dashboard
