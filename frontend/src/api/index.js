import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { getToken, removeToken } from '../utils/token'

const api = axios.create({
  baseURL: '/api',
  timeout: 60000
})

api.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['satoken'] = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 401) {
      removeToken()
      const path = router.currentRoute?.value?.path
      if (path !== '/login' && path !== '/register') {
        ElMessage.error('登录已过期，请重新登录')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '未授权'))
    }
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络错误'
    if (status === 401) {
      removeToken()
      const path = router.currentRoute?.value?.path
      if (path !== '/login' && path !== '/register') {
        router.push('/login')
      }
    }
    ElMessage.error(status === 401 ? '登录已过期，请重新登录' : message)
    return Promise.reject(new Error(message))
  }
)

export default api
