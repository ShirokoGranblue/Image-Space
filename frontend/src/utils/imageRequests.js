export const IMAGE_PAGE_SIZES = [30, 50, 100]
export const DEFAULT_IMAGE_PAGE_SIZE = 50

export function getImageDownloadUrl(id) {
  return id ? `/api/image/download/${id}` : ''
}

export function buildImageListParams(query) {
  const sortField = query.sortField || 'upload_time'
  return {
    ...query,
    limit: normalizeImagePageSize(query.limit),
    sortField,
    sortOrder: sortField === 'image_name' ? 'asc' : 'desc'
  }
}

export function normalizeImagePageSize(size) {
  const numericSize = Number(size)
  return IMAGE_PAGE_SIZES.includes(numericSize) ? numericSize : DEFAULT_IMAGE_PAGE_SIZE
}
