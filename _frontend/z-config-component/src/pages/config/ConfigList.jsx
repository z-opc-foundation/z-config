import {useEffect, useState} from 'react'
import {Button, Card, Input, message, Popconfirm, Select, Space, Table, Tag, Upload} from 'antd'
import {DeleteOutlined, DownloadOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, UploadOutlined} from '@ant-design/icons'
import {useNavigate} from 'react-router-dom'
import axios from 'axios'
import ConfigStatusBadge from './ConfigStatusBadge'

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
    const fetchNamespaceList = async () => {
        try {
            const res = await axios.get('/api/cluster/list')
            setNamespaceList(res.data.map(item => ({label: item.name, value: item.name})))
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
                pageNum: page,
                pageSize: pageSize,
                search: searchText || undefined,
                nameSpace: selectedNamespace,
                group: selectedGroup,
            })

            if (result.data.success && result.data.data) {
                // 适配后端返回的字段名: records, total, current
                const records = result.data.data.records || []
                const total = result.data.data.total || 0
                const current = result.data.data.current || 1

                // 调试：打印第一条记录的字段名
                if (records.length > 0) {
                    console.log('第一条记录字段:', Object.keys(records[0]))
                    console.log('第一条记录:', records[0])
                }

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
            message.error('网络错误')
        } finally {
            setLoading(false)
        }
    }

    // 删除配置
    const handleDelete = async (record) => {
        try {
            const result = await axios.post('/api/config/delete', {
                nameSpace: record.namespace,
                group: record.group,
                dataId: record.dataId,
            })

            if (result.data.success) {
                message.success('删除成功')
                fetchConfigList(pagination.current, pagination.pageSize)
            } else {
                message.error(result.data.message || '删除失败')
            }
        } catch (error) {
            console.error('删除错误:', error)
            message.error('网络错误')
        }
    }

    // 处理编辑
    const handleEdit = (record) => {
        navigate('/config/edit', {
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
            title: '类型',
            dataIndex: 'configType',
            key: 'configType',
            render: (type) => (
                <Tag color="blue">{type || 'TEXT'}</Tag>
            ),
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status) => (
                <ConfigStatusBadge status={status}/>
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
                        description={`确定要删除配置 "${record.dataId}" 吗？`}
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
            <Card
                title="配置列表"
                extra={
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
            >
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

export default ConfigList
