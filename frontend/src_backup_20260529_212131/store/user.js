import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo } from '../api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('satoken') || '')
  const userInfo = ref(null)

  function setToken(val) {
    token.value = val
    localStorage.setItem('satoken', val)
  }

  function clearToken() {
    token.value = ''
    localStorage.removeItem('satoken')
    userInfo.value = null
  }

  async function fetchUserInfo() {
    try {
      const res = await getUserInfo()
      userInfo.value = res.data
    } catch {
      userInfo.value = null
    }
  }

  return { token, userInfo, setToken, clearToken, fetchUserInfo }
})
