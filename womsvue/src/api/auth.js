import request from '../utils/request'
// 登录认证相关接口
export const getCaptcha = () => request.get('/auth/captcha')
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const logout = () => request.post('/auth/logout')
export const refreshToken = () => request.post('/auth/refresh')
