import {useEffect, useState} from 'react'
import {Button, Card, Form, Input, message, Modal, Popconfirm, Space, Table} from 'antd'
import {DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined} from '@ant-design/icons'
import {configApi} from '@/services/api'

const ClusterPage = () => {
    const [loading, setLoading] = useState(false)
    const [data, setData] = useState([])
    const [modalOpen, setModalOpen] = useState(false)
    const [editingRecord, setEditingRecord] = useState(null)
    const [form] = Form.useForm()

    const fetchList = async () => {
        setLoading(true)
        try {
            const list = await configApi.clusterList()
            if (list) setData((list || []).map((item) => ({...item, key: item.id})))
        } catch (e) {
            message.error('获取集群列表失败')
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
            <Card title="命名空间管理"
                  extra={<Space><Button icon={<ReloadOutlined/>} onClick={fetchList} loading={loading}>刷新</Button>
                      <Button type="primary" icon={<PlusOutlined/>} onClick={() => {
                          setEditingRecord(null);
                          form.resetFields();
                          setModalOpen(true)
                      }}>新建</Button></Space>}>
                <Table columns={columns} dataSource={data} loading={loading}
                       pagination={{showTotal: (t) => `共 ${t} 条`}}/>
            </Card>
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
