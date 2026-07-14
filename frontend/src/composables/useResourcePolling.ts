import { computed, onBeforeUnmount, onMounted, ref, toValue, watch } from 'vue'
import type {
  PollingResource,
  ResourceAccessUrlResponse,
  ResourcePollingOptions,
  ResourceStatusResponse,
} from '../types/resource'
import {
  isSafeAccessUrlReplacement,
  resourceNeedsAccessRefresh,
} from '../utils/resourceAccess'

export const POLLING_INTERVALS = {
  processing: 4000,
  detail: 15000,
  review: 8000,
} as const

export function useResourcePolling(options: ResourcePollingOptions) {
  const timer = ref<number | null>(null)
  const isChecking = ref(false)

  const enabled = computed(() => {
    return options.enabled === undefined ? true : Boolean(toValue(options.enabled))
  })
  const resourceKey = computed(() => {
    const current = currentResource()
    if (!current) return ''
    return [
      current.id ?? current.uuid ?? '',
      current.url ?? '',
      current.version ?? '',
      current.visibility ?? '',
      current.status ?? '',
      current.deleted ? 'deleted' : '',
    ].join('|')
  })

  onMounted(() => {
    document.addEventListener('visibilitychange', handleVisibilityChange)
    start()
  })

  onBeforeUnmount(() => {
    stop()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  })

  watch(enabled, (active) => {
    if (active) start()
    else stop()
  })

  watch(resourceKey, (key) => {
    if (!key) stop()
    else if (enabled.value) start()
  })

  function start(forceImmediate = false) {
    if (timer.value || !canPoll()) return
    if (forceImmediate || options.immediate !== false) {
      void checkNow()
    }
    timer.value = window.setInterval(() => {
      void checkNow()
    }, intervalMs())
  }

  function stop() {
    if (timer.value) {
      window.clearInterval(timer.value)
      timer.value = null
    }
  }

  async function checkNow(): Promise<boolean> {
    if (isChecking.value || !canPoll()) return false
    const current = currentResource()
    if (!current) return false

    isChecking.value = true
    try {
      const status = await options.getStatus(current)
      if (!status) return false
      options.onStatusChange?.(status)

      if (status.deleted) {
        options.onDeleted?.(status)
        stop()
        return true
      }

      if (resourceNeedsAccessRefresh(
        current,
        status,
        Math.floor(Date.now() / 1000),
        options.expireThresholdSeconds,
      )) {
        await refreshAccessUrl(current, status)
      }
      return true
    } catch (error) {
      console.warn('[resource-polling] resource check failed', error)
      options.onError?.(error)
      return false
    } finally {
      isChecking.value = false
    }
  }

  async function refreshAccessUrl(current: PollingResource, status: ResourceStatusResponse) {
    if (!options.getAccessUrl) return
    const access = await options.getAccessUrl(status, current)
    if (!access?.url) return
    if (!isSafeAccessUrlReplacement(current.url, access.url, status.visibility ?? access.visibility)) {
      console.warn('[resource-polling] ignored unsafe private access URL replacement')
      return
    }
    options.onAccessUrl?.(access, status)
  }

  function handleVisibilityChange() {
    if (document.hidden) {
      stop()
    } else if (enabled.value) {
      start(true)
    }
  }

  function canPoll() {
    return enabled.value && !document.hidden && !!currentResource()
  }

  function currentResource(): PollingResource | null | undefined {
    return toValue(options.resource)
  }

  function intervalMs() {
    const value = Number(toValue(options.intervalMs))
    return Number.isFinite(value) && value > 0 ? value : POLLING_INTERVALS.detail
  }

  return {
    isPolling: computed(() => !!timer.value),
    isChecking,
    start,
    stop,
    checkNow,
  }
}
