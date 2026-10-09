import {useEffect, useState} from 'react'
import {Button, Card, Input, message, Select, Space, Table, Tag} from 'antd'
import {ReloadOutlined, SearchOutlined} from '@ant-design/icons'
import axios from 'axios'

const ConfigHistory = () => {
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
    const fetchNamespaceList = async () => {
        try {
            const res = await axios.get('/api/cluster/list')
            setNamespaceList(res.data.map(item => ({label: item.name, value: item.id})))
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

    // 获取变更历史列表
    const fetchHistoryList = async (page = 1, pageSize = 10) => {
        setLoading(true)
        try {
            const res = await axios.post('/api/config/history/page', {
                pageNum: page,
                pageSize: pageSize,
                search: searchText || undefined,
                namespace: selectedNamespace,
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
        } catch (e) {
            message.error('获取变更历史失败')
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
        fetchNamespaceList()
        fetchGroupList()
    }, [])

    const columns = [
        {
            title: 'Data ID',
            dataIndex: 'dataId',
            key: 'dataId',
            ellipsis: true,
        },
        {
            title: 'Group',
            dataIndex: 'group',
            key: 'group',
        },
        {
            title: '应用名',
            dataIndex: 'appName',
            key: 'appName',
        },
        {
            title: '操作类型',
            dataIndex: 'opType',
            key: 'opType',
            render: (op) => (
                <Tag color={op === '新增' ? 'success' : op === '修改' ? 'blue' : 'red'}>
                    {op}
                </Tag>
            ),
        },
        {
            title: '操作人',
            dataIndex: 'srcUser',
            key: 'srcUser',
        },
        {
            title: '操作IP',
            dataIndex: 'srcIp',
            key: 'srcIp',
        },
        {
            title: 'MD5',
            dataIndex: 'md5',
            key: 'md5',
            ellipsis: true,
        },
        {
            title: '操作时间',
            dataIndex: 'gmtCreate',
            key: 'gmtCreate',
        },
    ]

    return (
        <div>
            <Card title="变更历史">
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
                        <Button
                            icon={<ReloadOutlined/>}
                            onClick={() => fetchHistoryList(pagination.current, pagination.pageSize)}
                            loading={loading}
                        >
                            刷新
                        </Button>
                    </Space>
                </div>
                <Table
                    columns={columns}
                    dataSource={data}
                    pagination={{
                        ...pagination,
                        showSizeChanger: true,
                        showTotal: (total) => `共 ${total} 条`,
                    }}
                    loading={loading}
                    onChange={handleTableChange}
                />
            </Card>
        </div>
    )
}

export default ConfigHistory
