import {useEffect, useState} from 'react'
import {Button, Input, message, Select, Space, Tag} from 'antd'
import {ReloadOutlined, SearchOutlined} from '@ant-design/icons'
import axios from 'axios'
import {ErrorState, ListTable} from '@yuku123/z-frontend-common/ui'

/**
 * 变更历史页（component 路由 /z-config/config/history 实际挂载的就是本文件）。
 *
 * ⚠️ 命名空间筛选已**整块删除**（不是禁用、不是隐藏）—— 后端收不住这个形状：
 *    ZConfigHistoryPageRequest{search:String, namespace:**Long**, group:String}
 *    （1.0.7 / 1.0.8 两版源码一致，不是版本漂移），
 *    而 GET /api/config/namespaceList 实测只回字符串 ["public"]
 *    ⇒ 命名空间塞进 Long 直接 400「Cannot deserialize value of type java.lang.Long from String "public"」。
 *    留着"选了就 400"的控件就是假能力。
 *    顺带说明：原来这个下拉的数据源是 /api/cluster/list（集群≠命名空间，且实测返回空数组），
 *    也就是"看着能选、选了必错"。
 *
 * ① 分页只认 current/size（PageRequest）：实测 {"current":2,"size":3} ⇒ 3 行且首条换人；
 *    {"pageNum":1,"pageSize":3} ⇒ 整表 11 行。
 * ② 响应体键名是 snake_case（实测 records[0]）：
 *    id/nid/data_id/group/app_name/content/md5/gmt_create/gmt_modified/src_user/src_ip/op_type/namespace
 *    ⇒ 表格 dataIndex 必须用 snake_case，否则列是**恒空的假绿列**。
 */
const ConfigHistory = () => {
    const [loading, setLoading] = useState(false)
    const [data, setData] = useState([])
    const [pagination, setPagination] = useState({
        current: 1,
        pageSize: 10,
        total: 0,
    })
    const [searchText, setSearchText] = useState('')
    const [selectedGroup, setSelectedGroup] = useState(undefined)
    const [groupList, setGroupList] = useState([])
    // lead 008 §17：读失败常驻报错 + 重试，避免空表被误读成"没有变更历史"。
    const [loadError, setLoadError] = useState(null)

    // 获取Group列表
    const fetchGroupList = async () => {
        try {
            const res = await axios.get('/api/config/groupList')
            setGroupList(res.data.map(item => ({label: item, value: item})))
        } catch (e) {
            console.error('获取Group失败', e)
        }
    }

    // 获取变更历史列表
    const fetchHistoryList = async (page = 1, pageSize = 10) => {
        setLoading(true)
        try {
            const res = await axios.post('/api/config/history/page', {
                // ① 分页只认 current/size（pageNum/pageSize 被忽略 ⇒ 整表进浏览器）
                current: page,
                size: pageSize,
                search: searchText || undefined,
                // ⚠️ 不发 namespace：后端 namespace 是 Long，字符串命名空间一选就 400（见文件头 R4 说明）
                group: selectedGroup,
            })
            const records = res.data.data.records || []
            const total = res.data.data.total || 0
            const current = res.data.data.current || 1
            setData(records.map((item, index) => ({
                key: item.id || index,
                ...item,
            })))
            setPagination({
                current: current,
                pageSize: pageSize,
                total: total,
            })
            setLoadError(null)
        } catch (e) {
            // 别把真因压成"获取变更历史失败"：后端把原因放在 response.data.message 里
            setLoadError(e?.response?.data?.message || e?.message || '获取变更历史失败')
            console.error(e)
        } finally {
            setLoading(false)
        }
    }

    // 处理表格分页变化
    const handleTableChange = (newPagination) => {
        fetchHistoryList(newPagination.current, newPagination.pageSize)
    }

    // 处理搜索
    const handleSearch = () => {
        fetchHistoryList(1, pagination.pageSize)
    }

    // 初始加载
    useEffect(() => {
        fetchHistoryList()
        fetchGroupList()
    }, [])

    // ② 表格列的 dataIndex 一律用**实测响应键**（snake_case）。
    //    改之前这里是 dataId/appName/opType/srcUser/srcIp/gmtCreate —— 响应里是
    //    data_id/app_name/op_type/src_user/src_ip/gmt_create ⇒ 6 列恒空（假绿列）。
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
            title: '操作类型',
            dataIndex: 'op_type',
            key: 'op_type',
            render: (op) => (
                <Tag color={op === '新增' ? 'success' : op === '修改' ? 'blue' : 'red'}>
                    {op}
                </Tag>
            ),
        },
        {
            title: '操作人',
            dataIndex: 'src_user',
            key: 'src_user',
        },
        {
            title: '操作IP',
            dataIndex: 'src_ip',
            key: 'src_ip',
        },
        {
            title: 'MD5',
            dataIndex: 'md5',
            key: 'md5',
            ellipsis: true,
        },
        {
            title: '操作时间',
            dataIndex: 'gmt_create',
            key: 'gmt_create',
        },
    ]

    return (
        <div>
            <div style={{marginBottom: 16}}>
                <Space size="middle">
                    {/* 命名空间筛选已整块删除：后端 namespace 是 Long，字符串命名空间一选就 400
                        （见文件头说明）—— 留一个"选了就 400"的控件就是假能力。
                        集群下拉也不并进来：集群 ≠ 命名空间。 */}
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
                    <Button
                        icon={<ReloadOutlined/>}
                        onClick={() => fetchHistoryList(pagination.current, pagination.pageSize)}
                        loading={loading}
                    >
                        刷新
                    </Button>
                </Space>
            </div>
            {/* 服务端分页，不套客户端分页：
                - 分页对象保留原样（current/pageSize/total 由后端驱动，翻页重查接口），
                  只补 §2 规则 2 要求的 pageSizeOptions；
                - 过滤走后端（search 参数），不传 searchKeys，否则页面上会出现两个搜索框；
                - 空态由基座 <EmptyState/> 承接。 */}
            {loadError && (
                <ErrorState
                    error={loadError}
                    title="变更历史没有读到"
                    description="接口没有返回数据，下方空表不代表真的没有变更记录。"
                    onRetry={() => fetchHistoryList(pagination.current, pagination.pageSize)}
                    style={{marginBottom: 16}}
                />
            )}
            <ListTable
                cardTitle="变更历史"
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
                emptyTitle={loadError ? '数据没有读到' : '暂无变更历史'}
                emptyDescription={loadError ? '上方错误条已说明原因，点「重新加载」重试。' : '当前没有配置变更记录'}
            />
        </div>
    )
}

export default ConfigHistory
