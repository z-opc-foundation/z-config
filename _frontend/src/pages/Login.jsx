import {useState} from 'react'
import {useNavigate} from 'react-router-dom'
import {Button, Card, Form, Input, message} from 'antd'
import {LockOutlined, UserOutlined} from '@ant-design/icons'
import request, {setToken} from '../utils/request'
import './Login.css'

const Login = () => {
    const [loading, setLoading] = useState(false)
    const navigate = useNavigate()

    const onFinish = async (values) => {
        setLoading(true)
        try {
            // 共享 request 已经按标准 {code, data, message} 解包为 data
            const data = await request.post('/auth/login', values)
            if (data && data.token) {
                setToken(data.token)
                localStorage.setItem('zconfig_user', JSON.stringify(data))
                message.success('登录成功')
                navigate('/')
            } else {
                message.error('登录响应缺少 token')
            }
        } catch (error) {
            console.error('登录错误:', error)
            message.error(error.message || '网络错误')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="login-page">
            <Card className="login-card" title="Z-Config 配置中心" bordered={false}>
                <p className="login-subtitle">分布式配置管理平台</p>
                <Form
                    name="login"
                    initialValues={{username: 'admin', password: 'admin'}}
                    onFinish={onFinish}
                    autoComplete="off"
                    size="large"
                >
                    <Form.Item
                        name="username"
                        rules={[{required: true, message: '请输入用户名'}]}
                    >
                        <Input prefix={<UserOutlined/>} placeholder="用户名"/>
                    </Form.Item>

                    <Form.Item
                        name="password"
                        rules={[{required: true, message: '请输入密码'}]}
                    >
                        <Input.Password prefix={<LockOutlined/>} placeholder="密码"/>
                    </Form.Item>

                    <Form.Item>
                        <Button type="primary" htmlType="submit" loading={loading} block>
                            登录
                        </Button>
                    </Form.Item>
                </Form>
                <p className="login-hint">默认账号: admin / admin</p>
            </Card>
        </div>
    )
}

export default Login
