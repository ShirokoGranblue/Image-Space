import api from './index'

export function getComments(imageId) {
  return api.get(`/comment/list/${imageId}`)
}

export function addComment(data) {
  return api.post('/comment', data)
}

export function deleteComment(id) {
  return api.delete(`/comment/${id}`)
}

export function uploadCommentImage(formData) {
  return api.post('/comment/upload-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function likeComment(id) {
  return api.post(`/comment/${id}/like`)
}

export function unlikeComment(id) {
  return api.delete(`/comment/${id}/like`)
}
