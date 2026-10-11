import request from '../utils/request'

// 设备相关接口（前缀 /device，与 DeviceController 对齐）
export const getDevicePage = (params) => request.get('/device/page', { params })

export const saveDevice = (data) => request.post('/device', data)

export const updateDevice = (data) => request.put('/device', data)

export const deleteDevice = (id) => request.delete(`/device/${id}`)

export const getDeviceWorkOrders = (id) => request.get(`/device/${id}/workorders`)

// 设备分类（前缀 /device/category，与 DeviceCategoryController 对齐）
export const getCategoryList = () => request.get('/device/category/tree')

export const saveCategory = (data) => request.post('/device/category', data)

export const updateCategory = (data) => request.put('/device/category', data)

export const deleteCategory = (id) => request.delete(`/device/category/${id}`)
