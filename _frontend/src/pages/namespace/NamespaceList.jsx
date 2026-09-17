import {useEffect, useState} from 'react'
import {Button, Card, Form, Input, InputNumber, Modal, Space, Table, Tag, message} from 'antd'
import {DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined} from '@ant-design/icons'

const API_BASE = '/api/namespace'

export default function NamespaceList() {
    const [data, setData] = useState([])
    const [loading, setLoading] = useState(false)
    const [modalOpen, setModalOpen] = useState(false)
    const [editingRecord, setEditingRecord] = useState(null)
    const [form] = Form.useForm()

    const fetchData = async () => {
        setLoading(true)
        try {
            const res = await fetch(`${API_BASE}/list`)
            const json = await res.json()
            setData(json.data || [])
        } catch (e) {
            message.error('加载失败: ' + e.message)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => { fetchData() }, [])

    const handleCreate = () => {
        setEditingRecord(null)
        form.resetFields()
        form.setFieldsValue({maxConfigCount: 200, enabled: true})
        setModalOpen(true)
    }

    const handleEdit = (record) => {
        setEditingRecord(record)
        form.setFieldsValue(record)
        setModalOpen(true)
    }

    const handleDelete = async (record) => {
        if (record.namespaceId === 'DEFAULT_NAMESPACE') {
            message.warning('不能删除默认命名空间')
            return
        }
        Modal.confirm({
            title: '确认删除',
            content: `确定要删除命名空间 "${record.namespaceName}" (${record.namespaceId}) 吗？`,
            onOk: async () => {
                try {
                    const res = await fetch(`${API_BASE}/delete?namespaceId=${record.namespaceId}`, {method: 'POST'})
                    const json = await res.json()
                    if (json.success) {
                        message.success('删除成功')
                        fetchData()
                    } else {
                        message.error(json.message || '删除失败')
                    }
                } catch (e) {
                    message.error('删除失败: ' + e.message)
                }
            }
        })
    }

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields()
            const url = editingRecord ? `${API_BASE}/update` : `${API_BASE}/create`
            const method = 'POST'
            const body = editingRecord ? {...values, id: editingRecord.id} : values

            const res = await fetch(url, {
                method,
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(body)
            })
            const json = await res.json()
            if (json.success) {
                message.success(editingRecord ? '更新成功' : '创建成功')
                setModalOpen(false)
                fetchData()
            } else {
                message.error(json.message || '操作失败')
            }
        } catch (e) {
            if (e.errorFields) return // 表单校验失败
            message.error('操作失败: ' + e.message)
        }
    }

    const columns = [
        {title: '命名空间ID', dataIndex: 'namespaceId', key: 'namespaceId', render: v => <code>{v}</code>},
        {title: '名称', dataIndex: 'namespaceName', key: 'namespaceName'},
        {title: '描述', dataIndex: 'namespaceDesc', key: 'namespaceDesc', ellipsis: true},
        {
            title: '配置数量', key: 'configCount',
            render: (_, r) => <span>{r.configCount || 0} / {r.maxConfigCount || 200}</span>
        },
        {
            title: '状态', dataIndex: 'enabled', key: 'enabled',
            render: v => <Tag color={v ? 'green' : 'red'}>{v ? '启用' : '禁用'}</Tag>
        },
        {
            title: '操作', key: 'action',
            render: (_, record) => (
                <Space>
                    <Button type="link" icon={<EditOutlined/>} onClick={() => handleEdit(record)}>编辑</Button>
                    {record.namespaceId !== 'DEFAULT_NAMESPACE' && (
                        <Button type="link" danger icon={<DeleteOutlined/>}
                                onClick={() => handleDelete(record)}>删除</Button>
                    )}
                </Space>
            )
        }
    ]

    return (
        <Card
            title="命名空间管理"
            extra={
                <Space>
                    <Button icon={<ReloadOutlined/>} onClick={fetchData}>刷新</Button>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>新建命名空间</Button>
                </Space>
            }
        >
            <Table
                dataSource={data}
                columns={columns}
                rowKey="id"
                loading={loading}
                pagination={false}
            />
            <Modal
                title={editingRecord ? '编辑命名空间' : '新建命名空间'}
                open={modalOpen}
                onOk={handleSubmit}
                onCancel={() => setModalOpen(false)}
                width={520}
            >
                <Form form={form} layout="vertical">
                    <Form.Item name="namespaceId" label="命名空间ID"
                               rules={[{required: true, message: '请输入命名空间ID'}]}>
                        <Input placeholder="如 dev/test/prod" disabled={!!editingRecord}/>
                    </Form.Item>
                    <Form.Item name="namespaceName" label="名称"
                               rules={[{required: true, message: '请输入名称'}]}>
                        <Input placeholder="如 开发环境"/>
                    </Form.Item>
                    <Form.Item name="namespaceDesc" label="描述">
                        <Input.TextArea rows={2} placeholder="命名空间描述"/>
                    </Form.Item>
                    <Form.Item name="maxConfigCount" label="最大配置数">
                        <InputNumber min={1} max={100000} style={{width: '100%'}}/>
                    </Form.Item>
                    <Form.Item name="enabled" label="状态" valuePropName="checked">
                        <input type="checkbox"/> 启用
                    </Form.Item>
                </Form>
            </Modal>
        </Card>
    )
}
