import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo } from '../api/user'
import { getToken, setToken as saveToken, removeToken } from '../utils/token'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userInfo = ref(null)

  function setToken(val) {
    token.value = val
    saveToken(val)
  }

  function clearToken() {
    token.value = ''
    removeToken()
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
