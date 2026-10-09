import React from 'react'
import ReactDOM from 'react-dom/client'
import {BrowserRouter} from 'react-router-dom'
import {ConfigProvider} from 'antd'
import zhCN from 'antd/locale/zh_CN'
import App from './App'
import './index.css'

// suit 只做壳（lead 005 §9.1）：入口组装。默认 apiBase=''（component request
// 路径自带 /api 前缀，vite proxy / nginx 同源反代即可）。
ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <ConfigProvider locale={zhCN}>
            <BrowserRouter>
                <App/>
            </BrowserRouter>
        </ConfigProvider>
    </React.StrictMode>,
)
