import {useEffect, useState} from 'react'
import {useLocation, useNavigate, useSearchParams} from 'react-router-dom'
import {Button, Card, Form, Input, message, Space} from 'antd'
import {ArrowLeftOutlined} from '@ant-design/icons'

const ConfigEdit = () => {
    const navigate = useNavigate()
    const [form] = Form.useForm()
    const [loading, setLoading] = useState(false)
    const [isEdit, setIsEdit] = useState(false)
    const [searchParams] = useSearchParams()
    const location = useLocation()

    // 获取 URL 参数中的配置信息
    useEffect(() => {
        const state = location.state
        if (state && state.config) {
            // 编辑模式
            setIsEdit(true)
            form.setFieldsValue({
                dataId: state.config.dataId,
                group: state.config.group,
                content: state.config.content,
                description: state.config.description,
            })
        } else {
            // 新建模式，从 query 参数获取初始值
            const dataId = searchParams.get('dataId')
            const group = searchParams.get('group')
            if (dataId) form.setFieldsValue({dataId})
            if (group) form.setFieldsValue({group})
        }
    }, [location.state, searchParams, form])

    const onFinish = async (values) => {
        setLoading(true)
        try {
            const url = '/api/config/saveConfig'
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(values),
            })

            const data = await response.json()

            if (data.success) {
                message.success(isEdit ? '修改成功' : '保存成功')
                navigate('/config/list')
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
                    <Button icon={<ArrowLeftOutlined/>} onClick={() => navigate('/config/list')}>
                        返回
                    </Button>
                    <span>{isEdit ? '编辑配置' : '新建配置'}</span>
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
                    name="content"
                    label="配置内容"
                    rules={[{required: true, message: '请输入配置内容'}]}
                >
                    <Input.TextArea
                        rows={15}
                        placeholder="请输入配置内容..."
                        style={{fontFamily: 'Monaco, Consolas, monospace'}}
                    />
                </Form.Item>

                <Form.Item
                    name="description"
                    label="描述"
                >
                    <Input.TextArea rows={3} placeholder="请输入配置描述（可选）"/>
                </Form.Item>

                <Form.Item>
                    <Space>
                        <Button type="primary" htmlType="submit" loading={loading}>
                            {isEdit ? '保存修改' : '保存'}
                        </Button>
                        <Button onClick={() => navigate('/config/list')}>取消</Button>
                    </Space>
                </Form.Item>
            </Form>
        </Card>
    )
}

export default ConfigEdit
