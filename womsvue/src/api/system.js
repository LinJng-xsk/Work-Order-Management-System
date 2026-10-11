import request from '../utils/request'

// 用户管理（前缀 /system/user，与 SysUserController 对齐）
export const getUserPage = (params) => request.get('/system/user/page', { params })

export const saveUser = (data) => request.post('/system/user', data)

export const updateUser = (data) => request.put('/system/user', data)

export const deleteUser = (id) => request.delete(`/system/user/${id}`)

export const resetUserPwd = (id, data) => request.put(`/system/user/${id}/resetPwd`, data)

// 角色管理（前缀 /system/role，与 SysRoleController 对齐）
export const getRolePage = (params) => request.get('/system/role/page', { params })

export const saveRole = (data) => request.post('/system/role', data)

export const updateRole = (data) => request.put('/system/role', data)

export const deleteRole = (id) => request.delete(`/system/role/${id}`)

// 菜单管理（前缀 /system/menu，与 SysMenuController 对齐）
export const getMenuTree = () => request.get('/system/menu/tree')

export const saveMenu = (data) => request.post('/system/menu', data)

export const updateMenu = (data) => request.put('/system/menu', data)

export const deleteMenu = (id) => request.delete(`/system/menu/${id}`)

// 操作日志（前缀 /system/log）
export const getOperationLogPage = (params) => request.get('/system/log/page', { params })


// 设备日志（前缀 /system/devicelog）
export const getDeviceLogPage = (params) => request.get('/system/devicelog/page', { params })
