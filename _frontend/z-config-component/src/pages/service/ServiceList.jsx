import {useEffect, useState} from 'react'
import {Button, Space, Tag} from 'antd'
import {EyeOutlined, ReloadOutlined} from '@ant-design/icons'
import request from '../../services/request'
import {ErrorState, ListTable} from '@yuku123/z-frontend-common/ui'

const ServiceList = () => {
    const [loading, setLoading] = useState(false)
    const [data, setData] = useState([])
    // lead 008 §17：接口挂了要常驻报错 + 重试。原来这里回填两条假服务充场面，
    // 会让"服务没注册"和"接口没通"长得一模一样 —— 假数据比空表更危险，已删除。
    const [loadError, setLoadError] = useState(null)

    // 获取服务列表
    // 共享 request 已按标准 {code, data, message} 解包：result 就是数组本身
    // 
    const fetchServiceList = async () => {
        setLoading(true)
        try {
            const result = await request.get('/api/naming/listServices')
            if (Array.isArray(result)) {
                setData(result.map((item, index) => ({key: index + 1, ...item})))
                setLoadError(null)
            } else {
                setLoadError('获取服务列表失败：响应不是数组')
            }
        } catch (error) {
            console.error('获取服务列表错误:', error)
            setLoadError(error)
        } finally {
            setLoading(false)
        }
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
            {loadError && (
                <ErrorState
                    error={loadError}
                    title="服务列表没有读到"
                    description="注册中心接口没有返回数据，下方空表不代表真的没有服务。"
                    onRetry={fetchServiceList}
                    style={{marginBottom: 16}}
                />
            )}
            {/* 整表一次拉回、翻页在前端做 ⇒ 基座 ListTable 统一承接过滤/分页/空态 */}
            <ListTable
                cardTitle="服务列表"
                rows={data}
                columns={columns}
                rowKey="key"
                loading={loading}
                searchKeys="serviceName,group,namespace,status"
                searchPlaceholder="按服务名 / 分组 / 命名空间 / 状态过滤"
                cardExtra={
                    <Button
                        icon={<ReloadOutlined/>}
                        onClick={fetchServiceList}
                        loading={loading}
                    >
                        刷新
                    </Button>
                }
                emptyTitle={loadError ? '数据没有读到' : '暂无服务'}
                emptyDescription={loadError ? '上方错误条已说明原因，点「重新加载」重试。' : '当前没有注册的服务'}
            />
        </div>
    )
}

export default ServiceList
