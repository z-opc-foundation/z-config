import React from 'react'
import ReactDOM from 'react-dom/client'
import {ConfigProvider} from 'antd'
import zhCN from 'antd/locale/zh_CN'
import App from './App'
import './index.css'

// suit 只做壳（lead 005 §9.1）：入口组装。默认 apiBase=''（component request
// 路径自带 /api 前缀，vite proxy / nginx 同源反代即可）。
// Router 只由 App 提供 —— 这里再包一层 BrowserRouter 会让 App 里的那个变成
// "Router inside Router"，react-router 直接抛错，整页只剩一段红字。
ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <ConfigProvider locale={zhCN}>
            <App/>
        </ConfigProvider>
    </React.StrictMode>,
)
