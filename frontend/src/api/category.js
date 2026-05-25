import api from './index'

export function getCategoryList() {
  return api.get('/category/list')
}

export function createCategory(categoryName) {
  return api.post('/category', { categoryName })
}

export function deleteCategory(id) {
  return api.delete(`/category/${id}`, { params: { id } })
}

export function updateCategory(id, categoryName) {
  return api.put(`/category/${id}`, { categoryName })
}
