import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock the API module before importing the store
vi.mock('../api/user', () => ({
  getUserInfo: vi.fn(() => Promise.resolve({
    code: 200,
    data: { id: 1, username: 'testuser', displayName: 'Test User' }
  })),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
}))

import { useUserStore } from '../store/user'
import { getUserInfo } from '../api/user'

function setupStore() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return useUserStore()
}

describe('userStore', () => {
  beforeEach(() => {
    localStorage.clear()
    // Reset mocks
    vi.clearAllMocks()
  })

  describe('Initial state', () => {
    it('has empty token when localStorage is empty', () => {
      const store = setupStore()
      expect(store.token).toBe('')
    })

    it('has null userInfo initially', () => {
      const store = setupStore()
      expect(store.userInfo).toBeNull()
    })

    it('reads token from localStorage on creation', () => {
      localStorage.setItem('satoken', 'existing-token')
      const store = setupStore()
      expect(store.token).toBe('existing-token')
    })
  })

  describe('setToken', () => {
    it('sets token ref and stores in localStorage', () => {
      const store = setupStore()
      store.setToken('new-token')
      expect(store.token).toBe('new-token')
      expect(localStorage.getItem('satoken')).toBe('new-token')
    })
  })

  describe('clearToken', () => {
    it('clears token, localStorage, and userInfo', () => {
      const store = setupStore()
      store.setToken('some-token')
      store.userInfo = { id: 1, username: 'x' }

      store.clearToken()

      expect(store.token).toBe('')
      expect(localStorage.getItem('satoken')).toBeNull()
      expect(store.userInfo).toBeNull()
    })
  })

  describe('fetchUserInfo', () => {
    it('fetches and sets userInfo from API', async () => {
      const store = setupStore()
      store.setToken('valid-token')

      await store.fetchUserInfo()

      expect(store.userInfo).toEqual({
        id: 1,
        username: 'testuser',
        displayName: 'Test User'
      })
    })

    it('sets userInfo to null when API fails', async () => {
      vi.mocked(getUserInfo).mockRejectedValueOnce(new Error('Network error'))
      const store = setupStore()
      store.token = 'valid-token'

      await store.fetchUserInfo()

      expect(store.userInfo).toBeNull()
    })
  })
})
