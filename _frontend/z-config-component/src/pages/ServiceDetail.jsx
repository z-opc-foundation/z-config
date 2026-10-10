import {useEffect, useState} from 'react'
import {useNavigate, useSearchParams} from 'react-router-dom'
import {Badge, Button, Card, Descriptions, message, Space, Table, Tag, Tooltip} from 'antd'
import {CheckCircleOutlined, CloseCircleOutlined, ReloadOutlined} from '@ant-design/icons'
import {namingApi} from '@/config/services/api'
import {EmptyState, ErrorState, PageHeader} from '@/common/components/ui'

const ServiceDetail = () => {
    const [searchParams] = useSearchParams()
    const navigate = useNavigate()
    const fullName = searchParams.get('serviceName') || ''
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState(null)
    const [instances, setInstances] = useState([])

    const fetchInstances = async () => {
        setLoading(true)
        try {
            // serviceName 格式: DEFAULT_GROUP@@com.zifang.user.UserService → 拆分传参
            const parts = fullName?.split('@@') || ['', '']
            const svcGroup = parts[0]
            const svcName = parts.slice(1).join('@@')
            const list = await namingApi.getInstances(svcName, svcGroup)
            if (list) setInstances((list || []).map((item, i) => ({key: item.instanceId || i, ...item})))
        } catch (e) {
            message.error('获取实例失败')
            setError(e)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        if (fullName) fetchInstances()
    }, [fullName])

    const healthyCount = instances.filter(i => i.healthy).length
    const unhealthyCount = instances.filter(i => !i.healthy).length
    // parse serviceName: group@@name → group, name
    const nameParts = fullName?.split('@@') || ['', '']
    const displayGroup = nameParts[0]
    const displayName = nameParts.slice(1).join('@@')

    const columns = [
        {title: '实例ID', dataIndex: 'instanceId', key: 'instanceId', ellipsis: true, width: 220},
        {title: 'IP', dataIndex: 'ip', key: 'ip', width: 130},
        {title: '端口', dataIndex: 'port', key: 'port', width: 70},
        {title: '集群', dataIndex: 'clusterName', key: 'clusterName', width: 100},
        {title: '权重', dataIndex: 'weight', key: 'weight', width: 60},
        {
            title: '健康状态', dataIndex: 'healthy', key: 'healthy', width: 100,
            render: (h) => h ? <Badge status="success" text="健康"/> : <Badge status="error" text="异常"/>
        },
        {
            title: '类型', dataIndex: 'ephemeral', key: 'ephemeral', width: 80,
            render: (e) => <Tag>{e ? '临时' : '持久'}</Tag>
        },
        {
            title: '元数据', dataIndex: 'metadata', key: 'metadata', ellipsis: true,
            render: (m) => {
                if (!m) return '-'
                const str = typeof m === 'object' ? JSON.stringify(m) : m
                return <Tooltip title={<pre style={{
                    margin: 0,
                    fontSize: 12
                }}>{JSON.stringify(typeof m === 'object' ? m : JSON.parse(m), null, 2)}</pre>}><span
                    style={{cursor: 'pointer'}}>{str}</span></Tooltip>
            }
        },
    ]

    return (
        <div>
            <PageHeader title="服务详情" back={() => navigate('/config/service')}/>
            <Card size="small" style={{marginBottom: 16}}>
                <Space>
                    <span style={{fontSize: 16, fontWeight: 600}}>{displayName || fullName}</span>
                    <Tag>{displayGroup || 'DEFAULT_GROUP'}</Tag>
                </Space>
            </Card>
            <Card size="small" style={{marginBottom: 16}}>
                <Descriptions size="small" column={4}>
                    <Descriptions.Item label="总实例"><span
                        style={{fontSize: 20, fontWeight: 600}}>{instances.length}</span></Descriptions.Item>
                    <Descriptions.Item label="健康">
                        <span style={{color: '#52c41a', fontSize: 20, fontWeight: 600}}>{healthyCount}</span>
                        <CheckCircleOutlined style={{color: '#52c41a', marginLeft: 4}}/>
                    </Descriptions.Item>
                    <Descriptions.Item label="异常">
                        <span style={{
                            color: unhealthyCount > 0 ? '#ff4d4f' : undefined,
                            fontSize: 20,
                            fontWeight: 600
                        }}>{unhealthyCount}</span>
                        {unhealthyCount > 0 ? <CloseCircleOutlined style={{color: '#ff4d4f', marginLeft: 4}}/> : null}
                    </Descriptions.Item>
                    <Descriptions.Item label="集群数">
                        {new Set(instances.map(i => i.clusterName)).size}
                    </Descriptions.Item>
                </Descriptions>
            </Card>
            <Card title="实例列表"
                  extra={<Button icon={<ReloadOutlined/>} onClick={fetchInstances} loading={loading}>刷新</Button>}>
                {error ? (
                    <ErrorState error={error} onRetry={fetchInstances} type="inline"/>
                ) : (
                    <Table columns={columns} dataSource={instances} loading={loading}
                           rowKey={(record, index) => record?.id ?? record?.key ?? `row-${index}`}
                           pagination={{pageSize: 10, showTotal: (t) => `共 ${t} 个实例`}}
                           locale={{
                               emptyText: (
                                   <EmptyState title="暂无实例" description="服务可能尚未注册实例或实例已下线"/>
                               ),
                           }}/>
                )}
            </Card>
        </div>
    )
}

export default ServiceDetail
