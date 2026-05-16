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
