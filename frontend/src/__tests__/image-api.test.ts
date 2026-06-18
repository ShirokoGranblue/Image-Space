import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../api/index', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

import api from '../api/index'
import { deleteImage, downloadImage, downloadImageAs, getImageSquare, getUserPublicImages, likeImage, unlikeImage, updateImage } from '../api/image'

describe('image api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('sends update payload as JSON body', () => {
    const payload = { imageName: 'new name.jpg', tags: 'cat,blue', visibility: 'PUBLIC' }
    const uuid = '400a1e49-6990-489e-b4a8-35eb0a02d056'

    updateImage(uuid, payload)

    expect(api.put).toHaveBeenCalledWith(`/image/${uuid}`, payload)
  })

  it('rejects numeric image ids for mutation routes', () => {
    expect(() => updateImage(7, {})).toThrow('Image UUID is required')
    expect(() => deleteImage('7')).toThrow('Image UUID is required')
    expect(api.put).not.toHaveBeenCalled()
    expect(api.delete).not.toHaveBeenCalled()
  })

  it('passes square filters as query params object', () => {
    const params = {
      page: 2,
      limit: 12,
      keyword: 'sunset',
      tags: 'sea,blue',
      sortMode: 'random',
      sortField: 'upload_time',
      sortOrder: 'desc',
      randomSeed: 'abc123',
    }

    getImageSquare(params)

    expect(api.get).toHaveBeenCalledWith('/image/square', { params })
  })

  it('loads public images for a viewed profile user', () => {
    const params = { page: 1, limit: 30, sortField: 'upload_time', sortOrder: 'desc' }

    getUserPublicImages('user-uuid', params)

    expect(api.get).toHaveBeenCalledWith('/image/user/user-uuid', { params })
  })

  it('calls image like endpoints', () => {
    const uuid = '400a1e49-6990-489e-b4a8-35eb0a02d056'

    likeImage(uuid)
    unlikeImage(uuid)

    expect(api.post).toHaveBeenCalledWith(`/image/${uuid}/like`)
    expect(api.delete).toHaveBeenCalledWith(`/image/${uuid}/like`)
  })

  it('builds original and format download URLs from image UUIDs', () => {
    const uuid = '400a1e49-6990-489e-b4a8-35eb0a02d056'

    expect(downloadImage(uuid)).toBe(`/api/image/download/${uuid}`)
    expect(downloadImageAs(uuid, 'jpeg')).toBe(`/api/image/download/${uuid}?format=jpg`)
    expect(downloadImageAs({ uuid }, 'png')).toBe(`/api/image/download/${uuid}?format=png`)
    expect(() => downloadImageAs(uuid, 'bmp')).toThrow('Unsupported image download format')
  })
})
