# 工单管理系统
> 毕业设计项目：园区设备报修全流程工单平台

## 📖 项目简介
园区设备报修工单管理平台，支持4种角色，实现工单发布、流转、接单、运维处理全流程。包含13张数据库表，8种业务状态机。

## 🛠 技术栈
后端：SpringBoot 4.1.1 + Spring Security + JWT + MyBatis-Plus + MySQL
前端：Vue3 + Pinia + Element Plus + ECharts

## ✨ 功能模块
1. 用户登录、权限控制（4角色）
2. 工单创建、提交、状态流转
3. 运维人员接单、处理工单
4. 数据统计可视化（ECharts图表）
5. 日志记录、异常处理

## 📁 项目目录
- woms：SpringBoot后端代码
- woms-view：Vue3前端代码
- SQL：数据库初始化脚本

## 🚀 部署运行
### 后端
1. 修改application.yml数据库连接配置
2. 执行SQL目录下初始化脚本
3. 启动WomsApplication.java

### 前端
```bash
npm install
npm run dev
