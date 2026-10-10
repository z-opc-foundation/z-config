import {useEffect, useState} from 'react'
import {useLocation, useNavigate, useSearchParams} from 'react-router-dom'
import {Button, Card, Form, Input, message, Select, Space} from 'antd'
import {configApi} from '@/config/services/api'
import {PageHeader} from '@/common/components/ui'

const ConfigEdit = () => {
    const navigate = useNavigate()
    const [form] = Form.useForm()
    const [loading, setLoading] = useState(false)
    const [isEdit, setIsEdit] = useState(false)
    const [searchParams] = useSearchParams()
    const location = useLocation()
    const [namespaceList, setNamespaceList] = useState([])
    const [configType, setConfigType] = useState('TEXT')

    useEffect(() => {
        (async () => {
            try {
                const records = await configApi.clusterList()
                setNamespaceList((records || []).map(i => ({label: i.name, value: i.name})))
            } catch (e) {
                console.error(e)
            }
        })()
    }, [])

    useEffect(() => {
        const state = location.state
        if (state && state.config) {
            setIsEdit(true)
            form.setFieldsValue({
                dataId: state.config.dataId, group: state.config.group,
                content: state.config.content, appName: state.config.appName,
                namespace: state.config.namespace, configDesc: state.config.configDesc,
            })
        } else {
            const dataId = searchParams.get('dataId'), group = searchParams.get('group')
            const namespace = searchParams.get('namespace')
            if (dataId) form.setFieldsValue({dataId})
            if (group) form.setFieldsValue({group})
            form.setFieldsValue({namespace: namespace || 'DEFAULT_NAMESPACE'})
        }
    }, [])

    const onFinish = async (values) => {
        setLoading(true)
        try {
            // mode="tags" 返回数组，转字符串
            const payload = {
                ...values,
                namespace: Array.isArray(values.namespace) ? values.namespace[0] : values.namespace
            }
            await configApi.saveConfig(payload)
            message.success(isEdit ? '修改成功' : '保存成功')
            navigate('/config/list')
        } catch (error) {
            message.error(isEdit ? '修改失败' : '保存失败')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div>
            <PageHeader title={isEdit ? '编辑配置' : '新建配置'} back={() => navigate('/config/list')}/>
            <Card>
                <Form form={form} layout="vertical" onFinish={onFinish} style={{maxWidth: 800}}>
                    <Form.Item name="namespace" label="命名空间" rules={[{required: true}]}>
                        <Select mode="tags" maxCount={1} placeholder="选择或输入命名空间" options={namespaceList}/>
                    </Form.Item>
                    <Form.Item name="appName" label="应用名"><Input placeholder="请输入应用名（可选）"/></Form.Item>
                    <Form.Item name="dataId" label="Data ID" rules={[{required: true}]}>
                        <Input disabled={isEdit} placeholder="如：application.yml"/></Form.Item>
                    <Form.Item name="group" label="Group" rules={[{required: true}]} initialValue="DEFAULT_GROUP">
                        <Input disabled={isEdit} placeholder="请输入 Group"/></Form.Item>
                    <Space style={{marginBottom: 8}}>
                        <Form.Item name="configType" label="格式" initialValue="TEXT" noStyle>
                            <Select style={{width: 120}} onChange={(v) => setConfigType(v)}>
                                <Select.Option value="TEXT">纯文本</Select.Option>
                                <Select.Option value="PROPERTIES">Properties</Select.Option>
                                <Select.Option value="YAML">YAML</Select.Option>
                                <Select.Option value="JSON">JSON</Select.Option>
                            </Select>
                        </Form.Item>
                    </Space>
                    <Form.Item name="content" label="配置内容" rules={[{required: true}]}>
                        <Input.TextArea rows={15} style={{fontFamily: 'monospace', fontSize: 13}}
                                        placeholder={(() => {
                                            const t = form.getFieldValue('configType') || 'TEXT'
                                            if (t === 'PROPERTIES') return 'key1=value1\nkey2=value2'
                                            if (t === 'YAML') return 'key1:\n  subkey: value'
                                            if (t === 'JSON') return '{\n  \"key\": \"value\"\n}'
                                            return '请输入配置内容'
                                        })()}/>
                    </Form.Item>
                    <Form.Item name="configDesc" label="描述"><Input.TextArea rows={3}/></Form.Item>
                    <Form.Item><Space>
                        <Button type="primary" htmlType="submit"
                                loading={loading}>{isEdit ? '保存修改' : '保存'}</Button>
                        <Button onClick={() => navigate('/config/list')}>取消</Button>
                    </Space></Form.Item>
                </Form>
            </Card>
        </div>
    )
}

export default ConfigEdit
