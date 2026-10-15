import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// z-config 组件层双入口（lead 005 §1.2/§9）：'.' 桶导出 + './pages' manifest
export default defineConfig({
    plugins: [react()],
    resolve: {
        // src/pages/*.jsx 用 @/services/api 与 @/common/components/ui 引本仓内部模块。
        // library mode 的 rollup 不读 tsconfig 的 paths，alias 必须配在这里，否则解析不了直接构建失败。
        alias: { '@': path.resolve(__dirname, 'src') },
        dedupe: ['react', 'react-dom', 'react-router-dom', 'antd', '@ant-design/icons', 'axios'],
    },
    build: {
        lib: {
            entry: {
                index: path.resolve(__dirname, 'src/index.js'),
                pages: path.resolve(__dirname, 'src/pages.js'),
            },
            formats: ['es'],
        },
        outDir: 'dist',
        emptyOutDir: true,
        rollupOptions: {
            external: ['react', 'react-dom', 'react-dom/client', 'react-router-dom',
                       'antd', '@ant-design/icons', 'axios', '@yuku123/z-frontend-common'],
        },
    },
});
