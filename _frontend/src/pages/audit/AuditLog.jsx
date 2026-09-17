import {useEffect, useState} from 'react'
import {Button, Card, DatePicker, Descriptions, Modal, Select, Space, Table, Tag, message} from 'antd'
import {EyeOutlined, ReloadOutlined} from '@ant-design/icons'

const ACTION_COLORS = {
    CREATE: 'green',
    UPDATE: 'blue',
    DELETE: 'red',
    ROLLBACK: 'orange',
    IMPORT: 'purple',
    EXPORT: 'cyan',
    CLONE: 'magenta',
}

const AuditLog = () => {
    const [data, setData] = useState([])
    const [loading, setLoading] = useState(false)
    const [pagination, setPagination] = useState({current: 1, pageSize: 20, total: 0})
    const [detailVisible, setDetailVisible] = useState(false)
    const [detailData, setDetailData] = useState(null)
    const [filters, setFilters] = useState({})

    const fetchData = async (page = 1, pageSize = 20) => {
        setLoading(true)
        try {
            const params = new URLSearchParams({current: page, pageSize})
            if (filters.action) params.append('action', filters.action)
            if (filters.srcUser) params.append('srcUser', filters.srcUser)
            if (filters.dataId) params.append('dataId', filters.dataId)

            const res = await fetch(`/api/config/audit/page?${params.toString()}`, {method: 'POST'})
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

    const showDetail = (record) => {
        setDetailData(record)
        setDetailVisible(true)
    }

    const columns = [
        {title: '时间', dataIndex: 'gmtCreate', key: 'gmtCreate', width: 180},
        {
            title: '操作', dataIndex: 'action', key: 'action', width: 100,
            render: v => <Tag color={ACTION_COLORS[v] || 'default'}>{v}</Tag>
        },
        {title: 'Data ID', dataIndex: 'dataId', key: 'dataId', ellipsis: true},
        {title: 'Group', dataIndex: 'groupName', key: 'groupName', width: 120},
        {title: '命名空间', dataIndex: 'namespace', key: 'namespace', width: 120},
        {title: '操作人', dataIndex: 'srcUser', key: 'srcUser', width: 100},
        {title: 'IP', dataIndex: 'srcIp', key: 'srcIp', width: 130},
        {
            title: '结果', dataIndex: 'result', key: 'result', width: 80,
            render: v => <Tag color={v === 'SUCCESS' ? 'green' : 'red'}>{v}</Tag>
        },
        {
            title: '操作', key: 'action', width: 80,
            render: (_, record) => (
                <Button type="link" size="small" icon={<EyeOutlined/>}
                        onClick={() => showDetail(record)}>详情</Button>
            )
        }
    ]

    return (
        <Card title="审计日志" extra={
            <Space>
                <Select placeholder="操作类型" allowClear style={{width: 120}}
                        onChange={v => setFilters(f => ({...f, action: v}))}
                        options={Object.keys(ACTION_COLORS).map(k => ({label: k, value: k}))}/>
                <Select placeholder="操作人" allowClear style={{width: 120}}
                        onChange={v => setFilters(f => ({...f, srcUser: v}))}/>
                <Button icon={<ReloadOutlined/>} onClick={() => fetchData(pagination.current, pagination.pageSize)}>刷新</Button>
            </Space>
        }>
            <Table
                dataSource={data} columns={columns} rowKey="id" loading={loading}
                pagination={{...pagination, showSizeChanger: true, showTotal: t => `共 ${t} 条`,
                    onChange: (page, pageSize) => fetchData(page, pageSize)}}
            />
            <Modal title="审计详情" open={detailVisible} onCancel={() => setDetailVisible(false)}
                   footer={null} width={700}>
                {detailData && (
                    <Descriptions column={2} size="small" bordered>
                        <Descriptions.Item label="Data ID" span={2}>{detailData.dataId}</Descriptions.Item>
                        <Descriptions.Item label="Group">{detailData.groupName}</Descriptions.Item>
                        <Descriptions.Item label="命名空间">{detailData.namespace}</Descriptions.Item>
                        <Descriptions.Item label="操作"><Tag color={ACTION_COLORS[detailData.action]}>{detailData.action}</Tag></Descriptions.Item>
                        <Descriptions.Item label="结果"><Tag color={detailData.result === 'SUCCESS' ? 'green' : 'red'}>{detailData.result}</Tag></Descriptions.Item>
                        <Descriptions.Item label="操作人">{detailData.srcUser}</Descriptions.Item>
                        <Descriptions.Item label="IP">{detailData.srcIp}</Descriptions.Item>
                        <Descriptions.Item label="时间" span={2}>{detailData.gmtCreate}</Descriptions.Item>
                        {detailData.oldContent && (
                            <Descriptions.Item label="变更前内容" span={2}>
                                <pre style={{maxHeight: 200, overflow: 'auto', fontSize: 12, background: '#f5f5f5', padding: 8}}>
                                    {detailData.oldContent}
                                </pre>
                            </Descriptions.Item>
                        )}
                        {detailData.newContent && (
                            <Descriptions.Item label="变更后内容" span={2}>
                                <pre style={{maxHeight: 200, overflow: 'auto', fontSize: 12, background: '#f5f5f5', padding: 8}}>
                                    {detailData.newContent}
                                </pre>
                            </Descriptions.Item>
                        )}
                        {detailData.errorMsg && (
                            <Descriptions.Item label="错误信息" span={2}>
                                <span style={{color: 'red'}}>{detailData.errorMsg}</span>
                            </Descriptions.Item>
                        )}
                    </Descriptions>
                )}
            </Modal>
        </Card>
    )
}

export default AuditLog
