export const IMAGE_PAGE_SIZES = [30, 50, 100]
export const DEFAULT_IMAGE_PAGE_SIZE = 50

export {
  isAccessUrlExpiring,
  parseAccessUrlMetadata,
} from './resourceAccess'

export function getImageDisplayUrl(imageOrId) {
  if (!imageOrId) return ''
  if (typeof imageOrId === 'object') {
    if (imageOrId.thumbUrl) return imageOrId.thumbUrl
    if (imageOrId.mediumUrl) return imageOrId.mediumUrl
    const original = originalCompatibleUrl(imageOrId)
    if (original) return original
    return imageOrId.uuid ? `/api/image/download/${imageOrId.uuid}` : ''
  }
  return typeof imageOrId === 'string' && !/^\d+$/.test(imageOrId)
    ? `/api/image/download/${imageOrId}`
    : ''
}

export function getImagePreviewUrl(imageOrId) {
  if (!imageOrId) return ''
  if (typeof imageOrId === 'object') {
    if (imageOrId.mediumUrl) return imageOrId.mediumUrl
    const original = originalCompatibleUrl(imageOrId)
    if (original) return original
    return imageOrId.uuid ? `/api/image/download/${imageOrId.uuid}` : ''
  }
  return getImageDisplayUrl(imageOrId)
}

export function getOriginalDownloadUrl(imageOrId) {
  const uuid = imageUuid(imageOrId)
  return uuid ? `/api/image/download/${uuid}` : ''
}

export function getImageDownloadUrl(imageOrId) {
  if (!imageOrId) return ''
  if (typeof imageOrId === 'object') {
    const original = originalCompatibleUrl(imageOrId)
    if (original) return original
    return imageOrId.uuid ? `/api/image/download/${imageOrId.uuid}` : ''
  }
  return typeof imageOrId === 'string' && !/^\d+$/.test(imageOrId)
    ? `/api/image/download/${imageOrId}`
    : ''
}

export function getFallbackUrl(imageOrId) {
  if (!imageOrId) return ''
  if (typeof imageOrId === 'object') {
    return imageOrId.uuid ? `/api/image/download/${imageOrId.uuid}` : ''
  }
  return typeof imageOrId === 'string' && !/^\d+$/.test(imageOrId)
    ? `/api/image/download/${imageOrId}`
    : ''
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

function originalCompatibleUrl(image) {
  if (image.visibility === 'PUBLIC' && image.publicUrl) return image.publicUrl
  if (image.visibility !== 'PUBLIC' && image.privateUrl) return image.privateUrl
  if (image.imageUrl) return image.imageUrl
  if (image.originalUrl) return image.originalUrl
  if (image.publicUrl) return image.publicUrl
  if (image.privateUrl) return image.privateUrl
  return ''
}

function imageUuid(imageOrId) {
  if (!imageOrId) return ''
  const uuid = typeof imageOrId === 'object' ? imageOrId.uuid : imageOrId
  return typeof uuid === 'string' && uuid.trim() && !/^\d+$/.test(uuid.trim()) ? uuid.trim() : ''
}
