import api from './index'

export function login(data) {
  return api.post('/user/login', data)
}

export function register(data) {
  return api.post('/user/register', data)
}

export function logout() {
  return api.post('/user/logout')
}

export function getUserInfo() {
  return api.get('/user/info')
}

export function getUserProfile(id) {
  return api.get(`/user/profile/${id}`)
}

export function updateProfile(data) {
  return api.put('/user/profile', data)
}

export function uploadAvatar(formData) {
  return api.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadBackground(formData) {
  return api.post('/user/background', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function checkField(field, value, excludeId) {
  return api.get('/user/check-field', { params: { field, value, excludeId } })
}

export function sendCode(email) {
  return api.post('/user/send-code', { email })
}

export function loginByCode(data) {
  return api.post('/user/login-by-code', data)
}
