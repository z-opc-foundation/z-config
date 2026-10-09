import {Badge} from 'antd'

const workflowColor = {
    DRAFT: 'default',
    PUBLISHED: 'success',
    DEPRECATED: 'warning',
    DELETED: 'error',
}
const enabledColor = {true: 'success', false: 'default'}
const enabledText = {true: '正常', false: '停用'}

export default function ConfigStatusBadge({status, mode = 'enabled'}) {
    if (mode === 'workflow') {
        const color = workflowColor[status] || 'default'
        return <Badge status={color} text={status}/>
    }
    const isOn = status === 1 || status === true || status === '1'
    return <Badge status={enabledColor[isOn]} text={enabledText[isOn]}/>
}
