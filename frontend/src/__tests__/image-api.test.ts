import { describe, expect, it, vi } from 'vitest'

vi.mock('../api/index', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

import api from '../api/index'
import { getImageSquare, updateImage } from '../api/image'

describe('image api', () => {
  it('sends update payload as JSON body', () => {
    const payload = { imageName: 'new name.jpg', tags: 'cat,blue', visibility: 'PUBLIC' }

    updateImage(7, payload)

    expect(api.put).toHaveBeenCalledWith('/image/7', payload)
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
})
