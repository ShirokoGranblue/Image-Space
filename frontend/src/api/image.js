import api from './index'

export function uploadImage(formData) {
  return api.post('/image/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getImageList(params) {
  return api.get('/image/list', { params })
}

export function getImageDetail(id) {
  return api.get(`/image/${id}`)
}

export function deleteImage(id) {
  return api.delete(`/image/${id}`)
}

export function updateImage(id, data) {
  return api.put(`/image/${id}`, data)
}

export function downloadImage(id) {
  return `/api/image/download/${id}`
}

export function getImageSquare(params) {
  return api.get('/image/square', { params })
}
