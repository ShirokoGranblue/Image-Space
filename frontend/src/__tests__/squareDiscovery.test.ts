import { describe, expect, it } from 'vitest'
import { collectPageCreators, collectPageTags, selectFeaturedImage } from '../utils/squareDiscovery'

describe('square discovery data', () => {
  it('keeps every unique tag on the current page', () => {
    const tags = Array.from({ length: 16 }, (_, index) => `tag-${index + 1}`)
    const images = [
      { tags: tags.map(tag => `#${tag}`).join('') },
      { tags: '#tag-1#tag-16' }
    ]

    expect(collectPageTags(images)).toEqual(tags)
  })

  it('deduplicates one account by UUID and aggregates its page likes', () => {
    const creators = collectPageCreators([
      { userUuid: 'user-1', username: 'creator', displayName: 'Creator', likeCount: 2 },
      { userUuid: 'user-1', username: 'creator', displayName: 'Creator', likeCount: 3 }
    ])

    expect(creators).toHaveLength(1)
    expect(creators[0]).toMatchObject({ key: 'uuid:user-1', uuidOrId: 'user-1', username: 'creator', likeCount: 5 })
  })

  it('keeps same-name accounts separate by UUID', () => {
    const creators = collectPageCreators([
      { userUuid: 'user-1', username: 'one', displayName: 'Creator', likeCount: 1 },
      { userUuid: 'user-2', username: 'two', displayName: 'Creator', likeCount: 2 }
    ])

    expect(creators).toHaveLength(2)
    expect(creators.map(creator => creator.key)).toEqual(['uuid:user-2', 'uuid:user-1'])
    expect(creators.map(creator => creator.username)).toEqual(['two', 'one'])
  })

  it('recommends by likes first and comments second without changing the input order', () => {
    const images = [
      { uuid: 'many-comments', likeCount: 4, commentCount: 99 },
      { uuid: 'most-liked', likeCount: 5, commentCount: 0 },
      { uuid: 'likes-tie-low-comments', likeCount: 5, commentCount: 2 },
      { uuid: 'likes-tie-high-comments', likeCount: 5, commentCount: 8 }
    ]

    expect(selectFeaturedImage(images)?.uuid).toBe('likes-tie-high-comments')
    expect(images.map(image => image.uuid)).toEqual([
      'many-comments',
      'most-liked',
      'likes-tie-low-comments',
      'likes-tie-high-comments'
    ])
  })

  it('uses upload time and then stable identity to resolve complete ties', () => {
    const newer = selectFeaturedImage([
      { uuid: 'older', likeCount: 1, commentCount: 1, uploadTime: '2026-07-15T10:00:00' },
      { uuid: 'newer', likeCount: 1, commentCount: 1, uploadTime: '2026-07-16T10:00:00' }
    ])
    const stable = selectFeaturedImage([
      { uuid: 'b', likeCount: null, commentCount: undefined },
      { uuid: 'a' }
    ])

    expect(newer?.uuid).toBe('newer')
    expect(stable?.uuid).toBe('a')
  })
})
