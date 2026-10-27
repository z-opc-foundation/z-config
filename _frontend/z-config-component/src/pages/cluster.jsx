import {useEffect, useState} from 'react'
import {Button, Form, Input, message, Modal, Popconfirm, Space} from 'antd'
import {DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined} from '@ant-design/icons'
import {configApi} from '@/services/api'
import {ErrorState, ListTable} from '@yuku123/z-frontend-common/ui'

const ClusterPage = () => {
    const [loading, setLoading] = useState(false)
    const [data, setData] = useState([])
    const [modalOpen, setModalOpen] = useState(false)
    const [editingRecord, setEditingRecord] = useState(null)
    const [form] = Form.useForm()
    // lead 008 §17：读失败常驻报错 + 重试，避免空表被误读成"没有集群"。
    const [loadError, setLoadError] = useState(null)

    const fetchList = async () => {
        setLoading(true)
        try {
            const list = await configApi.clusterList()
            if (list) setData((list || []).map((item) => ({...item, key: item.id})))
            setLoadError(null)
        } catch (e) {
            setLoadError(e)
        } finally {
            setLoading(false)
        }
    }

    const handleSave = async (values) => {
        try {
            await configApi.clusterSave({...values, id: editingRecord?.id})
            message.success(editingRecord ? '更新成功' : '创建成功')
            setModalOpen(false);
            setEditingRecord(null);
            form.resetFields();
            fetchList()
        } catch (e) {
            message.error('操作失败')
        }
    }

    const handleDelete = async (record) => {
        try {
            await configApi.clusterDelete(record.id)
            message.success('删除成功');
            fetchList()
        } catch (e) {
            message.error('删除失败')
        }
    }

    useEffect(() => {
        fetchList()
    }, [])

    const columns = [
        {title: 'ID', dataIndex: 'id', key: 'id', width: 80},
        {title: '命名空间名称', dataIndex: 'name', key: 'name'},
        {title: '健康检查URL', dataIndex: 'healthCheckUrl', key: 'healthCheckUrl', ellipsis: true},
        {
            title: '操作', key: 'action', width: 150,
            render: (_, record) => (
                <Space>
                    <Button type="link" size="small" icon={<EditOutlined/>}
                            onClick={() => {
                                setEditingRecord(record);
                                form.setFieldsValue(record);
                                setModalOpen(true)
                            }}>编辑</Button>
                    <Popconfirm title="确认删除" onConfirm={() => handleDelete(record)} okText="确定" cancelText="取消">
                        <Button type="link" danger size="small" icon={<DeleteOutlined/>}>删除</Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ]

    return (
        <div>
            {loadError && (
                <ErrorState
                    error={loadError}
                    title="集群列表没有读到"
                    description="接口没有返回数据，下方空表不代表真的没有集群。"
                    onRetry={fetchList}
                    style={{marginBottom: 16}}
                />
            )}
            {/* 客户端整表：过滤/分页/空态三件套交给基座 ListTable，动作按钮放 cardExtra */}
            <ListTable
                cardTitle="命名空间管理"
                rows={data}
                columns={columns}
                rowKey="key"
                loading={loading}
                searchKeys="name,healthCheckUrl"
                searchPlaceholder="按命名空间名称 / 健康检查 URL 过滤"
                cardExtra={<Space><Button icon={<ReloadOutlined/>} onClick={fetchList} loading={loading}>刷新</Button>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={() => {
                        setEditingRecord(null);
                        form.resetFields();
                        setModalOpen(true)
                    }}>新建</Button></Space>}
                emptyTitle={loadError ? '数据没有读到' : '暂无集群'}
                emptyDescription={loadError ? '上方错误条已说明原因，点「重新加载」重试。' : '还没有登记集群，点「新建」加第一个。'}
            />
            <Modal title={editingRecord ? '编辑命名空间' : '新建命名空间'}
                   open={modalOpen} onCancel={() => {
                setModalOpen(false);
                setEditingRecord(null)
            }}
                   onOk={() => form.submit()} destroyOnClose>
                <Form form={form} layout="vertical" onFinish={handleSave}>
                    <Form.Item name="name" label="名称" rules={[{required: true}]}>
                        <Input placeholder="如：DEFAULT_NAMESPACE"/></Form.Item>
                    <Form.Item name="healthCheckUrl" label="健康检查URL">
                        <Input placeholder="如：http://127.0.0.1:8080/health"/></Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

export default ClusterPage
