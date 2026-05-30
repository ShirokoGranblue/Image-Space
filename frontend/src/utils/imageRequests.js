export const IMAGE_PAGE_SIZES = [30, 50, 100]
export const DEFAULT_IMAGE_PAGE_SIZE = 50

export function getImageDownloadUrl(imageOrId) {
  if (!imageOrId) return ''
  if (typeof imageOrId === 'object') {
    if (imageOrId.imageUrl) return imageOrId.imageUrl
    return imageOrId.id ? `/api/image/download/${imageOrId.id}` : ''
  }
  return `/api/image/download/${imageOrId}`
}

export function getFallbackUrl(imageOrId) {
  if (!imageOrId) return ''
  if (typeof imageOrId === 'object') {
    return imageOrId.id ? `/api/image/download/${imageOrId.id}` : ''
  }
  return `/api/image/download/${imageOrId}`
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
