import {useEffect, useState} from 'react'
import {Button, Card, message, Space, Spin, Table, Tag} from 'antd'
import {EyeOutlined, ReloadOutlined} from '@ant-design/icons'
import request from '../../services/request'

const ServiceList = () => {
    const [loading, setLoading] = useState(false)
    const [data, setData] = useState([])

    // 获取服务列表
    // 共享 request 已按标准 {code, data, message} 解包：result 就是数组本身
    // 
    const fetchServiceList = async () => {
        setLoading(true)
        try {
            const result = await request.get('/api/naming/listServices')
            if (Array.isArray(result)) {
                setData(result.map((item, index) => ({key: index + 1, ...item})))
            } else {
                message.error('获取服务列表失败')
                setMockData()
            }
        } catch (error) {
            console.error('获取服务列表错误:', error)
            message.error(error.message || '网络错误')
            setMockData()
        } finally {
            setLoading(false)
        }
    }

    // 模拟数据（当后端接口不可用时）
    const setMockData = () => {
        setData([
            {
                key: '1',
                serviceName: 'user-service',
                group: 'DEFAULT_GROUP',
                namespace: 'public',
                healthyInstanceCount: 3,
                totalInstanceCount: 3,
                status: '健康',
            },
            {
                key: '2',
                serviceName: 'order-service',
                group: 'DEFAULT_GROUP',
                namespace: 'public',
                healthyInstanceCount: 2,
                totalInstanceCount: 2,
                status: '健康',
            },
        ])
    }

    useEffect(() => {
        fetchServiceList()
    }, [])

    const columns = [
        {title: '服务名', dataIndex: 'serviceName', key: 'serviceName'},
        {title: '分组', dataIndex: 'group', key: 'group'},
        {title: '命名空间', dataIndex: 'namespace', key: 'namespace'},
        {
            title: '健康实例',
            dataIndex: 'healthyInstanceCount',
            key: 'healthyInstanceCount',
            render: (count) => <Tag color="success">{count}</Tag>,
        },
        {title: '总实例', dataIndex: 'totalInstanceCount', key: 'totalInstanceCount'},
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status) => (
                <Tag color={status === '健康' ? 'success' : 'error'}>{status}</Tag>
            ),
        },
        {
            title: '操作',
            key: 'action',
            render: (_, record) => (
                <Space>
                    <Button type="link" icon={<EyeOutlined/>}>详情</Button>
                </Space>
            ),
        },
    ]

    return (
        <div>
            <Card
                title="服务列表"
                extra={
                    <Button
                        icon={<ReloadOutlined/>}
                        onClick={fetchServiceList}
                        loading={loading}
                    >
                        刷新
                    </Button>
                }
            >
                <Spin spinning={loading}>
                    <Table
                        columns={columns}
                        dataSource={data}
                        pagination={{
                            pageSize: 10,
                            showSizeChanger: true,
                            showTotal: (total) => `共 ${total} 条`,
                        }}
                    />
                </Spin>
            </Card>
        </div>
    )
}

export default ServiceList
