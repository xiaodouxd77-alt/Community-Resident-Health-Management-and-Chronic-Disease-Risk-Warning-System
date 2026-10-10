import { defineStore } from 'pinia'
import { login as loginApi, getCurrentUser } from '@/api/auth'
import { ElMessage } from 'element-plus'
import router from '@/router'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    role: (state) => state.userInfo?.role || '',
    isResident: (state) => state.userInfo?.role === 'RESIDENT',
    isDoctor: (state) => state.userInfo?.role === 'DOCTOR',
    isAdmin: (state) => state.userInfo?.role === 'ADMIN'
  },

  actions: {
    async login(loginForm) {
      const res = await loginApi(loginForm)
      this.token = res.data.token
      this.userInfo = res.data.user
      localStorage.setItem('token', this.token)
      ElMessage.success('登录成功')
      router.push('/')
    },

    async getUserInfo() {
      try {
        const res = await getCurrentUser()
        this.userInfo = res.data
      } catch (error) {
        this.logout()
      }
    },

    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      router.push('/login')
    }
  }
})
