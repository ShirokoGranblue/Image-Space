import type {
  AccessUrlMetadata,
  PollingResource,
  ResourceStatusResponse,
  ResourceVersion,
} from '../types/resource'

export const ACCESS_URL_REFRESH_THRESHOLD_SECONDS = 30

export function parseAccessUrlMetadata(url: string | null | undefined): AccessUrlMetadata {
  const normalizedUrl = typeof url === 'string' ? url : ''
  if (!normalizedUrl) {
    return emptyMetadata('')
  }

  try {
    const parsed = new URL(normalizedUrl, globalThis.location?.origin || 'http://localhost')
    const auth = parsed.searchParams.get('auth')
    const expire = parseExpire(parsed.searchParams.get('expire') ?? parsed.searchParams.get('expires'))
    const version = parseVersion(parsed.searchParams.get('v'))
    const isPrivate = parsed.pathname.includes('/private/') || !!auth
    return {
      url: normalizedUrl,
      auth,
      expire,
      version,
      isPrivate,
      hasAuth: !!auth,
      hasExpire: expire != null,
    }
  } catch {
    return emptyMetadata(normalizedUrl)
  }
}

export function isAccessUrlExpiring(
  url: string | null | undefined,
  nowSeconds = Math.floor(Date.now() / 1000),
  thresholdSeconds = ACCESS_URL_REFRESH_THRESHOLD_SECONDS,
): boolean {
  const metadata = parseAccessUrlMetadata(url)
  if (metadata.expire == null) return false
  return nowSeconds >= metadata.expire - thresholdSeconds
}

export function isSafeAccessUrlReplacement(
  currentUrl: string | null | undefined,
  nextUrl: string | null | undefined,
  nextVisibility?: string | null,
): boolean {
  if (!nextUrl) return false
  const current = parseAccessUrlMetadata(currentUrl)
  const next = parseAccessUrlMetadata(nextUrl)
  const visibility = String(nextVisibility || '').toUpperCase()
  if (!current.isPrivate) return true
  if (visibility === 'PUBLIC' && !next.isPrivate) return true
  return next.hasAuth && next.hasExpire
}

export function normalizeResourceVersion(resource: unknown): ResourceVersion | null {
  if (!resource || typeof resource !== 'object') return null
  const record = resource as Record<string, unknown>
  const value = record.version ?? record.mediaVersion
  if (value == null || value === '') return null
  if (typeof value === 'number') return value
  if (typeof value === 'string') return parseVersion(value)
  return String(value)
}

export function selectImageAccessUrl(image: unknown): string {
  if (!image || typeof image !== 'object') return ''
  const record = image as Record<string, unknown>
  const visibility = String(record.visibility || '').toUpperCase()
  const publicUrl = stringValue(record.publicUrl)
  const privateUrl = stringValue(record.privateUrl)
  const imageUrl = stringValue(record.imageUrl)
  const directUrl = stringValue(record.url)
  const uuid = stringValue(record.uuid)

  if (visibility === 'PUBLIC' && publicUrl) return publicUrl
  if (visibility && visibility !== 'PUBLIC' && privateUrl) return privateUrl
  if (publicUrl) return publicUrl
  if (privateUrl) return privateUrl
  if (directUrl) return directUrl
  if (imageUrl) return imageUrl
  return uuid ? `/api/image/download/${uuid}` : ''
}

export function resourceNeedsAccessRefresh(
  current: PollingResource | null | undefined,
  status: ResourceStatusResponse | null | undefined,
  nowSeconds = Math.floor(Date.now() / 1000),
  thresholdSeconds = ACCESS_URL_REFRESH_THRESHOLD_SECONDS,
): boolean {
  if (!current || !status || status.deleted) return false
  if (isAccessUrlExpiring(current.url, nowSeconds, thresholdSeconds)) return true

  const currentVersion = normalizeComparable(current.version)
  const statusVersion = normalizeComparable(status.version)
  if (currentVersion != null && statusVersion != null && currentVersion !== statusVersion) return true

  const currentVisibility = normalizeComparable(current.visibility)
  const statusVisibility = normalizeComparable(status.visibility)
  if (currentVisibility && statusVisibility && currentVisibility !== statusVisibility) return true

  const currentStatus = normalizeComparable(current.status)
  const nextStatus = normalizeComparable(status.status)
  if (currentStatus === 'PROCESSING' && (nextStatus === 'READY' || nextStatus === 'FAILED')) return true
  if (currentStatus && nextStatus && currentStatus !== nextStatus) return true

  return false
}

export function normalizeAccessExpire(access: { expire?: unknown; expires?: unknown; url?: unknown }): number | null {
  const direct = parseExpire(access.expire ?? access.expires)
  if (direct != null) return direct
  return parseAccessUrlMetadata(stringValue(access.url)).expire
}

function emptyMetadata(url: string): AccessUrlMetadata {
  return {
    url,
    auth: null,
    expire: null,
    version: null,
    isPrivate: false,
    hasAuth: false,
    hasExpire: false,
  }
}

function parseExpire(value: unknown): number | null {
  if (value == null || value === '') return null
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : null
}

function parseVersion(value: unknown): ResourceVersion | null {
  if (value == null || value === '') return null
  if (typeof value === 'number') return Number.isFinite(value) ? value : null
  const stringified = String(value)
  const numeric = Number(stringified)
  return Number.isFinite(numeric) && stringified.trim() !== '' ? numeric : stringified
}

function stringValue(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function normalizeComparable(value: unknown): string | null {
  if (value == null || value === '') return null
  return String(value).toUpperCase()
}
