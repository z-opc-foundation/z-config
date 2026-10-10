import {useEffect, useState} from 'react'
import {Button, Card, message, Select, Space, Table, Tag} from 'antd'
import {ReloadOutlined} from '@ant-design/icons'
import {configApi} from '@/config/services/api'
import {EmptyState, ErrorState, PageHeader, SearchInput} from '@/common/components/ui'

const ConfigHistory = () => {
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState(null)
    const [data, setData] = useState([])
    const [pagination, setPagination] = useState({current: 1, pageSize: 10, total: 0})
    const [searchText, setSearchText] = useState('')
    const [selectedGroup, setSelectedGroup] = useState(undefined)
    const [groupList, setGroupList] = useState([])

    useEffect(() => {
        fetchHistoryList();
        loadFilterOptions()
    }, [])

    const loadFilterOptions = async () => {
        try {
            const r = await configApi.groupList();
            setGroupList((r || []).map(i => ({label: i, value: i})))
        } catch (e) {
        }
    }

    // 🔴 请求体字段名按 21:0x 制品源码 + 实测双向对齐，不要"顺手改回 pageNum/pageSize"：
    //   ZConfigHistoryPageRequest extends PageRequest ⇒ 服务端只认 current/size，
    //   实测 {"current":2,"size":3} 给 3 行且首条换人，{"pageNum":1,"pageSize":3} 仍给全 8 行。
    //   原来这里发 pageNum/pageSize：后端把整表塞回来，antd 在前端切片，
    //   于是"翻页能用"是假的（这正是 TASK-20260925-035 记下的同一个病）。
    //   命名空间下拉已整块删除：同一个 DTO 里 `private Long namespace`（1.0.7 / 1.0.8 两版源码一致），
    //   而 /api/config/namespaceList 实测回 ["public"] —— 字符串塞进 Long 直接 400
    //   （"Cannot deserialize value of type `java.lang.Long` from String \"public\""）。
    //   留一个"选了就 400"的下拉就是假能力，所以不如有一个空。详见 TASK-20260925-036。
    const fetchHistoryList = async (page = 1, pageSize = 10) => {
        setLoading(true)
        try {
            const result = await configApi.historyPage({
                current: page, size: pageSize,
                search: searchText || undefined,
                group: selectedGroup,
            })
            if (result && result.records) {
                setData(result.records.map((item, i) => ({key: item.id || i, ...item})))
                setPagination({current: result.current || 1, pageSize, total: result.total || 0})
            }
        } catch (e) {
            message.error('获取变更历史失败')
            setError(e)
        } finally {
            setLoading(false)
        }
    }

    // 🔴 列名按 20:5x 逐字段实测写死，不要"顺手改回 camelCase"：
    //   `POST /api/config/history/page` 把 `ZConfigInfoHistory` 实体直接放进 data（制品里实测到的类名），
    //   合并进程的全局 SNAKE_CASE 把 bean 属性改名 ⇒ 响应里的键是
    //   data_id / app_name / op_type / src_user / gmt_create。
    //   改之前这页是典型的"假绿页"：接口 200、表里 8 行，但 7 列里有 5 列的 dataIndex 在响应体里根本不存在。
    const OP_COLORS = {新增: 'success', 修改: 'blue', 删除: 'red'}
    const columns = [
        {title: 'Data ID', dataIndex: 'data_id', key: 'data_id', ellipsis: true},
        {title: 'Group', dataIndex: 'group', key: 'group'},
        {title: '应用名', dataIndex: 'app_name', key: 'app_name',
            render: (v) => (v === null || v === undefined || v === '' ? '-' : String(v))},
        {
            title: '操作类型', dataIndex: 'op_type', key: 'op_type',
            render: (op) => <Tag color={OP_COLORS[op] || 'default'}>{op}</Tag>
        },
        {title: '操作人', dataIndex: 'src_user', key: 'src_user',
            render: (v) => (v === null || v === undefined || v === '' ? '-' : String(v))},
        {title: 'MD5', dataIndex: 'md5', key: 'md5', ellipsis: true},
        // 实测是 ISO 串 "2026-09-06T21:11:33"，直接渲染会带个 T
        {title: '操作时间', dataIndex: 'gmt_create', key: 'gmt_create',
            render: (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-')}
    ]

    return (
        <div>
            <PageHeader title="变更历史"/>
            <Card>
                <div style={{marginBottom: 16}}>
                    <Space size="middle">
                        <Select placeholder="选择Group" style={{width: 200}} value={selectedGroup}
                                onChange={setSelectedGroup} allowClear options={groupList}/>
                        <SearchInput placeholder="搜索 Data ID..." value={searchText}
                                     onChange={setSearchText}
                                     onSearch={() => fetchHistoryList(1, pagination.pageSize)}
                                     size="large" allowClear/>
                        <Button type="primary" onClick={() => fetchHistoryList(1, pagination.pageSize)}>搜索</Button>
                        <Button icon={<ReloadOutlined/>}
                                onClick={() => fetchHistoryList(pagination.current, pagination.pageSize)}
                                loading={loading}>刷新</Button>
                    </Space>
                </div>
                {error ? (
                    <ErrorState error={error} onRetry={() => fetchHistoryList(pagination.current, pagination.pageSize)}
                                type="inline"/>
                ) : (
                    <Table columns={columns} dataSource={data} loading={loading}
                           rowKey={(record) => record?.id ?? record?.key}
                           pagination={{...pagination, showSizeChanger: true, showTotal: t => `共 ${t} 条`}}
                           onChange={p => fetchHistoryList(p.current, p.pageSize)}
                           locale={{
                               emptyText: (
                                   <EmptyState title="暂无变更历史" description="当前没有配置变更记录"/>
                               ),
                           }}/>
                )}
            </Card>
        </div>
    )
}

export default ConfigHistory
