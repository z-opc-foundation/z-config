import {useEffect, useState} from 'react'
import {Badge, Button, message, Space, Tag} from 'antd'
import {CloudServerOutlined, EyeOutlined, ReloadOutlined} from '@ant-design/icons'
import {useNavigate} from 'react-router-dom'
import {namingApi} from '@/services/api'
import {ErrorState, ListTable, PageHeader} from '@yuku123/z-frontend-common/ui'

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
            {error ? (
                <ErrorState error={error} onRetry={fetch} type="inline"/>
            ) : (
                /* 整表一次拉回、翻页在前端做 ⇒ 走基座 ListTable：
                   分页/过滤/空态三件套由基座统一承接，这里只声明按哪几个字段过滤。 */
                <ListTable
                    rows={data}
                    columns={columns}
                    loading={loading}
                    rowKey={(record) => record?.id ?? record?.key ?? record?.serviceName}
                    searchKeys="serviceName,group,namespace"
                    searchPlaceholder="按服务名 / 分组 / 命名空间过滤"
                    cardExtra={<Button icon={<ReloadOutlined/>} onClick={fetch} loading={loading}>刷新</Button>}
                    emptyTitle="暂无服务"
                    emptyDescription="当前没有注册的服务"
                />
            )}
        </div>
    )
}

export default ServiceList
