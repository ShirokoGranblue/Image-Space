import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, ref } from 'vue'

import { POLLING_INTERVALS, useResourcePolling } from '../composables/useResourcePolling'

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

function setDocumentHidden(value: boolean) {
  Object.defineProperty(document, 'hidden', {
    configurable: true,
    value,
  })
}

describe('useResourcePolling', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-06-03T12:00:00Z'))
    setDocumentHidden(false)
  })

  afterEach(() => {
    vi.useRealTimers()
    setDocumentHidden(false)
  })

  it('exports configured polling intervals for known resource contexts', () => {
    expect(POLLING_INTERVALS.processing).toBe(4000)
    expect(POLLING_INTERVALS.detail).toBe(15000)
    expect(POLLING_INTERVALS.review).toBe(8000)
  })

  it('uses one timer, clears it on unmount, and avoids concurrent polling requests', async () => {
    const pending = deferred<{ version: number; visibility: string; status: string }>()
    const getStatus = vi.fn(() => pending.promise)
    const getAccessUrl = vi.fn()
    const clearSpy = vi.spyOn(window, 'clearInterval')

    const wrapper = mount(defineComponent({
      setup() {
        const resource = ref({
          id: 'image-uuid',
          url: 'https://cdn.image-space.app/private/images/a.png?auth=abc&expires=1893456000',
          version: 1,
          visibility: 'PRIVATE',
          status: 'READY',
        })
        useResourcePolling({
          resource,
          intervalMs: 1000,
          immediate: false,
          getStatus,
          getAccessUrl,
        })
        return () => null
      },
    }))

    vi.advanceTimersByTime(1000)
    expect(getStatus).toHaveBeenCalledTimes(1)
    vi.advanceTimersByTime(3000)
    expect(getStatus).toHaveBeenCalledTimes(1)

    pending.resolve({ version: 1, visibility: 'PRIVATE', status: 'READY' })
    await flushPromises()
    vi.advanceTimersByTime(1000)
    expect(getStatus).toHaveBeenCalledTimes(2)

    wrapper.unmount()
    expect(clearSpy).toHaveBeenCalled()
  })

  it('pauses while the page is hidden and checks immediately when visible again', async () => {
    const getStatus = vi.fn().mockResolvedValue({ version: 1, visibility: 'PRIVATE', status: 'READY' })

    mount(defineComponent({
      setup() {
        useResourcePolling({
          resource: ref({
            id: 'image-uuid',
            url: 'https://cdn.image-space.app/private/images/a.png?auth=abc&expires=1893456000',
            version: 1,
            visibility: 'PRIVATE',
            status: 'READY',
          }),
          intervalMs: 1000,
          immediate: false,
          getStatus,
        })
        return () => null
      },
    }))

    setDocumentHidden(true)
    document.dispatchEvent(new Event('visibilitychange'))
    vi.advanceTimersByTime(5000)
    expect(getStatus).toHaveBeenCalledTimes(0)

    setDocumentHidden(false)
    document.dispatchEvent(new Event('visibilitychange'))
    await flushPromises()
    expect(getStatus).toHaveBeenCalledTimes(1)
  })

  it('refreshes access URL when the current URL is near expiry', async () => {
    const now = Math.floor(Date.now() / 1000)
    const onAccessUrl = vi.fn()
    const getStatus = vi.fn().mockResolvedValue({ version: 1, visibility: 'PRIVATE', status: 'READY' })
    const getAccessUrl = vi.fn().mockResolvedValue({
      url: `https://cdn.image-space.app/private/images/a.png?auth=new&expires=${now + 60}`,
      expire: now + 60,
      version: 1,
    })

    mount(defineComponent({
      setup() {
        useResourcePolling({
          resource: ref({
            id: 'image-uuid',
            url: `https://cdn.image-space.app/private/images/a.png?auth=old&expires=${now + 20}`,
            version: 1,
            visibility: 'PRIVATE',
            status: 'READY',
          }),
          intervalMs: 1000,
          immediate: true,
          getStatus,
          getAccessUrl,
          onAccessUrl,
        })
        return () => null
      },
    }))

    await flushPromises()
    expect(getAccessUrl).toHaveBeenCalledTimes(1)
    expect(onAccessUrl).toHaveBeenCalledWith(
      expect.objectContaining({
        url: expect.stringContaining('auth=new'),
        expire: now + 60,
      }),
      expect.objectContaining({ status: 'READY' }),
    )
  })

  it('stops polling deleted resources and reports the deleted state', async () => {
    const onDeleted = vi.fn()
    const getStatus = vi.fn().mockResolvedValue({ version: 1, visibility: 'PRIVATE', status: 'READY', deleted: true })
    const getAccessUrl = vi.fn()

    mount(defineComponent({
      setup() {
        useResourcePolling({
          resource: ref({
            id: 'image-uuid',
            url: 'https://cdn.image-space.app/private/images/a.png?auth=abc&expires=1893456000',
            version: 1,
            visibility: 'PRIVATE',
            status: 'READY',
          }),
          intervalMs: 1000,
          immediate: true,
          getStatus,
          getAccessUrl,
          onDeleted,
        })
        return () => null
      },
    }))

    await flushPromises()
    expect(onDeleted).toHaveBeenCalledWith(expect.objectContaining({ deleted: true }))
    expect(getAccessUrl).not.toHaveBeenCalled()
  })
})
