import {useEffect, useState} from 'react'
import {Button, Card, Drawer, message, Modal, Popconfirm, Select, Space, Table, Tabs, Tag, Tooltip} from 'antd'
import {
    DeleteOutlined,
    DiffOutlined,
    EditOutlined,
    HistoryOutlined,
    PlusOutlined,
    ReloadOutlined,
    RollbackOutlined,
} from '@ant-design/icons'
import {useLocation, useNavigate} from 'react-router-dom'
import {configApi} from '@/config/services/api'
import {EmptyState, ErrorState, PageHeader, SearchInput} from '@/common/components/ui'

/**
 * 配置列表页 - 交互修复要点 (来自 ui-ux-pro-max / frontend-ui-engineering / anthropics):
 *   - 合并 useEffect, 避免首次加载双重请求
 *   - 用 SearchInput 替换 Input.Search, 输入即时搜索 (debounce 300ms), Enter 立即, Esc 清空
 *   - 错误就近显示 (ErrorState), 不用全局 message 一闪而过
 *   - 空状态用 EmptyState 引导下一步
 *   - PageHeader 统一标题 + 面包屑 + 操作区
 */
const ConfigList = () => {
    const navigate = useNavigate()
    const location = useLocation()

    const [loading, setLoading] = useState(false)
    const [data, setData] = useState([])
    const [pagination, setPagination] = useState({current: 1, pageSize: 10, total: 0})
    const [searchText, setSearchText] = useState('')
    const [selectedNamespace, setSelectedNamespace] = useState(undefined)
    const [selectedGroup, setSelectedGroup] = useState(undefined)
    const [namespaceList, setNamespaceList] = useState([])
    const [groupList, setGroupList] = useState([])
    const [namespaceLoaded, setNamespaceLoaded] = useState(false)
    // FIX: 错误就近显示, 不用全局 message
    const [loadError, setLoadError] = useState(null)
    const [historyDrawerOpen, setHistoryDrawerOpen] = useState(false)
    const [historyRecord, setHistoryRecord] = useState(null)
    const [historyList, setHistoryList] = useState([])
    const [historyLoading, setHistoryLoading] = useState(false)
    const [historyError, setHistoryError] = useState(null)
    const [compareVer1, setCompareVer1] = useState(null)
    const [compareVer2, setCompareVer2] = useState(null)
    const [diffContent, setDiffContent] = useState({left: '', right: ''})
    const [diffModalOpen, setDiffModalOpen] = useState(false)

    // ===== 数据获取 =====
    const fetchNamespaceList = async () => {
        try {
            // 原来这里把 clusterList() 的集群名并进命名空间下拉，两个都是假的：
            //   /api/cluster/list 实测 200 但 0 行（z_cluster 空表）⇒ 并进来永远是空集；
            //   而且"集群"和"命名空间"不是一个概念（后端 ZConfigPageRequest 只有 nameSpace）。
            // 也删掉了 merged 为空时兜底伪造的 [{value:'DEFAULT_NAMESPACE'}]——
            // 那是"看着有下拉、选了拿不到数据"的假能力。实测命名空间只有 ["public"]。
            const nsList = await configApi.namespaceList().catch(() => [])
            setNamespaceList((nsList || []).map(n => ({label: n, value: n})))
            setSelectedNamespace(prev => prev || (nsList || [])[0])
            setNamespaceLoaded(true)
        } catch (e) {
            console.error('获取命名空间失败', e)
        }
    }

    const fetchGroupList = async () => {
        try {
            const list = await configApi.groupList()
            setGroupList((list || []).map(item => ({label: item, value: item})))
        } catch (e) {
            console.error('获取Group失败', e)
        }
    }

    const fetchConfigList = async (page = 1, pageSize = 10) => {
        setLoading(true)
        setLoadError(null)
        try {
            const result = await configApi.pageConfig({
                current: page,
                size: pageSize,
                search: searchText || undefined,
                // 全局 SNAKE_CASE 对请求体双向生效：发 nameSpace 时这个筛选会被整个丢掉（实测回到全表），
                // 发 name_space 才真的过滤。详见 src/config/services/api.js 的同一条注释。
                name_space: selectedNamespace,
                group: selectedGroup,
            })
            if (result && result.records) {
                setData(result.records.map((item, i) => ({key: item.id || i, ...item})))
                setPagination({
                    current: result.current || 1,
                    pageSize: result.size || pageSize,
                    total: result.total || 0
                })
            }
        } catch (error) {
            // 原来这里压成字符串（`error?.message`）⇒ ErrorState 只能拿到 axios 的
            // "Request failed with status code 500"，真因（响应体 message）在压的那一刻就丢了。
            // 传整个 error，由 ErrorState 优先取后端 message。
            setLoadError(error)
        } finally {
            setLoading(false)
        }
    }

    // FIX: 路径切换时重置并刷新 (合并到一个 useEffect, 避免分散)
    useEffect(() => {
        setNamespaceLoaded(false)
        setSearchText('')
        setSelectedGroup(undefined)
        fetchNamespaceList()
        fetchGroupList()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [location.key])

    // FIX: 仅在 namespace/group/search 变化时重拉 (避免 namespaceList 加载完成后双重请求)
    useEffect(() => {
        if (!namespaceLoaded) return
        fetchConfigList()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedNamespace, selectedGroup, searchText, namespaceLoaded])

    // ===== 操作 =====
    const handleDelete = async (record) => {
        try {
            // 键名两件事是实测/handler 给的：deleteConfig 的 handler 收 @RequestBody(ZConfigQueryRequest)，
            // 原来发 query ⇒ body 恒空；且 SNAKE_CASE 双向生效 ⇒ 请求体键要 snake_case。
            // ⚠️ 两点都没跑过：写路径不拿共享库试；record.* 也还没有实测依据（见上面"表格列"的说明）。
            await configApi.deleteConfig({
                name_space: record.namespace, group: record.group, data_id: record.data_id
            })
            message.success(`已删除 ${record.data_id}`)
            fetchConfigList(pagination.current, pagination.pageSize)
        } catch (error) {
            // 整个 error 交给 ErrorState，它优先取后端 message；压成字符串就把真因丢了。
            setLoadError(error)
        }
    }

    const handleEdit = (record) => navigate('/config/edit', {state: {config: record}})
    const handleTableChange = (p) => fetchConfigList(p.current, p.pageSize)

    // ===== 变更历史 =====
    const openHistory = async (record) => {
        setHistoryRecord(record)
        setHistoryDrawerOpen(true)
        setHistoryLoading(true)
        setHistoryError(null)
        try {
            // 不发 namespace：ZConfigHistoryPageRequest.namespace 是 Long，传 "public" 直接 400。
            // 分页只认 current/size（pageNum/pageSize 被忽略 ⇒ 整表进浏览器）。
            const res = await configApi.historyPage({
                current: 1,
                size: 50,
                group: record.group,
                search: record.data_id
            })
            setHistoryList((res?.records || []).map((r, i) => ({key: r.id ?? i, ...r})))
        } catch (e) {
            // 真因在 e.response.data.message（后端把 SQL 报错原文放进 message），
            // 原来一句 message.error('加载历史失败') 把它丢了 ⇒ 就近渲染 ErrorState。
            setHistoryError(e)
            setHistoryList([])
        } finally {
            setHistoryLoading(false)
        }
    }

    const handleRollback = async (item) => {
        try {
            // ZConfigSaveRequest 的字段同样被 SNAKE_CASE 改名 ⇒ dataId→data_id、configDesc→config_desc。
            // item.* 读的是 history 响应体，实测键名本来就是 snake_case（原来读 item.dataId 恒 undefined）。
            // ⚠️ 写路径未在共享库上实测。
            await configApi.saveConfig({
                namespace: item.namespace,
                data_id: item.data_id,
                group: item.group,
                content: item.content,
                config_desc: `回滚至 ${item.gmt_create}`
            })
            message.success('回滚成功')
            setHistoryDrawerOpen(false)
            fetchConfigList()
        } catch (e) {
            message.error(e?.response?.data?.message || '回滚失败')
        }
    }

    const handleCompare = () => {
        if (!compareVer1 || !compareVer2) {
            message.warning('请选择两个版本')
            return
        }
        setDiffContent({left: compareVer1.content || '', right: compareVer2.content || ''})
        setDiffModalOpen(true)
    }

    // ===== 表格列 =====
    // ⚠️ 下面的 dataIndex 一律**没有实测依据**：21:0x 逐形状探过 pageConfig / list / listConfig / getConfig，
    //   凡是能命中 ≥1 行的形状都 500（oc.z_config_info 缺 encrypted_data_key，见 TASK-20260925-036），
    //   200 的那些响应 records 恒为 0 行 ⇒ 这一页从来没有拿到过一条真记录的键名。
    //   所以这里不照抄 ConfigHistory 的 snake_case（那是量出来的），也不"顺手改"：
    //   补列之后必须重新量一次响应再定列名，否则就是把上一轮 变更历史 的"假绿列"原样再造一遍。
    const columns = [
        {title: 'Data ID', dataIndex: 'dataId', key: 'dataId', ellipsis: true},
        {title: 'Group', dataIndex: 'group', key: 'group'},
        {title: '应用名', dataIndex: 'appName', key: 'appName'},
        {
            title: '类型', dataIndex: 'configType', key: 'configType',
            render: (type) => <Tag color="blue">{type || 'TEXT'}</Tag>
        },
        {
            title: '操作', key: 'action', width: 180,
            render: (_, record) => (
                <Space size={4}>
                    <Button type="link" size="small" icon={<EditOutlined/>}
                            onClick={() => handleEdit(record)}>编辑</Button>
                    <Button type="link" size="small" icon={<HistoryOutlined/>}
                            onClick={() => openHistory(record)}>历史</Button>
                    <Popconfirm
                        title="确认删除"
                        description={`确定要删除 "${record.dataId}" 吗? 此操作不可撤销.`}
                        onConfirm={() => handleDelete(record)}
                        okText="确定删除" cancelText="取消" okButtonProps={{danger: true}}
                    >
                        <Button type="link" danger size="small" icon={<DeleteOutlined/>}>删除</Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ]

    // ===== 渲染 =====
    const isEmpty = !loading && !loadError && data.length === 0 && namespaceLoaded

    return (
        <div>
            <PageHeader
                title="配置列表"
                subtitle="集中管理应用的运行时配置, 支持命名空间隔离、变更历史与版本对比"
                breadcrumb={[{label: '配置中心', href: '/config/list'}, {label: '配置列表'}]}
            />

            {/* 错误状态 - 就近显示 (替代全局 message) */}
            {loadError && !loading && (
                <div style={{marginBottom: 16}}>
                    <ErrorState
                        error={loadError}
                        title="加载配置失败"
                        onRetry={() => fetchConfigList()}
                    />
                </div>
            )}

            <Card styles={{body: {padding: 20}}}>
                <div style={{marginBottom: 16}}>
                    <Tabs
                        activeKey={selectedNamespace}
                        onChange={(key) => {
                            setSelectedNamespace(key)
                            fetchConfigList(1, pagination.pageSize)
                        }}
                        items={namespaceList.map(ns => ({key: ns.value, label: ns.label}))}
                    />
                    <Space size={12} wrap>
                        <Select
                            placeholder="选择 Group"
                            style={{width: 200}}
                            value={selectedGroup}
                            onChange={setSelectedGroup}
                            allowClear
                            options={groupList}
                        />
                        <SearchInput
                            placeholder="搜索 Data ID..."
                            value={searchText}
                            onChange={setSearchText}
                            size="large"
                        />
                        <Button icon={<ReloadOutlined/>}
                                onClick={() => fetchConfigList(pagination.current, pagination.pageSize)}
                                loading={loading}>刷新</Button>
                        <Button type="primary" icon={<PlusOutlined/>}
                                onClick={() => navigate('/config/edit')}>新建配置</Button>
                    </Space>
                </div>

                {/* 表格 或 空状态 */}
                {isEmpty ? (
                    <EmptyState
                        title="暂无配置"
                        description="该命名空间下还没有任何配置项, 用上方「新建配置」加第一条."
                        actionText="新建配置"
                        onAction={() => navigate('/config/edit')}
                    />
                ) : (
                    <Table
                        columns={columns}
                        dataSource={data}
                        pagination={{
                            ...pagination,
                            showSizeChanger: true,
                            showTotal: (t) => `共 ${t} 条`,
                        }}
                        loading={loading}
                        onChange={handleTableChange}
                        rowKey="key"
                    />
                )}
            </Card>

            {/* 变更历史抽屉 */}
            <Drawer
                title={`变更历史 - ${historyRecord?.data_id || ''}`}
                open={historyDrawerOpen}
                onClose={() => setHistoryDrawerOpen(false)}
                size="large"
            >
                {/* 历史接口挂了要在抽屉里就近说清（真因在后端 message 里），不要只留一张空表 */}
                {historyError && (
                    <div style={{marginBottom: 12}}>
                        <ErrorState error={historyError} title="加载该配置的历史失败"
                                    onRetry={() => openHistory(historyRecord)}/>
                    </div>
                )}
                <Space style={{marginBottom: 12}} wrap>
                    <Select
                        placeholder="选择版本 1"
                        style={{width: 250}}
                        allowClear
                        value={compareVer1?.id}
                        onChange={(id) => setCompareVer1(id ? historyList.find(r => r.id === id) : null)}
                        options={historyList.map(r => ({
                            label: `${r.gmt_create} (${r.op_type})`,
                            value: r.id
                        }))}
                    />
                    <Select
                        placeholder="选择版本 2"
                        style={{width: 250}}
                        allowClear
                        value={compareVer2?.id}
                        onChange={(id) => setCompareVer2(id ? historyList.find(r => r.id === id) : null)}
                        options={historyList.map(r => ({
                            label: `${r.gmt_create} (${r.op_type})`,
                            value: r.id
                        }))}
                    />
                    <Button
                        icon={<DiffOutlined/>}
                        onClick={handleCompare}
                        disabled={!compareVer1 || !compareVer2}
                    >对比</Button>
                </Space>
                <Table
                    columns={[
                        // 键名与 /api/config/history/page 的实测响应一致（snake_case），
                        // 与 src/config/pages/ConfigHistory.jsx 同一份判据。
                        {title: '操作时间', dataIndex: 'gmt_create', key: 'gmt_create', width: 160,
                            render: (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-')},
                        {
                            title: '操作类型', dataIndex: 'op_type', key: 'op_type', width: 80,
                            render: (op) => (
                                <Tag color={op === '新增' ? 'success' : op === '修改' ? 'processing' : 'red'}>
                                    {op}
                                </Tag>
                            )
                        },
                        {title: '操作人', dataIndex: 'src_user', key: 'src_user', width: 80},
                        {
                            title: '当前内容', key: 'content', ellipsis: true,
                            render: (_, r) => (
                                <Tooltip
                                    title={<pre style={{margin: 0, maxHeight: 200, overflow: 'auto'}}>{r.content}</pre>}
                                >
                                    <span style={{cursor: 'pointer'}}>{(r.content || '').substring(0, 60)}</span>
                                </Tooltip>
                            )
                        },
                        {
                            title: '操作', key: 'action', width: 100,
                            render: (_, r) => (
                                <Popconfirm title="确认回滚到此版本?" onConfirm={() => handleRollback(r)}>
                                    <Button type="link" size="small" icon={<RollbackOutlined/>}>回滚</Button>
                                </Popconfirm>
                            )
                        },
                    ]}
                    dataSource={historyList}
                    loading={historyLoading}
                    size="small"
                    pagination={{pageSize: 10, showTotal: t => `共 ${t} 条`}}
                    rowKey="key"
                />
            </Drawer>

            {/* 版本对比弹窗 */}
            <Modal
                title="版本对比"
                open={diffModalOpen}
                size="large"
                footer={null}
                onCancel={() => setDiffModalOpen(false)}
            >
                <div style={{display: 'flex', gap: 8}}>
                    <pre style={{
                        flex: 1, background: '#f5f5f5', padding: 12, borderRadius: 4,
                        maxHeight: 500, overflow: 'auto', fontSize: 13, margin: 0
                    }}>{diffContent.left}</pre>
                    <pre style={{
                        flex: 1, background: '#fff7e6', padding: 12, borderRadius: 4,
                        maxHeight: 500, overflow: 'auto', fontSize: 13, margin: 0
                    }}>{diffContent.right}</pre>
                </div>
            </Modal>
        </div>
    )
}

export default ConfigList
