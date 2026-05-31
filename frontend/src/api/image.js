import api from './index'

export function uploadImage(formData) {
  return api.post('/image/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getImageList(params) {
  return api.get('/image/list', { params })
}

export function getImageDetail(uuid) {
  return api.get(`/image/${uuid}`)
}

export function deleteImage(uuid) {
  return api.delete(`/image/${uuid}`)
}

export function updateImage(uuid, data) {
  return api.put(`/image/${uuid}`, data)
}

export function downloadImage(uuid) {
  return `/api/image/download/${uuid}`
}

export function getImageSquare(params) {
  return api.get('/image/square', { params })
}

export function likeImage(uuid) {
  return api.post(`/image/${uuid}/like`)
}

export function unlikeImage(uuid) {
  return api.delete(`/image/${uuid}/like`)
}
