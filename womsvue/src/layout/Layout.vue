<template>
  <el-container class="layout">
    <!-- 侧边栏 -->
    <el-aside width="210px" class="aside">
      <div class="logo">
        <el-icon><Tools /></el-icon>
        <span>WOMS 工单系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1d2939"
        text-color="#9ca3af"
        active-text-color="#ffffff"
      >
      <!-- ★数据统计：仅管理员可见 -->
        <el-menu-item v-if="isAdmin" index="/dashboard">
         <el-icon><DataAnalysis /></el-icon>
         <span>数据统计</span>
        </el-menu-item>
        <el-menu-item index="/workorder">
          <el-icon><Tickets /></el-icon>
          <span>工单管理</span>
        </el-menu-item>

      <!-- ★ 我的工单：仅用户（员工）可见，存放自己报修的工单 -->
        <el-menu-item v-if="isEmployee" index="/workorder/my">
          <el-icon><Document /></el-icon>
          <span>我的工单</span>
        </el-menu-item>

        <!-- ★ 我的接单：仅运维可见 -->
        <el-menu-item v-if="isOperator" index="/workorder/mine">
          <el-icon><Calendar /></el-icon>
          <span>我的接单</span>
        </el-menu-item>
        <!-- ★ 工单监控：仅管理角色（超管+管理员）可见 -->
        <el-menu-item v-if="isAdmin" index="/workorder/monitor">
          <el-icon><AlarmClock /></el-icon>
          <span>工单监控</span>
        </el-menu-item>
        <el-menu-item index="/device">
          <el-icon><Monitor /></el-icon>
          <span>设备管理</span>
        </el-menu-item>
        <el-menu-item index="/device/category">
          <el-icon><Collection /></el-icon>
          <span>设备分类</span>
        </el-menu-item>

        <el-menu-item index="/profile">
          <el-icon><Avatar /></el-icon>
          <span>个人用户</span>
        </el-menu-item>

        <!-- ★ 系统管理：仅超管可见 -->
        <el-sub-menu v-if="isSuperAdmin" index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
          <el-menu-item index="/system/menu">菜单管理</el-menu-item>
          <el-menu-item index="/system/devicelog">设备日志</el-menu-item>
          <el-menu-item index="/system/log">操作日志</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
        <div class="header-right">
          <el-tag v-if="roles.length" type="success" size="small" effect="plain">
            {{ roleText }}
          </el-tag>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ userStore.nickname || '管理员' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <!-- 主内容 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  DataAnalysis, Tickets, Calendar, AlarmClock, Monitor, Collection,
  Setting, User, ArrowDown, Tools, Avatar
} from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'
import { logout as logoutApi } from '../api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const roles = computed(() => userStore.roles || [])
const isSuperAdmin = computed(() => roles.value.includes('super_admin'))
const isAdmin = computed(() => roles.value.includes('super_admin') || roles.value.includes('manager'))
const isOperator = computed(() => roles.value.includes('operator'))
const isEmployee = computed(() => roles.value.includes('employee'))

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')
const roleText = computed(() => {
  const map = { super_admin: '超级管理员', manager: '管理员', operator: '运维人员', employee: '用户' }
  return roles.value.map((r) => map[r] || r).join(' / ')
})
const handleCommand = async (command) => {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    try {
      await logoutApi()
    } catch (e) {
      // 后端注销失败也照常清本地
    }
    userStore.logout()
    router.push('/login')
  }
}
</script>
<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background-color: #1d2939;
  overflow-x: hidden;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  background-color: #16202e;
}
.aside :deep(.el-menu) {
  border-right: none;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #374151;
  outline: none;
}
.main {
  background-color: #f3f4f6;
  padding: 16px;
}
</style>
