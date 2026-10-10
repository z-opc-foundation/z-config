import {useEffect, useState} from 'react'
import {Badge, Button, Card, message, Space, Table, Tag} from 'antd'
import {CloudServerOutlined, EyeOutlined, ReloadOutlined} from '@ant-design/icons'
import {useNavigate} from 'react-router-dom'
import {namingApi} from '@/config/services/api'
import {EmptyState, ErrorState, PageHeader} from '@/common/components/ui'

const ServiceList = () => {
    const navigate = useNavigate()
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState(null)
    const [data, setData] = useState([])

    const fetch = async () => {
        setLoading(true)
        try {
            const list = await namingApi.listServices()
            setData((list || []).map((item, i) => ({key: item.serviceName || i, ...item})))
        } catch (e) {
            message.error('获取失败')
            setError(e)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetch()
    }, [])

    const columns = [
        {
            title: '服务名', dataIndex: 'serviceName', key: 'serviceName',
            render: (v) => <Space><CloudServerOutlined style={{color: '#1677ff'}}/><span
                style={{fontWeight: 500}}>{v}</span></Space>
        },
        {
            title: '分组', dataIndex: 'group', key: 'group', width: 140,
            render: (v) => <Tag>{v}</Tag>
        },
        {title: '命名空间', dataIndex: 'namespace', key: 'namespace', width: 160},
        {
            title: '集群数', dataIndex: 'clusterMap', key: 'clusterCount', width: 80,
            render: (v) => {
                try {
                    return Object.keys(JSON.parse(v || '{}')).length
                } catch {
                    return 0
                }
            }
        },
        {
            title: '实例数', dataIndex: 'clusterMap', key: 'instanceCount', width: 80,
            render: (v) => {
                try {
                    const cm = JSON.parse(v || '{}')
                    return Object.values(cm).flat().length
                } catch {
                    return 0
                }
            }
        },
        {
            title: '健康状态', key: 'health', width: 100,
            render: (_, r) => {
                try {
                    const cm = JSON.parse(r.clusterMap || '{}')
                    const instances = Object.values(cm).flat()
                    const allHealthy = instances.every(i => i.healthy !== false)
                    const hasUnhealthy = instances.some(i => i.healthy === false)
                    return allHealthy ? <Badge status="success" text="健康"/> : hasUnhealthy ?
                        <Badge status="error" text="异常"/> : <Badge status="default" text="未知"/>
                } catch {
                    return <Badge status="default" text="未知"/>
                }
            }
        },
        {
            title: '操作', key: 'action', width: 100,
            render: (_, record) => (
                <Button type="link" size="small" icon={<EyeOutlined/>}
                        onClick={() => navigate(`/config/service/detail?serviceName=${record.serviceName}`)}>详情</Button>
            ),
        },
    ]

    return (
        <div>
            <PageHeader title="服务列表"/>
            <Card extra={<Button icon={<ReloadOutlined/>} onClick={fetch} loading={loading}>刷新</Button>}>
                {error ? (
                    <ErrorState error={error} onRetry={fetch} type="inline"/>
                ) : (
                    <Table columns={columns} dataSource={data} loading={loading}
                           rowKey={(record) => record?.id ?? record?.key ?? record?.serviceName}
                           pagination={{showSizeChanger: true, showTotal: (t) => `共 ${t} 个服务`}}
                           locale={{
                               emptyText: (
                                   <EmptyState title="暂无服务" description="当前没有注册的服务"/>
                               ),
                           }}/>
                )}
            </Card>
        </div>
    )
}

export default ServiceList
