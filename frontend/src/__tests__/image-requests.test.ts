import { describe, expect, it } from 'vitest'

import {
  DEFAULT_IMAGE_PAGE_SIZE,
  IMAGE_PAGE_SIZES,
  buildImageListParams,
  getImageDownloadUrl,
  normalizeImagePageSize,
} from '../utils/imageRequests'

describe('image request helpers', () => {
  it('uses backend download endpoint for image rendering', () => {
    expect(getImageDownloadUrl(9)).toBe('/api/image/download/9')
    expect(getImageDownloadUrl(null)).toBe('')
  })

  it('sorts personal images by name ascending', () => {
    expect(buildImageListParams({
      page: 1,
      limit: 12,
      keyword: '',
      categoryId: null,
      sortField: 'image_name',
      sortOrder: 'desc',
    })).toMatchObject({
      sortField: 'image_name',
      sortOrder: 'asc',
    })
  })

  it('keeps non-name personal image sorts descending', () => {
    expect(buildImageListParams({
      page: 1,
      limit: 12,
      keyword: '',
      categoryId: null,
      sortField: 'file_size',
      sortOrder: 'asc',
    })).toMatchObject({
      sortField: 'file_size',
      sortOrder: 'desc',
    })
  })

  it('supports 30, 50, and 100 image page sizes with 50 as fallback', () => {
    expect(IMAGE_PAGE_SIZES).toEqual([30, 50, 100])
    expect(DEFAULT_IMAGE_PAGE_SIZE).toBe(50)
    expect(normalizeImagePageSize(30)).toBe(30)
    expect(normalizeImagePageSize(50)).toBe(50)
    expect(normalizeImagePageSize(100)).toBe(100)
    expect(normalizeImagePageSize(12)).toBe(50)
  })
})
