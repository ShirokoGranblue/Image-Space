const TOKEN_KEY = 'satoken'

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(val) {
  sessionStorage.setItem(TOKEN_KEY, val)
}

export function removeToken() {
  sessionStorage.removeItem(TOKEN_KEY)
}
