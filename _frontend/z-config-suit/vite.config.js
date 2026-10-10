import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// z-config 独立运行壳。dev 3002；proxy /api → z-config server（本地 8888 或后端约定端口）。
export default defineConfig({
    plugins: [react()],
  resolve: { dedupe: ['react', 'react-dom', 'react-router-dom', 'antd', '@ant-design/icons', 'axios'] ,
        alias: process.env.LOCAL_SIBLINGS === '1' ? { '@yuku123/z-config-component': '../z-config-component/src' } : {}},
    server: {
        port: 3002,
        fs: { allow: ['..'] },
        proxy: { '/api': { target: 'http://localhost:8888', changeOrigin: true } },
    },
})
