import {useEffect, useState} from 'react'
import {Card, Col, message, Row, Spin, Statistic} from 'antd'
import {ApartmentOutlined, CloudServerOutlined, ClusterOutlined, FileTextOutlined,} from '@ant-design/icons'
import axios from 'axios'

const Dashboard = () => {
    const [stats, setStats] = useState({
        configCount: 0,
        serviceCount: 0,
        instanceCount: 0,
        namespaceCount: 0,
    })
    const [loading, setLoading] = useState(true)

    // 从后端获取真实统计数据
    useEffect(() => {
        const fetchStats = async () => {
            try {
                const res = await axios.get('/api/dashboard/stats')
                setStats(res.data)
            } catch (e) {
                message.error('获取统计数据失败')
                console.error(e)
            } finally {
                setLoading(false)
            }
        }
        fetchStats()
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
                                title="命名空间"
                                value={stats.namespaceCount}
                                prefix={<ApartmentOutlined/>}
                                valueStyle={{color: '#f5222d'}}
                            />
                        </Card>
                    </Col>
                </Row>
            </Spin>
        </div>
    )
}

export default Dashboard
