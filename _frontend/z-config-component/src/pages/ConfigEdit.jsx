import {useEffect, useState} from 'react'
import {useLocation, useNavigate, useSearchParams} from 'react-router-dom'
import {Button, Card, Form, Input, message, Select, Space} from 'antd'
import {configApi} from '@/services/api'
import {PageHeader} from '@yuku123/z-frontend-common/ui'

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
                // 实测数据源是 /api/config/namespaceList ⇒ ["public"]（字符串数组）；
                // 原来打的 /api/cluster/list 200 但恒空（z_cluster 空表）⇒ 下拉没得选。
                const nsList = await configApi.namespaceList()
                setNamespaceList((nsList || []).map(n => ({label: n, value: n})))
            } catch (e) {
                console.error(e)
            }
        })()
    }, [])

    useEffect(() => {
        const state = location.state
        if (state && state.config) {
            setIsEdit(true)
            // 列表页传来的 record 是响应体原样键（snake_case，2026-11-13 实测），
            // 原来读 dataId/appName/configDesc 恒 undefined ⇒ 编辑页打开是全空表单。
            form.setFieldsValue({
                dataId: state.config.data_id, group: state.config.group,
                content: state.config.content, appName: state.config.app_name,
                namespace: state.config.namespace, configDesc: state.config.config_desc,
                configType: state.config.config_type || 'TEXT',
            })
        } else {
            const dataId = searchParams.get('dataId'), group = searchParams.get('group')
            const namespace = searchParams.get('namespace')
            if (dataId) form.setFieldsValue({dataId})
            if (group) form.setFieldsValue({group})
            form.setFieldsValue({namespace: namespace || 'public'})
        }
    }, [])

    const onFinish = async (values) => {
        setLoading(true)
        try {
            // mode="tags" 返回数组，转字符串。
            // 保存请求体走 @RequestBody(ZConfigSaveRequest)，bean 被全局 SNAKE_CASE 改名 ⇒
            // 多词键必须 snake_case（发 dataId 会被静默丢掉 ⇒ 存出来 data_id 为空）。
            // 表单内部字段名保持 camelCase，只有出站载荷做一次映射。
            const payload = {
                namespace: Array.isArray(values.namespace) ? values.namespace[0] : values.namespace,
                data_id: values.dataId,
                group: values.group,
                content: values.content,
                app_name: values.appName,
                config_type: values.configType,
                config_desc: values.configDesc,
            }
            await configApi.saveConfig(payload)
            message.success(isEdit ? '修改成功' : '保存成功')
            navigate('/config/list')
        } catch (error) {
            message.error(isEdit ? '修改失败' : (error?.message || '保存失败'))
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
