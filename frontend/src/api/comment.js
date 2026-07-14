import api from './index'
import { getToken } from '../utils/token'

function requireUuid(value) {
  const uuid = typeof value === 'object' && value !== null ? value?.uuid : value
  if (typeof uuid !== 'string') {
    throw new Error('UUID must be a string')
  }
  const trimmed = uuid.trim()
  if (!trimmed) {
    throw new Error('UUID is required')
  }
  return trimmed
}

function requireId(value) {
  if (value == null || value === '' || value === false) {
    throw new Error('ID is required')
  }
  return value
}

export async function getComments(imageUuid) {
  return api.get(`/comment/list/${requireUuid(imageUuid)}`)
}

export function addComment(data) {
  return api.post('/comment', data)
}

export async function deleteComment(id) {
  return api.delete(`/comment/${requireId(id)}`)
}

export function uploadCommentImage(formData) {
  return api.post('/comment/upload-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export async function fetchCommentImage(url) {
  const token = getToken()
  const headers = token ? { satoken: token } : {}
  const response = await fetch(url, { headers, credentials: 'same-origin' })
  if (!response.ok) {
    throw new Error(`评论图片加载失败 (${response.status})`)
  }
  return response.blob()
}

export async function likeComment(id) {
  return api.post(`/comment/${requireId(id)}/like`)
}

export async function unlikeComment(id) {
  return api.delete(`/comment/${requireId(id)}/like`)
}
