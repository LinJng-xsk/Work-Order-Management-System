import request from '../utils/request'

// 统计接口（前缀 /stats，与 StatisticsController 对齐）
// 月度工单总量（默认近 12 个月，可传 startTime 指定起始时间）
export const getMonthlyStats = (params) => request.get('/stats/order/monthly', { params })


// 月度派单明细（month 格式：2026-09）
export const getMonthlyDetail = (params) => request.get('/stats/order/monthlyDetail', { params })



// 设备故障率
export const getDeviceFaultRate = () => request.get('/stats/device/faultRate')

// 运维人员工作量排行
export const getWorkerWorkload = () => request.get('/stats/worker/workload')

// 故障设备 TOP 排名
export const getFaultTop = () => request.get('/stats/device/top')



