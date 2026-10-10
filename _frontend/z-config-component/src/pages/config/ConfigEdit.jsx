import {useEffect, useState} from 'react'
import {useLocation, useNavigate, useSearchParams} from 'react-router-dom'
import {Button, Card, Form, Input, InputNumber, message, Select, Space, Tag} from 'antd'
import {ArrowLeftOutlined, LockOutlined} from '@ant-design/icons'

const CONFIG_TYPES = [
    {label: 'TEXT', value: 'text'},
    {label: 'JSON', value: 'json'},
    {label: 'YAML', value: 'yaml'},
    {label: 'Properties', value: 'properties'},
    {label: 'XML', value: 'xml'},
]

const MAX_CONTENT_SIZE = 100 * 1024 // 100KB，对齐 Nacos 默认限制

const ConfigEdit = () => {
    const navigate = useNavigate()
    const [form] = Form.useForm()
    const [loading, setLoading] = useState(false)
    const [isEdit, setIsEdit] = useState(false)
    const [searchParams] = useSearchParams()
    const location = useLocation()
    const [namespaceList, setNamespaceList] = useState([])
    const [contentSize, setContentSize] = useState(0)

    // 获取命名空间列表
    useEffect(() => {
        fetch('/api/config/namespaceList')
            .then(r => r.json())
            .then(json => {
                if (json.success && json.data) {
                    setNamespaceList(json.data.map(ns => ({label: ns, value: ns})))
                }
            })
            .catch(() => {})
    }, [])

    // 获取 URL 参数中的配置信息
    useEffect(() => {
        const state = location.state
        if (state && state.config) {
            setIsEdit(true)
            const config = state.config
            form.setFieldsValue({
                dataId: config.dataId,
                group: config.group,
                namespace: config.namespace,
                content: config.content,
                configType: config.configType || 'text',
                configDesc: config.configDesc,
            })
            if (config.content) {
                setContentSize(new Blob([config.content]).size)
            }
        } else {
            const dataId = searchParams.get('dataId')
            const group = searchParams.get('group')
            if (dataId) form.setFieldsValue({dataId})
            if (group) form.setFieldsValue({group})
            form.setFieldsValue({configType: 'text', group: 'DEFAULT_GROUP'})
        }
    }, [location.state, searchParams, form])

    // 内容大小变化检测
    const handleContentChange = (e) => {
        const content = e.target.value || ''
        setContentSize(new Blob([content]).size)
    }

    const onFinish = async (values) => {
        // 内容大小校验（对齐 Nacos 100KB 限制）
        if (values.content && new Blob([values.content]).size > MAX_CONTENT_SIZE) {
            message.error(`配置内容超出大小限制（最大 ${MAX_CONTENT_SIZE / 1024}KB）`)
            return
        }
        setLoading(true)
        try {
            const url = '/api/config/saveConfig'
            const response = await fetch(url, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(values),
            })
            const data = await response.json()
            if (data.success) {
                message.success(isEdit ? '修改成功' : '保存成功')
                navigate('/z-config/config/list')
            } else {
                message.error(data.message || (isEdit ? '修改失败' : '保存失败'))
            }
        } catch (error) {
            console.error('保存错误:', error)
            message.error('网络错误')
        } finally {
            setLoading(false)
        }
    }

    return (
        <Card
            title={
                <Space>
                    <Button icon={<ArrowLeftOutlined/>} onClick={() => navigate('/z-config/config/list')}>
                        返回
                    </Button>
                    <span>{isEdit ? '编辑配置' : '新建配置'}</span>
                    {isEdit && (
                        <Tag icon={<LockOutlined/>} color="warning">
                            加密配置请使用 cipher- 前缀的 Data ID
                        </Tag>
                    )}
                </Space>
            }
        >
            <Form
                form={form}
                layout="vertical"
                onFinish={onFinish}
                style={{maxWidth: 800}}
            >
                <Form.Item
                    name="namespace"
                    label="命名空间"
                >
                    <Select
                        placeholder="选择命名空间"
                        options={namespaceList}
                        allowClear
                    />
                </Form.Item>

                <Form.Item
                    name="dataId"
                    label="Data ID"
                    rules={[{required: true, message: '请输入 Data ID'}]}
                >
                    <Input disabled={isEdit} placeholder="请输入 Data ID，如：application.yml"/>
                </Form.Item>

                <Form.Item
                    name="group"
                    label="Group"
                    rules={[{required: true, message: '请输入 Group'}]}
                    initialValue="DEFAULT_GROUP"
                >
                    <Input disabled={isEdit} placeholder="请输入 Group"/>
                </Form.Item>

                <Form.Item
                    name="configType"
                    label="配置类型"
                >
                    <Select options={CONFIG_TYPES} placeholder="选择配置类型"/>
                </Form.Item>

                <Form.Item
                    name="content"
                    label={
                        <span>
                            配置内容
                            <span style={{marginLeft: 8, fontSize: 12, color: contentSize > MAX_CONTENT_SIZE ? '#ff4d4f' : '#999'}}>
                                ({(contentSize / 1024).toFixed(1)}KB / {MAX_CONTENT_SIZE / 1024}KB)
                            </span>
                        </span>
                    }
                    rules={[{required: true, message: '请输入配置内容'}]}
                >
                    <Input.TextArea
                        rows={15}
                        placeholder="请输入配置内容..."
                        style={{fontFamily: 'Monaco, Consolas, monospace'}}
                        onChange={handleContentChange}
                    />
                </Form.Item>

                <Form.Item
                    name="configDesc"
                    label="描述"
                >
                    <Input.TextArea rows={3} placeholder="请输入配置描述（可选）"/>
                </Form.Item>

                <Form.Item>
                    <Space>
                        <Button type="primary" htmlType="submit" loading={loading}>
                            {isEdit ? '保存修改' : '保存'}
                        </Button>
                        <Button onClick={() => navigate('/z-config/config/list')}>取消</Button>
                    </Space>
                </Form.Item>
            </Form>
        </Card>
    )
}

export default ConfigEdit
