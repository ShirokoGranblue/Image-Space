import type {
  PollingResource,
  ResourceAccessUrlResponse,
  ResourceStatusResponse,
} from '../types/resource'
import {
  normalizeResourceVersion,
  parseAccessUrlMetadata,
  selectImageAccessUrl,
} from './resourceAccess'

export function imageToPollingResource(image: Record<string, unknown> | null | undefined): PollingResource | null {
  if (!image?.uuid || image.deleted) return null
  return {
    id: stringValue(image.uuid),
    uuid: stringValue(image.uuid),
    url: selectImageAccessUrl(image),
    version: normalizeResourceVersion(image),
    visibility: stringValue(image.visibility) || null,
    status: stringValue(image.status) || 'READY',
    deleted: Boolean(image.deleted),
  }
}

export function applyImageStatus(target: Record<string, unknown>, status: ResourceStatusResponse) {
  if (!target || !status) return
  if (status.visibility) target.visibility = status.visibility
  if (status.version != null) {
    target.mediaVersion = status.version
    target.version = status.version
  }
  if (status.status) target.status = status.status
  if (status.deleted) target.deleted = true
}

export function applyImageAccessUrl(
  target: Record<string, unknown>,
  access: ResourceAccessUrlResponse,
  status?: ResourceStatusResponse,
) {
  if (!target || !access?.url) return
  const source = isRecord(access.source) ? access.source : null
  if (source) {
    Object.assign(target, source)
  }
  const visibility = stringValue(access.visibility) || stringValue(status?.visibility) || stringValue(target.visibility)
  if (visibility) target.visibility = visibility
  if (access.version != null) {
    target.mediaVersion = access.version
    target.version = access.version
  }
  setImageAccessUrl(target, access.url, visibility)
}

export function userMediaToPollingResource(
  user: Record<string, unknown> | null | undefined,
  kind: 'avatar' | 'background',
): PollingResource | null {
  if (!user?.uuid || user.deleted) return null
  const url = kind === 'avatar'
    ? stringValue(user.avatarUrl) || stringValue(user.avatar)
    : stringValue(user.backgroundUrl) || stringValue(user.background)
  if (!url || isLocalPreviewUrl(url)) return null
  return {
    id: `${stringValue(user.uuid)}:${kind}`,
    uuid: stringValue(user.uuid),
    url,
    version: parseAccessUrlMetadata(url).version,
    visibility: 'PUBLIC',
    status: 'READY',
    deleted: false,
  }
}

export function isLocalPreviewUrl(url: string) {
  return /^(data:|blob:|#|rgb|linear-gradient)/i.test(url)
}

function setImageAccessUrl(target: Record<string, unknown>, url: string, visibility: string) {
  target.imageUrl = url
  if (visibility.toUpperCase() === 'PUBLIC') {
    target.publicUrl = url
  } else {
    target.privateUrl = url
  }
}

function stringValue(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object'
}
