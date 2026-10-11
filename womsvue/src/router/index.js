import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layout/Layout.vue'),
    // ★ 默认页按角色：员工→我的工单、运维→我的接单、管理→数据统计
    redirect: () => {
      const userStore = useUserStore()
      const roles = userStore.roles || []
      if (roles.includes('employee')) return '/workorder/my'
      if (roles.includes('operator')) return '/workorder/mine'
      return '/dashboard'
    },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '数据统计' }
      },
      {
        path: 'workorder',
        name: 'WorkOrder',
        component: () => import('../views/workorder/WorkOrderList.vue'),
        meta: { title: '工单管理' }
      },
      {
        path: '/workorder/mine',
        name: 'MyWorkOrder',
        component: () => import('../views/workorder/MyWorkOrder.vue'),
        meta: { title: '我的接单' }
      },
      {
        path: '/workorder/my',
        name: 'MyWorkOrders',
        component: () => import('../views/workorder/MyWorkOrders.vue'),
        meta: { title: '我的工单' }
      },
      {
        path: '/workorder/monitor',
        name: 'WorkOrderMonitor',
        component: () => import('../views/workorder/WorkOrderMonitor.vue'),
        meta: { title: '工单监控' }
      },
      {
        path: 'device',
        name: 'Device',
        component: () => import('../views/device/DeviceList.vue'),
        meta: { title: '设备管理' }
      },
      {
        path: 'device/category',
        name: 'DeviceCategory',
        component: () => import('../views/device/DeviceCategory.vue'),
        meta: { title: '设备分类' }
      },
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('../views/profile.vue'),
        meta: { title: '个人用户' }
      },
      {
        path: 'system/user',
        name: 'UserManage',
        component: () => import('../views/system/UserManage.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/role',
        name: 'RoleManage',
        component: () => import('../views/system/RoleManage.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'system/menu',
        name: 'MenuManage',
        component: () => import('../views/system/MenuManage.vue'),
        meta: { title: '菜单管理' }
      },
      {
        path: 'system/log',
        name: 'OperationLog',
        component: () => import('../views/system/OperationLog.vue'),
        meta: { title: '操作日志' }
      },
      {
        path: 'system/devicelog',
        name: 'DeviceLog',
        component: () => import('../views/system/DeviceLog.vue'),
        meta: { title: '设备日志' }
      }
    ]
  },
  // ★ 未匹配路径：按角色回默认页
  {
    path: '/:pathMatch(.*)*',
    redirect: () => {
      const userStore = useUserStore()
      const roles = userStore.roles || []
      if (roles.includes('employee')) return '/workorder/my'
      if (roles.includes('operator')) return '/workorder/mine'
      return '/dashboard'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳登录页；数据统计仅管理角色可访问
router.beforeEach((to, from) => {
  const userStore = useUserStore()
  document.title = to.meta.title ? `${to.meta.title} - WOMS 工单管理系统` : 'WOMS 工单管理系统'
  if (to.path === '/login') return true
  if (!userStore.token) return '/login'
  // ★ 数据统计：仅 super_admin / manager 可见（防止手动输 URL 绕过菜单）
  if (to.path === '/dashboard') {
    const roles = userStore.roles || []
    const isAdminRole = roles.includes('super_admin') || roles.includes('manager')
    if (!isAdminRole) {
      return roles.includes('employee') ? '/workorder/my' : roles.includes('operator') ? '/workorder/mine' : '/profile'
    }
  }
  return true
})

export default router
