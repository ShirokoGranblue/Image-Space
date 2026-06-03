import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../api/index', () => ({
  default: {
    get: vi.fn(),
  },
}))

import api from '../api/index'
import { getImageResourceStatus, refreshImageAccessUrl } from '../api/resource'

describe('resource api wrappers', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('reuses the existing image detail endpoint for image status', async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: {
        id: 123,
        uuid: '400a1e49-6990-489e-b4a8-35eb0a02d056',
        visibility: 'PRIVATE',
        mediaVersion: 9,
        uploadTime: '2026-06-03T12:00:00',
      },
    })

    const res = await getImageResourceStatus('400a1e49-6990-489e-b4a8-35eb0a02d056')

    expect(api.get).toHaveBeenCalledWith('/image/400a1e49-6990-489e-b4a8-35eb0a02d056')
    expect(res.data).toMatchObject({
      id: 123,
      uuid: '400a1e49-6990-489e-b4a8-35eb0a02d056',
      visibility: 'PRIVATE',
      version: 9,
      status: 'READY',
      deleted: false,
      updatedAt: '2026-06-03T12:00:00',
    })
  })

  it('normalizes backend image URLs into access-url responses', async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: {
        uuid: '400a1e49-6990-489e-b4a8-35eb0a02d056',
        visibility: 'PRIVATE',
        mediaVersion: 9,
        privateUrl: 'https://cdn.image-space.app/private/images/a.png?auth=abc&expires=1748939200&v=9',
        imageUrl: '/api/image/download/400a1e49-6990-489e-b4a8-35eb0a02d056',
      },
    })

    const res = await refreshImageAccessUrl('400a1e49-6990-489e-b4a8-35eb0a02d056')

    expect(api.get).toHaveBeenCalledWith('/image/400a1e49-6990-489e-b4a8-35eb0a02d056')
    expect(res.data).toMatchObject({
      url: 'https://cdn.image-space.app/private/images/a.png?auth=abc&expires=1748939200&v=9',
      expire: 1748939200,
      version: 9,
      visibility: 'PRIVATE',
    })
  })

  it('rejects numeric image ids', async () => {
    await expect(getImageResourceStatus(123)).rejects.toThrow('Image UUID is required')
    await expect(refreshImageAccessUrl('123')).rejects.toThrow('Image UUID is required')
    expect(api.get).not.toHaveBeenCalled()
  })
})
