# WOMS 工单管理系统 · PC 前端

Vue 3 + Vite + Element Plus + ECharts + Pinia + Vue Router + Axios

## 运行步骤

```bash
# 1. 安装依赖（在项目根目录）
npm install

# 2. 启动开发服务器（默认 5173 端口）
npm run dev
```

浏览器打开 `http://localhost:5173`，使用后端创建的管理员账号登录。

## 前提

- 后端已在 `localhost:8080` 启动（WOMS Spring Boot 项目）
- Redis 已启动
- `sys_user` 表已存在管理员账号（admin / 你设置的密码）

## 关键机制

| 机制 | 说明 |
|---|---|
| 接口代理 | `/api` 前缀请求由 Vite 代理转发到后端 `localhost:8080`（见 `vite.config.js`），前端无跨域问题 |
| Token 管理 | 登录后 token 存入 localStorage，axios 拦截器自动加 `Authorization: Bearer xxx` |
| 401 处理 | token 失效自动清空并跳转登录页 |
| 路由守卫 | 未登录访问任何页面强制跳 `/login` |
| 验证码 | 登录页直接渲染后端返回的 base64 图片，点图片刷新 |

## 页面与接口对照表

> 若某个接口返回 404，打开后端对应 Controller，核对方法上的 `@GetMapping/@PostMapping` 实际路径，微调 `src/api/*.js` 即可（方法名与参数一般不变）。

| 页面 | 文件 | 接口前缀 |
|---|---|---|
| 登录 | `src/views/Login.vue` | `/auth` |
| 数据统计 | `src/views/Dashboard.vue` | `/stats` |
| 工单管理 | `src/views/workorder/WorkOrderList.vue` | `/workorder` |
| 设备管理 | `src/views/device/DeviceList.vue` | `/device` |
| 设备分类 | `src/views/device/DeviceCategory.vue` | `/device/category` |
| 用户管理 | `src/views/system/UserManage.vue` | `/system/user` |
| 角色管理 | `src/views/system/RoleManage.vue` | `/system/role` |
| 菜单管理 | `src/views/system/MenuManage.vue` | `/system/menu` |
| 操作日志 | `src/views/system/OperationLog.vue` | `/system/log` |

## 工单状态流转按钮（与后端状态机对应）

| 当前状态 | 可用操作 |
|---|---|
| 0 待提交 | 提交 |
| 1 待接单 | 接单 / 指派 / 驳回 |
| 2 处理中 | 处理 / 驳回 |
| 3 待复核 | 验收（通过→已完成，不通过→驳回）/ 驳回 |
| 4 已完成 | 关闭 |
| 6 已驳回 | 重新打开 |

## 常见问题

1. **登录页验证码不显示**：后端 `/auth/captcha` 未通（查 Redis 是否启动、后端日志）。
2. **请求 404**：见上表提示，核对 Controller 实际路径。
3. **请求 401**：token 过期（120 分钟），重新登录。
4. **ECharts 空白**：统计接口返回空数据属正常，造几条工单数据后再看。

## 生产构建（答辩部署可选）

```bash
npm run build
# 产物在 dist/，可放到 Nginx 并将 /api 反向代理到后端 8080
```
