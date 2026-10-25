import {useEffect, useMemo, useState} from 'react'
import {Button, Card, Checkbox, Form, Input, InputNumber, Modal, Space, Table, Tag, message} from 'antd'
import {DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined} from '@ant-design/icons'
import {DEFAULT_PAGINATION} from '@yuku123/z-frontend-common/ui'

const API_BASE = '/api/namespace'

/**
 * ⚠ **键名口径（2026-10-25 实测，别再改回 camelCase）** ─────────────────────────
 *
 * 宿主（z-opc）全局 Jackson 是 `PropertyNamingStrategy.SNAKE_CASE`
 * （z-team-core/JacksonConfig）⇒ **@RequestBody 的 bean 属性按 snake_case 绑定**，
 * 而 **@RequestParam（query）保持字面量 camelCase**。
 *
 * 实测（生产 opc.zopc.top，2026-10-25，带真实会话）：
 *   POST /api/namespace/create {"namespaceId":"DEFAULT_NAMESPACE"}
 *        → 200 `处理失败|namespaceId 不能为空`      ⛔ **camelCase 没绑上**
 *   POST /api/namespace/create {"namespace_id":"DEFAULT_NAMESPACE"}
 *        → 200 `处理失败|命名空间ID已存在: DEFAULT_NAMESPACE`  ✅ snake_case 绑上了
 *        （该请求在「ID 已存在」处就返回 ⇒ 未写库，是**只读探针**）
 *   POST /api/namespace/delete?namespaceId=... → 400 `缺少参数: namespaceId`
 *        ⇒ **query 键是 camelCase**（`namespaceId`），不是 `namespace_id`
 *
 * ⇒ 结论：**body 用 snake_case、query 用 camelCase**。
 * 原实现两处都错：body 传 camelCase（⇒ 新建/更新永远失败 ⇒ 面板不可编辑），
 * 列的 dataIndex 读 camelCase（⇒ 后端发的是 snake_case ⇒ 列恒空的假绿列）。
 */

/** 表单（camelCase）→ 请求体（snake_case）。 */
const toBody = (values) => ({
    id: values.id,
    namespace_id: values.namespaceId,
    namespace_name: values.namespaceName,
    namespace_desc: values.namespaceDesc,
    max_config_count: values.maxConfigCount,
    enabled: values.enabled,
})

export default function NamespaceList() {
    const [data, setData] = useState([])
    const [loading, setLoading] = useState(false)
    const [modalOpen, setModalOpen] = useState(false)
    const [editingRecord, setEditingRecord] = useState(null)
    const [form] = Form.useForm()

    const fetchData = async () => {
        setLoading(true)
        try {
            const res = await fetch(`${API_BASE}/list`, {headers: {Accept: 'application/json'}})
            const json = await res.json()
            setData(json.data || [])
        } catch (e) {
            message.error('加载失败: ' + e.message)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => { fetchData() }, [])

    // lead 008 §2 规则 3：客户端查询过滤（后端 list 接口不带关键字参数，过滤在端侧做）。
    // 参与匹配的字段取自本页 columns 真实存在的 dataIndex：namespace_id / namespace_name。
    const [keyword, setKeyword] = useState('')
    const filteredData = useMemo(() => {
        const kw = keyword.trim().toLowerCase()
        if (!kw) return data
        return data.filter((r) => `${r.namespace_id || ''} ${r.namespace_name || ''}`.toLowerCase().includes(kw))
    }, [data, keyword])

    const handleCreate = () => {
        setEditingRecord(null)
        form.resetFields()
        form.setFieldsValue({maxConfigCount: 200, enabled: true})
        setModalOpen(true)
    }

    const handleEdit = (record) => {
        setEditingRecord(record)
        // 编辑回填：响应是 snake_case，表单字段是 camelCase ⇒ 这里做一次映射
        form.resetFields()
        form.setFieldsValue({
            id: record.id,
            namespaceId: record.namespace_id,
            namespaceName: record.namespace_name,
            namespaceDesc: record.namespace_desc,
            maxConfigCount: record.max_config_count,
            enabled: record.enabled,
        })
        setModalOpen(true)
    }

    const handleDelete = async (record) => {
        if (record.namespace_id === 'DEFAULT_NAMESPACE') {
            message.warning('不能删除默认命名空间')
            return
        }
        Modal.confirm({
            title: '确认删除',
            content: `确定要删除命名空间 "${record.namespace_name}" (${record.namespace_id}) 吗？`,
            onOk: async () => {
                try {
                    // ⚠ query 键是 camelCase（实测「缺少参数: namespaceId」）
                    const res = await fetch(`${API_BASE}/delete?namespaceId=${encodeURIComponent(record.namespace_id)}`,
                        {method: 'POST', headers: {Accept: 'application/json'}})
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
            // ⚠ body 必须 snake_case（实测 camelCase 绑不上 ⇒ 新建/更新必失败）
            const body = toBody(values)

            const res = await fetch(url, {
                method,
                headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
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
        {title: '命名空间ID', dataIndex: 'namespace_id', key: 'namespace_id', render: v => <code>{v}</code>},
        {title: '名称', dataIndex: 'namespace_name', key: 'namespace_name'},
        {title: '描述', dataIndex: 'namespace_desc', key: 'namespace_desc', ellipsis: true},
        {
            title: '配置数量', key: 'config_count',
            render: (_, r) => <span>{r.config_count ?? 0} / {r.max_config_count ?? 200}</span>
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
                    {record.namespace_id !== 'DEFAULT_NAMESPACE' && (
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
                    <Input.Search allowClear placeholder="按命名空间 ID / 名称过滤" style={{width: 240}}
                                  onChange={(e) => setKeyword(e.target.value)}/>
                    <Button icon={<ReloadOutlined/>} onClick={fetchData}>刷新</Button>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>新建命名空间</Button>
                </Space>
            }
        >
            <Table
                dataSource={filteredData}
                columns={columns}
                rowKey="id"
                loading={loading}
                pagination={DEFAULT_PAGINATION}
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
                        <Checkbox>启用</Checkbox>
                    </Form.Item>
                </Form>
            </Modal>
        </Card>
    )
}
