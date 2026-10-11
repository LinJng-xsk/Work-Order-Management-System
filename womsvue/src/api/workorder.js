import request from '../utils/request'

// ===== 工单 =====
export const getWorkOrderPage = (params) => request.get('/workorder/page', { params })

// ===== 工单监控 =====
export const getMonitorPage = (params) => request.get('/workorder/monitor/page', { params })


export const getWorkOrderDetail = (id) => request.get(`/workorder/${id}`)
export const getWorkOrderRecord = (id) => request.get(`/workorder/${id}/records`)
export const createWorkOrder = (data) => request.post('/workorder', data)
// ★ 编辑：后端是 PUT /workorder/{id}/edit，参数走 query
export const editWorkOrder = (id, data) => request.put(`/workorder/${id}/edit`, null, { params: data })
export const deleteWorkOrder = (id) => request.delete(`/workorder/${id}`)
export const scrapWorkOrder = (id) => request.put(`/workorder/${id}/scrap`)

// ===== 接单/指派/拒单/取消 =====
export const acceptWorkOrder = (id, data) => request.put(`/workorder/${id}/accept`, null, { params: data })
export const assignWorkOrder = (id, data) => request.put(`/workorder/${id}/assign`, null, { params: data })
export const rejectWorkOrder = (id, reason) => request.put(`/workorder/${id}/reject`, null, { params: { reason } })
export const cancelAssignWorkOrder = (id) => request.put(`/workorder/${id}/cancelAssign`)
export const getRejectedOperators = (id) => request.get(`/workorder/${id}/rejectedOperators`)

// ===== 处理/验收/关闭/重开 =====
export const submitReviewWorkOrder = (id, data) => request.put(`/workorder/${id}/submitReview`, data)
export const reviewWorkOrder = (id, data) => request.put(`/workorder/${id}/review`, data)
export const closeWorkOrder = (id) => request.put(`/workorder/${id}/close`)
export const reopenWorkOrder = (id) => request.put(`/workorder/${id}/reopen`)

// ===== 运维列表（含工作日）=====
export const getOperators = () => request.get('/workorder/operators')

// ===== 沟通 =====
export const getOrderMessages = (id) => request.get(`/workorder/${id}/messages`)
export const sendOrderMessage = (id, data) => request.post(`/workorder/${id}/messages`, data)



// ===== 运维排期（派遣/请求/接单时看占用）=====
export const getOperatorSchedule = (operatorId, start, end) =>
  request.get('/workorder/schedule', { params: { operatorId, start, end } })


// 当前登录运维的排期（接单弹窗用，后端识别当前用户）
export const getMySchedule = (start, end) => request.get('/workorder/schedule/mine', { params: { start, end } })
