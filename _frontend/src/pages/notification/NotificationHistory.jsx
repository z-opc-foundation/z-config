import {useEffect, useState} from 'react'
import {Button, Card, Select, Space, Table, Tag, message} from 'antd'
import {ReloadOutlined} from '@ant-design/icons'

const RESULT_COLORS = {
    SUCCESS: 'green',
    FAILED: 'red',
    TIMEOUT: 'orange',
}

const NotificationHistory = () => {
    const [data, setData] = useState([])
    const [loading, setLoading] = useState(false)
    const [pagination, setPagination] = useState({current: 1, pageSize: 20, total: 0})
    const [filters, setFilters] = useState({})

    const fetchData = async (page = 1, pageSize = 20) => {
        setLoading(true)
        try {
            const params = new URLSearchParams({current: page, pageSize})
            if (filters.dataId) params.append('dataId', filters.dataId)
            if (filters.group) params.append('group', filters.group)

            const res = await fetch(`/api/config/push/page?${params.toString()}`, {method: 'POST'})
            const json = await res.json()
            if (json.success && json.data) {
                setData(json.data.records || [])
                setPagination({current: json.data.current || page, pageSize, total: json.data.total || 0})
            }
        } catch (e) {
            message.error('加载失败: ' + e.message)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => { fetchData() }, [])

    const columns = [
        {title: '推送时间', dataIndex: 'pushTime', key: 'pushTime', width: 180},
        {title: 'Data ID', dataIndex: 'dataId', key: 'dataId', ellipsis: true},
        {title: 'Group', dataIndex: 'groupName', key: 'groupName', width: 120},
        {title: '命名空间', dataIndex: 'namespace', key: 'namespace', width: 120},
        {title: '客户端 IP', dataIndex: 'clientIp', key: 'clientIp', width: 140},
        {
            title: '推送类型', dataIndex: 'pushType', key: 'pushType', width: 120,
            render: v => <Tag color="blue">{v}</Tag>
        },
        {
            title: '推送结果', dataIndex: 'pushResult', key: 'pushResult', width: 100,
            render: v => <Tag color={RESULT_COLORS[v] || 'default'}>{v}</Tag>
        },
        {title: '配置 MD5', dataIndex: 'newMd5', key: 'newMd5', width: 100, ellipsis: true},
    ]

    return (
        <Card
            title="推送通知历史"
            extra={
                <Space>
                    <Button icon={<ReloadOutlined/>}
                            onClick={() => fetchData(pagination.current, pagination.pageSize)}>
                        刷新
                    </Button>
                </Space>
            }
        >
            <Table
                dataSource={data}
                columns={columns}
                rowKey="id"
                loading={loading}
                pagination={{
                    ...pagination,
                    showSizeChanger: true,
                    showTotal: t => `共 ${t} 条`,
                    onChange: (page, pageSize) => fetchData(page, pageSize)
                }}
            />
        </Card>
    )
}

export default NotificationHistory
