import { defineStore } from 'pinia'
// 用户状态：token / 用户id / 昵称 / 角色，持久化到 sessionStorage（每个标签页独立，支持多角色同时测试）
export const useUserStore = defineStore('user', {
  state: () => ({
    token: sessionStorage.getItem('woms_token') || '',
    // ★ 当前登录用户 id：必须保持【字符串】！
    userId: sessionStorage.getItem('woms_user_id') || '',
    nickname: sessionStorage.getItem('woms_nickname') || '',
    roles: JSON.parse(sessionStorage.getItem('woms_roles') || '[]')
  }),
  actions: {
    setLogin(data) {
      this.token = data.token || ''
      // ★ 兼容后端各种返回结构；统一转字符串，禁止 Number()
      this.userId = String(
        data.userId ?? data.id ?? data.user?.id ?? data.userInfo?.id ??
        data.data?.userId ?? data.data?.id ?? data.data?.user?.id ?? ''
      )
      this.nickname = data.nickname || ''
      this.roles = data.roles || []
      sessionStorage.setItem('woms_token', this.token)
      sessionStorage.setItem('woms_user_id', String(this.userId))
      sessionStorage.setItem('woms_nickname', this.nickname)
      sessionStorage.setItem('woms_roles', JSON.stringify(this.roles))
    },
    logout() {
      this.token = ''
      this.userId = ''
      this.nickname = ''
      this.roles = []
      sessionStorage.removeItem('woms_token')
      sessionStorage.removeItem('woms_user_id')
      sessionStorage.removeItem('woms_nickname')
      sessionStorage.removeItem('woms_roles')
    }
  }
})
