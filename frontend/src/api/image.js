import api from './index'

function requireImageUuid(value) {
  const uuid = typeof value === 'object' ? value?.uuid : value
  if (typeof uuid !== 'string' || !uuid.trim() || /^\d+$/.test(uuid.trim())) {
    throw new Error('Image UUID is required')
  }
  return uuid.trim()
}

export function uploadImage(formData) {
  return api.post('/image/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getImageList(params) {
  return api.get('/image/list', { params })
}

export function getUserPublicImages(userUuid, params) {
  if (typeof userUuid !== 'string' || !userUuid.trim()) {
    throw new Error('User UUID is required')
  }
  return api.get(`/image/user/${userUuid.trim()}`, { params })
}

export function getImageDetail(uuid) {
  return api.get(`/image/${requireImageUuid(uuid)}`)
}

export function deleteImage(uuid) {
  return api.delete(`/image/${requireImageUuid(uuid)}`)
}

export function updateImage(uuid, data) {
  return api.put(`/image/${requireImageUuid(uuid)}`, data)
}

export function downloadImage(uuid) {
  return `/api/image/download/${requireImageUuid(uuid)}`
}

export function downloadOriginalImage(uuid) {
  return downloadImage(uuid)
}

export function downloadImageAs(uuid, format) {
  const normalized = normalizeDownloadFormat(format)
  return `/api/image/download/${requireImageUuid(uuid)}?format=${normalized}`
}

export function getImageSquare(params) {
  return api.get('/image/square', { params })
}

export function likeImage(uuid) {
  return api.post(`/image/${requireImageUuid(uuid)}/like`)
}

export function unlikeImage(uuid) {
  return api.delete(`/image/${requireImageUuid(uuid)}/like`)
}

function normalizeDownloadFormat(format) {
  const normalized = String(format || '').trim().toLowerCase()
  const value = normalized === 'jpeg' ? 'jpg' : normalized
  if (!['jpg', 'png', 'gif'].includes(value)) {
    throw new Error('Unsupported image download format')
  }
  return value
}
