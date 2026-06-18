import { describe, expect, it } from 'vitest'

import {
  DEFAULT_IMAGE_PAGE_SIZE,
  IMAGE_PAGE_SIZES,
  buildImageListParams,
  getImageDisplayUrl,
  getImageDownloadUrl,
  getImagePreviewUrl,
  getOriginalDownloadUrl,
  normalizeImagePageSize,
} from '../utils/imageRequests'

describe('image request helpers', () => {
  it('uses backend download endpoint for image rendering', () => {
    expect(getImageDownloadUrl('400a1e49-6990-489e-b4a8-35eb0a02d056')).toBe('/api/image/download/400a1e49-6990-489e-b4a8-35eb0a02d056')
    expect(getImageDownloadUrl(9)).toBe('')
    expect(getImageDownloadUrl(null)).toBe('')
  })

  it('prefers backend-provided versioned image urls when available', () => {
    expect(getImageDownloadUrl({
      id: 9,
      uuid: '400a1e49-6990-489e-b4a8-35eb0a02d056',
      imageUrl: '/api/image/download/400a1e49-6990-489e-b4a8-35eb0a02d056?v=b031160aee8f',
    })).toBe('/api/image/download/400a1e49-6990-489e-b4a8-35eb0a02d056?v=b031160aee8f')
  })

  it('prefers publicUrl for public image rendering', () => {
    expect(getImageDownloadUrl({
      uuid: '400a1e49-6990-489e-b4a8-35eb0a02d056',
      visibility: 'PUBLIC',
      publicUrl: 'https://cdn.image-space.app/public/images/a.png?v=2',
      imageUrl: '/api/image/download/400a1e49-6990-489e-b4a8-35eb0a02d056',
    })).toBe('https://cdn.image-space.app/public/images/a.png?v=2')
  })

  it('prefers privateUrl for private image rendering', () => {
    expect(getImageDownloadUrl({
      uuid: '400a1e49-6990-489e-b4a8-35eb0a02d056',
      visibility: 'PRIVATE',
      privateUrl: 'https://cdn.image-space.app/private/images/a.png?auth=abc&expires=1893456000',
      imageUrl: '/api/image/download/400a1e49-6990-489e-b4a8-35eb0a02d056',
    })).toBe('https://cdn.image-space.app/private/images/a.png?auth=abc&expires=1893456000')
  })

  it('uses thumb then medium then original-compatible URLs for card display', () => {
    const image = {
      uuid: '400a1e49-6990-489e-b4a8-35eb0a02d056',
      thumbUrl: 'https://cdn.image-space.app/public/images/a/thumb.jpg?v=2',
      mediumUrl: 'https://cdn.image-space.app/public/images/a/medium.jpg?v=2',
      originalUrl: 'https://cdn.image-space.app/public/images/a/original.png?v=2',
      imageUrl: 'https://cdn.image-space.app/public/images/a/original.png?v=2',
    }

    expect(getImageDisplayUrl(image)).toBe(image.thumbUrl)
    expect(getImageDisplayUrl({ ...image, thumbUrl: '' })).toBe(image.mediumUrl)
    expect(getImageDisplayUrl({ ...image, thumbUrl: '', mediumUrl: '' })).toBe(image.imageUrl)
  })

  it('uses medium before original-compatible URLs for detail preview', () => {
    const image = {
      uuid: '400a1e49-6990-489e-b4a8-35eb0a02d056',
      thumbUrl: 'https://cdn.image-space.app/public/images/a/thumb.jpg?v=2',
      mediumUrl: 'https://cdn.image-space.app/public/images/a/medium.jpg?v=2',
      originalUrl: 'https://cdn.image-space.app/public/images/a/original.png?v=2',
      imageUrl: 'https://cdn.image-space.app/public/images/a/original.png?v=2',
    }

    expect(getImagePreviewUrl(image)).toBe(image.mediumUrl)
    expect(getImagePreviewUrl({ ...image, mediumUrl: '' })).toBe(image.imageUrl)
  })

  it('keeps original download URL separate from display URLs', () => {
    const image = {
      uuid: '400a1e49-6990-489e-b4a8-35eb0a02d056',
      visibility: 'PUBLIC',
      thumbUrl: 'https://cdn.image-space.app/public/images/a/thumb.jpg?v=2',
      mediumUrl: 'https://cdn.image-space.app/public/images/a/medium.jpg?v=2',
      publicUrl: 'https://cdn.image-space.app/public/images/a/original.png?v=2',
      imageUrl: 'https://cdn.image-space.app/public/images/a/original.png?v=2',
    }

    expect(getImageDisplayUrl(image)).toBe(image.thumbUrl)
    expect(getImageDownloadUrl(image)).toBe(image.publicUrl)
    expect(getOriginalDownloadUrl(image)).toBe('/api/image/download/400a1e49-6990-489e-b4a8-35eb0a02d056')
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
