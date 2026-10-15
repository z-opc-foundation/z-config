/** ./pages 入口：manifest + apiBase 注入（§8.2/§8.3/§9.3）。 */

// 整份转发，不要手挑名字：上一版这里写死了 `menuItems, routes, isAuthenticated, Dashboard, Login`
// 五个，而 manifest 还导出了 HomePage / LoginPage —— suit 的 App.jsx 正好 import 这两个，
// 于是 rollup 报 "LoginPage is not exported by dist/pages.js"，构建直接红。
// 再挑一次名字，下个新增页面还会重演一遍。
export * from './pages-manifest.jsx';

export { configureConfig } from './services/request.js';
