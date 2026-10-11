# WOMS 工单管理系统
> 基于 SpringBoot 4.1 + Vue3 + mysql8.0 的企业工单管理系统

## 📖 项目简介
WOMS（Work Order Management System）工单管理系统，面向企业内部业务人员，实现工单的提交、派单、处理、完结与全流程跟踪。系统采用前后端分离架构，后端使用SpringBoot开发，前端基于Vue3 + Element Plus实现，支持用户权限控制、操作日志留存、文件上传等功能，方便管理人员统一维护工单业务数据。

## 🛠 技术栈
### 后端
- JDK：JDK 17
- 框架：SpringBoot 4.1
- ORM框架：MyBatis-Plus
- 数据库：MySQL 8.0
- 缓存：Redis
- 身份认证：JWT令牌
- 构建工具：Gradle
- 工具库：Hutool

### 前端
- Vue 3
- UI组件库：Element Plus
- 网络请求：Axios
- 路由：Vue Router
- 状态管理：Pinia

## ✨ 功能模块
1. **用户模块**
    - 用户登录、退出登录，JWT身份校验
    - 用户信息管理、密码修改
    - 角色权限控制，不同角色菜单可见性隔离

2. **工单核心模块**
    - 工单创建，填写工单描述、优先级、关联业务信息
    - 工单分配：管理员将工单指派给处理人
    - 工单流转：待处理、处理中、已完成、已驳回等状态切换
    - 工单查询与筛选：按时间、状态、处理人检索工单

3. **日志模块**
    - 系统操作日志自动记录，记录操作人、操作时间、操作接口

4. **文件上传模块**
    - 工单附件上传，支持图片、文档上传预览

5. **系统基础管理**
    - 用户管理、角色管理
    - 基础字典维护

## 📁 项目结构
```
Work-Order-Management-System
├── sql                  # MySQL 建表脚本
├── woms                 # SpringBoot 后端项目
│   ├── src/main/java    # Java 后端源码
│   ├── src/main/resources # yml、mapper 配置文件
│   ├── build.gradle
│   └── settings.gradle
├── womsvue              # Vue3 前端项目
└── README.md            # 项目说明文档
```

## 🚀 部署与运行
### 后端运行
1. 准备环境：JDK17，MySQL8.0，Redis
2. 创建MySQL数据库，执行项目内SQL脚本，导入数据表
3. 修改`application.yml`，配置数据库连接、Redis连接信息
4. Gradle加载依赖，启动SpringBoot主程序
5. 后端默认端口：8080

### 前端运行
1. 进入`womsvue`前端目录
2. 安装项目依赖
```bash
npm install
```
3.启动开发环境：
```bash
npm run dev
```
4. 前端访问地址：[[http://localhost:5173](http://localhost:5173)](http://localhost:5173)

## 💡 项目亮点
1. 前后端分离架构，代码解耦，便于维护
2. 使用 JWT 无状态登录，配合角色实现接口权限拦截
3. 全局统一异常处理器，统一返回结果封装
4. MyBatis-Plus 简化 CRUD 开发，减少重复 SQL
5. Redis 缓存热点数据，提升查询性能
6. AOP 切面自动记录操作日志，低侵入式实现日志功能

### ©️ 版权声明

本项目为个人---LJ-XSK---设计作品，仅供在线查阅学习，禁止直接复制源码用于其他设计。
