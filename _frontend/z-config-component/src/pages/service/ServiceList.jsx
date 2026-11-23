import {useEffect, useRef, useState} from 'react'
import {Button, Descriptions, Drawer, Spin, Table, Tag} from 'antd'
import {ReloadOutlined} from '@ant-design/icons'
import {namingApi} from '../../services/api'
import {EmptyState, ErrorState, ListTable} from '@yuku123/z-frontend-common/ui'

const wrapText = {overflowWrap: 'anywhere'}
const serviceKey = service => JSON.stringify([service.serviceName, service.group, service.namespace])
const flag = value => value == null ? '—' : <Tag color={value ? 'success' : 'default'}>{value ? '是' : '否'}</Tag>
const instanceColumns = [
    {title: '实例 ID', dataIndex: 'instanceId', width: 250},
    {title: 'IP / 端口', key: 'address', width: 180, render: (_, row) => `${row.ip ?? '—'} / ${row.port ?? '—'}`},
    {title: '集群', dataIndex: 'clusterName', width: 160},
    {title: '健康', dataIndex: 'healthy', width: 80, render: flag},
    {title: '启用', dataIndex: 'enabled', width: 80, render: flag},
    {title: '临时实例', dataIndex: 'ephemeral', width: 100, render: flag},
    {title: '权重', dataIndex: 'weight', width: 80},
    {
        title: '元数据', dataIndex: 'metadata', width: 300,
        render: metadata => metadata == null ? '—' : (
            <details>
                <summary style={{cursor: 'pointer'}}>查看元数据</summary>
                <pre style={{whiteSpace: 'pre-wrap', ...wrapText, margin: '8px 0', maxHeight: 240, overflow: 'auto'}}>
                    {typeof metadata === 'string' ? metadata : JSON.stringify(metadata, null, 2)}
                </pre>
            </details>
        ),
    },
]

const ServiceList = () => {
    const [loading, setLoading] = useState(false)
    const [data, setData] = useState([])
    const [loadError, setLoadError] = useState(null)
    const [selectedService, setSelectedService] = useState(null)
    const [instances, setInstances] = useState([])
    const [instancesLoading, setInstancesLoading] = useState(false)
    const [instancesError, setInstancesError] = useState(null)
    const mounted = useRef(false)
    const listGeneration = useRef(0)
    const instanceGeneration = useRef(0)

    const fetchServiceList = async () => {
        const generation = ++listGeneration.current
        const isCurrent = () => mounted.current && generation === listGeneration.current
        setLoading(true)
        setLoadError(null)
        try {
            const result = await namingApi.listServices()
            if (!isCurrent()) return
            if (!Array.isArray(result)) throw new Error('获取服务列表失败：响应不是数组')
            setData(result)
            setLoadError(null)
        } catch (error) {
            if (!isCurrent()) return
            setData([])
            setLoadError(error)
        } finally {
            if (isCurrent()) setLoading(false)
        }
    }

    const fetchInstances = async service => {
        const generation = ++instanceGeneration.current
        const isCurrent = () => mounted.current && generation === instanceGeneration.current
        setInstancesLoading(true)
        setInstancesError(null)
        setInstances([])
        try {
            const result = await namingApi.getInstances(service.serviceName, service.group ?? 'DEFAULT_GROUP', service.namespace ?? '')
            if (!isCurrent()) return
            setInstances(result)
            setInstancesError(null)
        } catch (error) {
            if (isCurrent()) setInstancesError(error)
        } finally {
            if (isCurrent()) setInstancesLoading(false)
        }
    }

    const openDetails = service => {
        setSelectedService(service)
        fetchInstances(service)
    }
    const closeDetails = () => {
        // Closing must invalidate requests even when the next selection is the same service.
        ++instanceGeneration.current
        setSelectedService(null)
        setInstances([])
        setInstancesError(null)
        setInstancesLoading(false)
    }

    useEffect(() => {
        mounted.current = true
        fetchServiceList()
        return () => {
            mounted.current = false
            ++listGeneration.current
            ++instanceGeneration.current
        }
    }, [])

    const columns = [
        {title: '服务名', dataIndex: 'serviceName', key: 'serviceName', width: 280},
        {title: '分组', dataIndex: 'group', key: 'group', width: 180},
        {title: '命名空间', dataIndex: 'namespace', key: 'namespace', width: 180},
        {
            title: '健康实例', dataIndex: 'healthyInstanceCount', key: 'healthyInstanceCount', width: 100,
            render: count => <Tag color="success">{count}</Tag>,
        },
        {title: '总实例', dataIndex: 'totalInstanceCount', key: 'totalInstanceCount', width: 90},
        {
            title: '状态', dataIndex: 'status', key: 'status', width: 100,
            render: status => <Tag color={status === '健康' ? 'success' : 'error'}>{status}</Tag>,
        },
        {
            title: '操作', key: 'action', width: 90,
            render: (_, record) => <Button type="link" size="small" onClick={() => openDetails(record)}>详情</Button>,
        },
    ]

    return (
        <div style={{minWidth: 0, ...wrapText}}>
            {loadError && (
                <ErrorState
                    error={loadError}
                    title="服务列表没有读到"
                    onRetry={fetchServiceList}
                    style={{marginBottom: 16}}
                />
            )}
            <ListTable
                cardTitle="服务列表"
                rows={data}
                columns={columns}
                rowKey={serviceKey}
                loading={loading}
                searchKeys="serviceName,group,namespace,status"
                searchWidth="min(220px, 35vw)"
                searchPlaceholder="按服务名 / 分组 / 命名空间 / 状态过滤"
                cardProps={{styles: {header: {flexWrap: 'wrap', gap: 12, paddingBlock: 16}, body: {minWidth: 0}}}}
                tableProps={{scroll: {x: 1020}, tableLayout: 'fixed'}}
                cardExtra={
                    <Button icon={<ReloadOutlined spin={loading}/>} onClick={fetchServiceList} aria-busy={loading}>
                        刷新
                    </Button>
                }
                emptyTitle={loadError ? '数据没有读到' : '暂无服务'}
                emptyDescription={loadError ? '上方错误条已说明原因，点「重新加载」重试。' : '当前没有注册的服务'}
            />
            <Drawer
                title="服务实例详情"
                placement="right"
                size="min(1120px, 100vw)"
                open={!!selectedService}
                onClose={closeDetails}
                destroyOnHidden
                styles={{body: {minWidth: 0, ...wrapText}}}
                extra={<Button icon={<ReloadOutlined spin={instancesLoading}/>} onClick={() => selectedService && fetchInstances(selectedService)} aria-busy={instancesLoading}>刷新实例</Button>}
            >
                {selectedService && (
                    <>
                        <Descriptions column={1} size="small" style={{marginBottom: 24}} items={[
                            {key: 'service', label: '服务名', children: selectedService.serviceName},
                            {key: 'group', label: '分组', children: selectedService.group ?? 'DEFAULT_GROUP'},
                            {key: 'namespace', label: '命名空间', children: selectedService.namespace || '（空）'},
                        ]}/>
                        {instancesLoading ? (
                            <div role="status" style={{padding: 32, textAlign: 'center'}}><Spin/><div>正在加载实例…</div></div>
                        ) : instancesError ? (
                            <ErrorState error={instancesError} title="服务实例没有读到" onRetry={() => fetchInstances(selectedService)}/>
                        ) : instances.length === 0 ? (
                            <EmptyState title="暂无实例" description="该服务在当前分组和命名空间下没有注册实例。"/>
                        ) : (
                            <Table
                                size="small"
                                columns={instanceColumns}
                                dataSource={instances}
                                rowKey={row => row.instanceId ?? JSON.stringify([row.ip, row.port, row.clusterName])}
                                scroll={{x: 1230}}
                                tableLayout="fixed"
                                pagination={{defaultPageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50', '100'], showTotal: total => `共 ${total} 条`}}
                            />
                        )}
                    </>
                )}
            </Drawer>
        </div>
    )
}

export default ServiceList
