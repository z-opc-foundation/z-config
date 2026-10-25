import {useEffect, useState} from 'react'
import {Button, Input, message, Popconfirm, Select, Space, Tag, Upload} from 'antd'
import {DeleteOutlined, DownloadOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, UploadOutlined} from '@ant-design/icons'
import {useNavigate} from 'react-router-dom'
import axios from 'axios'
import {ListTable} from '@yuku123/z-frontend-common/ui'

/**
 * 配置列表页（component 路由 /z-config/config/list 实际挂载的就是本文件）。
 *
 * ⚠️ 三条契约都是 2026-10-22 实测出来的（证据见 lead TASK-20260925-036），
 *    改之前先看那三条，别"顺手改回去"：
 *
 * ① 请求体键名：**@RequestBody 的 bean 属性被全局 Jackson SNAKE_CASE 改名**
 *    （z-team 的 JacksonConfig 把 ObjectMapper 设成 PropertyNamingStrategy.SNAKE_CASE，
 *      整个合并 JVM 一份）。所以多词键发 camelCase = **静默失效**（不过滤 = 命中全表），
 *      发 snake_case 才生效。实测：{"nameSpace":"no_such_ns"} ⇒ 8 行（键被丢弃），
 *      {"name_space":"no_such_ns"} ⇒ 0 行（键生效）。
 *    ⚠️ 但 **query 参数不受影响**（@RequestParam 名就是字面量）：实测
 *      GET /api/schedule/jobgroup/registryNodes?app_name=x ⇒ 400「缺少参数: appName」，
 *      ?appName=x ⇒ 200。所以 query 一律保持 camelCase。
 *
 * ② 分页参数只认 current/size：ZConfigPageRequest extends PageRequest。
 *    实测 {"current":2,"size":3} ⇒ 3 行且首条换人；{"pageNum":1,"pageSize":3} ⇒ 整表 8 行。
 *
 * ③ 响应体键名是 snake_case：实测 records[0] 的键 =
 *    id/data_id/group/content/md5/gmt_create/gmt_modified/creator_staff_no/
 *    creator_staff_nick_mn/creator_staff_real_nm/source_ip/app_name/namespace/
 *    config_desc/config_usage/config_enable_rule/config_type/config_schema/encrypted_data_key
 *    ⇒ 表格 dataIndex 必须用 snake_case，否则列是**恒空的假绿列**。
 *    （注意 creator_staff_nick_**mn** —— 后端就是这个拼写，别"顺手修正"。）
 *
 * ⚠️ 响应里**没有 status 字段** ⇒ 原来的「状态」列恒显示"停用"，
 *    对一条正在生效的配置也这么说 ⇒ 是会误导人的假列，本轮已删除。
 */

const ConfigList = () => {
    const navigate = useNavigate()
    const [loading, setLoading] = useState(false)
    const [data, setData] = useState([])
    const [pagination, setPagination] = useState({
        current: 1,
        pageSize: 10,
        total: 0,
    })
    const [searchText, setSearchText] = useState('')
    const [selectedNamespace, setSelectedNamespace] = useState(undefined)
    const [selectedGroup, setSelectedGroup] = useState(undefined)
    const [namespaceList, setNamespaceList] = useState([])
    const [groupList, setGroupList] = useState([])

    // 获取命名空间列表
    // ⚠️ 原来这里打的是 /api/cluster/list —— 两个问题：
    //   ① 集群 ≠ 命名空间（后端 ZConfigPageRequest 里只有 nameSpace 字段）；
    //   ② /api/cluster/list 实测 200 但**返回空数组**（z_cluster 空表）⇒ 下拉恒空。
    // 实测真正的命名空间数据源是 GET /api/config/namespaceList ⇒ ["public"]（字符串数组）。
    const fetchNamespaceList = async () => {
        try {
            const res = await axios.get('/api/config/namespaceList')
            const list = Array.isArray(res.data) ? res.data : []
            setNamespaceList(list.map((item) => ({label: item, value: item})))
        } catch (e) {
            console.error('获取命名空间失败', e)
        }
    }

    // 获取Group列表
    const fetchGroupList = async () => {
        try {
            const res = await axios.get('/api/config/groupList')
            setGroupList(res.data.map(item => ({label: item, value: item})))
        } catch (e) {
            console.error('获取Group失败', e)
        }
    }

    // 获取配置列表
    const fetchConfigList = async (page = 1, pageSize = 10) => {
        setLoading(true)
        try {
            const result = await axios.post('/api/config/pageConfig', {
                // ② 分页只认 current/size（pageNum/pageSize 被忽略 ⇒ 整表进浏览器）
                current: page,
                size: pageSize,
                search: searchText || undefined,
                // ① 请求体 bean 属性被全局 SNAKE_CASE 改名 ⇒ 多词键必须 snake_case，
                //    发 nameSpace 的话这个筛选会被整个丢掉（静默回到全表）。
                name_space: selectedNamespace,
                group: selectedGroup,
            })

            if (result.data.success && result.data.data) {
                // 适配后端返回的字段名: records, total, current
                const records = result.data.data.records || []
                const total = result.data.data.total || 0
                const current = result.data.data.current || 1

                setData(records.map((item, index) => ({
                    key: item.id || index,
                    ...item,
                })))
                setPagination({
                    current: current,
                    pageSize: pageSize,
                    total: total,
                })
            } else {
                message.error(result.data.message || '获取配置列表失败')
            }
        } catch (error) {
            console.error('获取配置列表错误:', error)
            // 别把真因压成"网络错误"：后端把原因放在 response.data.message 里
            // （实测恒 500 时就是 "Unknown column 'encrypted_data_key' in 'field list'"）。
            message.error(error?.response?.data?.message || error?.message || '获取配置列表失败')
        } finally {
            setLoading(false)
        }
    }

    // 删除配置
    // ⚠️ handler 是 deleteConfig(@RequestBody ZConfigQueryRequest)：
    //   ① 键名同样被 SNAKE_CASE 改名 ⇒ name_space / group / data_id；
    //   ② record.data_id 是**响应体的键**（不是 dataId —— 响应是 snake_case），
    //      原来读 record.dataId 恒 undefined ⇒ 删的是"找不到的配置"。
    // ⚠️ 写路径未在共享库实测（不拿生产真配置做删除），依据是 handler 签名。
    const handleDelete = async (record) => {
        try {
            const result = await axios.post('/api/config/delete', {
                name_space: record.namespace,
                group: record.group,
                data_id: record.data_id,
            })

            if (result.data.success) {
                message.success('删除成功')
                fetchConfigList(pagination.current, pagination.pageSize)
            } else {
                message.error(result.data.message || '删除失败')
            }
        } catch (error) {
            console.error('删除错误:', error)
            message.error(error?.response?.data?.message || error?.message || '删除失败')
        }
    }

    // 处理编辑
    const handleEdit = (record) => {
        navigate('/z-config/config/edit', {
            state: {config: record},
        })
    }

    // 处理表格分页变化
    const handleTableChange = (newPagination) => {
        fetchConfigList(newPagination.current, newPagination.pageSize)
    }

    // 处理搜索
    const handleSearch = () => {
        fetchConfigList(1, pagination.pageSize)
    }

    // 导出配置
    // ⚠️ 这里是 **query** 参数（不是 body）：@RequestParam 的名字就是字面量，**不受** SNAKE_CASE 影响
    //    （后端 exportConfigs 的签名就是 @RequestParam String nameSpace）⇒ 这里必须写 camelCase。
    const handleExport = async () => {
        try {
            const params = new URLSearchParams()
            if (selectedNamespace) params.append('nameSpace', selectedNamespace)
            if (selectedGroup) params.append('group', selectedGroup)
            const res = await fetch(`/api/config/export?${params.toString()}`, {method: 'POST'})
            const json = await res.json()
            if (json.success && json.data) {
                const blob = new Blob([JSON.stringify(json.data, null, 2)], {type: 'application/json'})
                const url = URL.createObjectURL(blob)
                const a = document.createElement('a')
                a.href = url
                a.download = `z-config-export-${Date.now()}.json`
                a.click()
                URL.revokeObjectURL(url)
                message.success('导出成功，共 ' + json.data.length + ' 条配置')
            } else {
                message.error(json.message || '导出失败')
            }
        } catch (e) {
            message.error('导出失败: ' + e.message)
        }
    }

    // 导入配置
    const handleImport = async (file) => {
        try {
            const text = await file.text()
            const configs = JSON.parse(text)
            if (!Array.isArray(configs)) {
                message.error('导入文件格式错误：应为 JSON 数组')
                return false
            }
            const res = await fetch('/api/config/import', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(configs)
            })
            const json = await res.json()
            if (json.success) {
                message.success(json.data)
                fetchConfigList(1, pagination.pageSize)
            } else {
                message.error(json.message || '导入失败')
            }
        } catch (e) {
            message.error('导入失败: ' + e.message)
        }
        return false // 阻止 Upload 组件自动上传
    }

    // 初始加载
    useEffect(() => {
        fetchConfigList()
        fetchNamespaceList()
        fetchGroupList()
    }, [])

    // ③ 表格列的 dataIndex 一律用**实测响应键**（snake_case）。
    //    改之前这里是 dataId/appName/configType/status —— 响应里根本没有这几个 camelCase 键，
    //    ⇒ 4 列恒空（假绿列）；status 连字段都不存在（z_config_info 无 status 列），
    //    恒渲染成"停用"，对正在生效的配置也这么说 ⇒ 会误导人，本轮删除该列。
    const columns = [
        {
            title: 'Data ID',
            dataIndex: 'data_id',
            key: 'data_id',
            ellipsis: true,
        },
        {
            title: 'Group',
            dataIndex: 'group',
            key: 'group',
        },
        {
            title: '应用名',
            dataIndex: 'app_name',
            key: 'app_name',
        },
        {
            title: '类型',
            dataIndex: 'config_type',
            key: 'config_type',
            render: (type) => (
                <Tag color="blue">{type || 'TEXT'}</Tag>
            ),
        },
        {
            title: '操作',
            key: 'action',
            width: 150,
            render: (_, record) => (
                <Space>
                    <Button
                        type="link"
                        size="small"
                        icon={<EditOutlined/>}
                        onClick={() => handleEdit(record)}
                    >
                        编辑
                    </Button>
                    <Popconfirm
                        title="确认删除"
                        description={`确定要删除配置 "${record.data_id}" 吗？`}
                        onConfirm={() => handleDelete(record)}
                        okText="确定"
                        cancelText="取消"
                    >
                        <Button
                            type="link"
                            danger
                            size="small"
                            icon={<DeleteOutlined/>}
                        >
                            删除
                        </Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ]

    return (
        <div>
            <div style={{marginBottom: 16}}>
                <Space size="middle">
                    <Select
                        placeholder="选择命名空间"
                        style={{width: 200}}
                        value={selectedNamespace}
                        onChange={(value) => setSelectedNamespace(value)}
                        allowClear
                        options={namespaceList}
                    />
                    <Select
                        placeholder="选择Group"
                        style={{width: 200}}
                        value={selectedGroup}
                        onChange={(value) => setSelectedGroup(value)}
                        allowClear
                        options={groupList}
                    />
                    <Input.Search
                        placeholder="搜索 Data ID..."
                        value={searchText}
                        onChange={(e) => setSearchText(e.target.value)}
                        onSearch={handleSearch}
                        style={{width: 300}}
                        prefix={<SearchOutlined/>}
                        allowClear
                    />
                    <Button
                        type="primary"
                        onClick={handleSearch}
                    >
                        搜索
                    </Button>
                </Space>
            </div>
            {/* 服务端分页，不套客户端分页：
                - 分页对象保留原样（current/pageSize/total 由后端驱动，翻页重查接口），
                  只补 §2 规则 2 要求的 pageSizeOptions；
                - 过滤走后端（search 参数），不传 searchKeys，否则页面上会出现两个搜索框；
                - 空态由基座 <EmptyState/> 承接。 */}
            <ListTable
                cardTitle="配置列表"
                rows={data}
                columns={columns}
                rowKey="key"
                loading={loading}
                pagination={{
                    ...pagination,
                    showSizeChanger: true,
                    pageSizeOptions: ['10', '20', '50', '100'],
                    showTotal: (total) => `共 ${total} 条`,
                }}
                onChange={handleTableChange}
                cardExtra={
                    <Space>
                        <Upload
                            accept=".json"
                            showUploadList={false}
                            beforeUpload={handleImport}
                        >
                            <Button icon={<UploadOutlined/>}>导入</Button>
                        </Upload>
                        <Button icon={<DownloadOutlined/>} onClick={handleExport}>导出</Button>
                        <Button
                            icon={<ReloadOutlined/>}
                            onClick={() => fetchConfigList(pagination.current, pagination.pageSize)}
                            loading={loading}
                        >
                            刷新
                        </Button>
                        <Button
                            type="primary"
                            icon={<PlusOutlined/>}
                            onClick={() => navigate('/z-config/config/edit')}
                        >
                            新建配置
                        </Button>
                    </Space>
                }
                emptyTitle="暂无配置"
                emptyDescription="该命名空间下还没有任何配置项，用「新建配置」加第一条。"
            />
        </div>
    )
}

export default ConfigList
