import { describe, expect, it, vi } from 'vitest'

import {
  isAccessUrlExpiring,
  isSafeAccessUrlReplacement,
  parseAccessUrlMetadata,
  resourceNeedsAccessRefresh,
} from '../utils/resourceAccess'

describe('resource access helpers', () => {
  it('parses auth, expires, expire, and version parameters without adding cache busters', () => {
    const expiresUrl = 'https://cdn.image-space.app/private/images/a.png?auth=abc&expires=1748939200&v=9'
    const expireUrl = 'https://cdn.image-space.app/private/images/a.png?auth=def&expire=1748939300&v=10'

    expect(parseAccessUrlMetadata(expiresUrl)).toMatchObject({
      url: expiresUrl,
      auth: 'abc',
      expire: 1748939200,
      version: 9,
      isPrivate: true,
      hasAuth: true,
      hasExpire: true,
    })
    expect(parseAccessUrlMetadata(expireUrl)).toMatchObject({
      auth: 'def',
      expire: 1748939300,
      version: 10,
    })
    expect(parseAccessUrlMetadata(expiresUrl).url).not.toContain('Date.now')
  })

  it('treats private URLs as expiring at expire minus the configured threshold', () => {
    const url = 'https://cdn.image-space.app/private/images/a.png?auth=abc&expires=1748939200'

    expect(isAccessUrlExpiring(url, 1748939169, 30)).toBe(false)
    expect(isAccessUrlExpiring(url, 1748939170, 30)).toBe(true)
  })

  it('requires refreshed private URLs to preserve auth and expire parameters', () => {
    const current = 'https://cdn.image-space.app/private/images/a.png?auth=old&expires=1748939200'
    const safe = 'https://cdn.image-space.app/private/images/a.png?auth=new&expires=1748939230'
    const unsafe = 'https://cdn.image-space.app/private/images/a.png?v=9'

    expect(isSafeAccessUrlReplacement(current, safe)).toBe(true)
    expect(isSafeAccessUrlReplacement(current, unsafe)).toBe(false)
    expect(isSafeAccessUrlReplacement('/api/image/download/uuid', unsafe)).toBe(true)
  })

  it('detects refresh triggers from expiry and resource state changes', () => {
    vi.setSystemTime(new Date('2026-06-03T12:00:00Z'))
    const now = Math.floor(Date.now() / 1000)
    const current = {
      id: 'image-uuid',
      url: `https://cdn.image-space.app/private/images/a.png?auth=abc&expires=${now + 20}`,
      version: 8,
      visibility: 'PRIVATE',
      status: 'PROCESSING',
    }

    expect(resourceNeedsAccessRefresh(current, { version: 8, visibility: 'PRIVATE', status: 'PROCESSING' })).toBe(true)
    expect(resourceNeedsAccessRefresh({ ...current, url: current.url.replace(String(now + 20), String(now + 120)) }, { version: 9, visibility: 'PRIVATE', status: 'PROCESSING' })).toBe(true)
    expect(resourceNeedsAccessRefresh({ ...current, url: current.url.replace(String(now + 20), String(now + 120)) }, { version: 8, visibility: 'PUBLIC', status: 'PROCESSING' })).toBe(true)
    expect(resourceNeedsAccessRefresh({ ...current, url: current.url.replace(String(now + 20), String(now + 120)) }, { version: 8, visibility: 'PRIVATE', status: 'READY' })).toBe(true)
  })
})
