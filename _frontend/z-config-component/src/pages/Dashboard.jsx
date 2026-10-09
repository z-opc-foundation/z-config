import {useEffect, useState} from 'react'
import {Card, Col, Descriptions, message, Row, Spin, Statistic, Tag} from 'antd'
import {
    ApartmentOutlined,
    ApiOutlined,
    CloudServerOutlined,
    ClusterOutlined,
    DashboardOutlined,
    FileTextOutlined,
    HeartOutlined,
    NodeIndexOutlined,
} from '@ant-design/icons'
import axios from 'axios'

const Dashboard = () => {
    const [stats, setStats] = useState({
        configCount: 0,
        serviceCount: 0,
        instanceCount: 0,
        namespaceCount: 0,
        activeListeners: 0,
    })
    const [nodeInfo, setNodeInfo] = useState(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const fetchStats = async () => {
            try {
                const [statsRes, nodeRes] = await Promise.all([
                    axios.get('/api/dashboard/stats'),
                    axios.get('/api/dashboard/node'),
                ])
                setStats(statsRes.data)
                setNodeInfo(nodeRes.data)
            } catch (e) {
                message.error('获取统计数据失败')
                console.error(e)
            } finally {
                setLoading(false)
            }
        }
        fetchStats()
        // 每 30 秒刷新一次
        const timer = setInterval(fetchStats, 30000)
        return () => clearInterval(timer)
    }, [])

    return (
        <div>
            <Spin spinning={loading}>
                <Row gutter={[16, 16]}>
                    <Col xs={24} sm={12} lg={6}>
                        <Card>
                            <Statistic
                                title="配置总数"
                                value={stats.configCount}
                                prefix={<FileTextOutlined/>}
                                valueStyle={{color: '#1890ff'}}
                            />
                        </Card>
                    </Col>
                    <Col xs={24} sm={12} lg={6}>
                        <Card>
                            <Statistic
                                title="注册服务"
                                value={stats.serviceCount}
                                prefix={<CloudServerOutlined/>}
                                valueStyle={{color: '#52c41a'}}
                            />
                        </Card>
                    </Col>
                    <Col xs={24} sm={12} lg={6}>
                        <Card>
                            <Statistic
                                title="运行实例"
                                value={stats.instanceCount}
                                prefix={<ClusterOutlined/>}
                                valueStyle={{color: '#fa8c16'}}
                            />
                        </Card>
                    </Col>
                    <Col xs={24} sm={12} lg={6}>
                        <Card>
                            <Statistic
                                title="活跃监听"
                                value={stats.activeListeners || 0}
                                prefix={<ApiOutlined/>}
                                valueStyle={{color: '#722ed1'}}
                            />
                        </Card>
                    </Col>
                </Row>

                {nodeInfo && (
                    <Row gutter={[16, 16]} style={{marginTop: 16}}>
                        <Col span={24}>
                            <Card title={<><DashboardOutlined/> 当前节点信息</>} size="small">
                                <Descriptions column={4} size="small">
                                    <Descriptions.Item label={<><NodeIndexOutlined/> IP</>}>
                                        {nodeInfo.ip}
                                    </Descriptions.Item>
                                    <Descriptions.Item label="端口">
                                        {nodeInfo.port}
                                    </Descriptions.Item>
                                    <Descriptions.Item label={<><HeartOutlined/> 状态</>}>
                                        <Tag color="green">{nodeInfo.status}</Tag>
                                    </Descriptions.Item>
                                    <Descriptions.Item label="活跃监听数">
                                        {nodeInfo.activeListeners}
                                    </Descriptions.Item>
                                </Descriptions>
                            </Card>
                        </Col>
                    </Row>
                )}
            </Spin>
        </div>
    )
}

export default Dashboard
