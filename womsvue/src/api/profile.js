import request from '../utils/request'

// 个人中心
export const getMyInfo = () => request.get('/system/user/me')
export const updateMyInfo = (data) => request.put('/system/user/me', data)
export const deleteMyAccount = () => request.delete('/system/user/me')

// 资金（仅运维/用户本人）
export const getFinanceOverview = () => request.get('/finance/overview')
export const getFinanceDaily = (params) => request.get('/finance/daily', { params })
export const getDepositOverview = () => request.get('/finance/deposit')
