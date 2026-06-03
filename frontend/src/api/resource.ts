import api from './index'
import type { ResourceAccessUrlResponse, ResourceStatusResponse } from '../types/resource'
import {
  normalizeAccessExpire,
  normalizeResourceVersion,
  parseAccessUrlMetadata,
  selectImageAccessUrl,
} from '../utils/resourceAccess'

export function requireResourceUuid(value: unknown): string {
  const uuid = typeof value === 'object' && value !== null
    ? (value as { uuid?: unknown }).uuid
    : value
  if (typeof uuid !== 'string' || !uuid.trim() || /^\d+$/.test(uuid.trim())) {
    throw new Error('Image UUID is required')
  }
  return uuid.trim()
}

export async function getImageResourceStatus(uuid: unknown) {
  const imageUuid = requireResourceUuid(uuid)
  const res = await api.get(`/image/${imageUuid}`)
  return {
    ...res,
    data: normalizeImageStatus(res.data),
  }
}

export async function refreshImageAccessUrl(uuid: unknown) {
  const imageUuid = requireResourceUuid(uuid)
  const res = await api.get(`/image/${imageUuid}`)
  return {
    ...res,
    data: normalizeImageAccessUrl(res.data),
  }
}

export async function getUserMediaResourceStatus(userUuid: unknown, kind: 'avatar' | 'background') {
  const uuid = requireUserUuid(userUuid)
  const res = await api.get(`/user/profile/${uuid}`)
  return {
    ...res,
    data: normalizeUserMediaStatus(res.data, kind),
  }
}

export async function refreshUserMediaAccessUrl(userUuid: unknown, kind: 'avatar' | 'background') {
  const uuid = requireUserUuid(userUuid)
  const res = await api.get(`/user/profile/${uuid}`)
  return {
    ...res,
    data: normalizeUserMediaAccessUrl(res.data, kind),
  }
}

export function normalizeImageStatus(image: unknown): ResourceStatusResponse {
  const record = objectRecord(image)
  return {
    id: primitive(record.id),
    uuid: stringValue(record.uuid),
    visibility: stringValue(record.visibility) || null,
    version: normalizeResourceVersion(record),
    status: stringValue(record.status) || 'READY',
    deleted: Boolean(record.deleted),
    updatedAt: stringValue(record.updatedAt) || stringValue(record.updateTime) || stringValue(record.uploadTime) || null,
    url: selectImageAccessUrl(record),
    source: image,
  }
}

export function normalizeImageAccessUrl(image: unknown): ResourceAccessUrlResponse {
  const record = objectRecord(image)
  const url = selectImageAccessUrl(record)
  const metadata = parseAccessUrlMetadata(url)
  const version = normalizeResourceVersion(record) ?? metadata.version
  return {
    url,
    expire: normalizeAccessExpire({ url }),
    expires: metadata.expire,
    version,
    visibility: stringValue(record.visibility) || null,
    status: stringValue(record.status) || 'READY',
    source: image,
  }
}

function normalizeUserMediaStatus(user: unknown, kind: 'avatar' | 'background'): ResourceStatusResponse {
  const url = userMediaUrl(user, kind)
  const metadata = parseAccessUrlMetadata(url)
  const record = objectRecord(user)
  return {
    id: `${stringValue(record.uuid) || primitive(record.id) || 'user'}:${kind}`,
    uuid: stringValue(record.uuid),
    visibility: 'PUBLIC',
    version: metadata.version,
    status: 'READY',
    deleted: Boolean(record.deleted),
    updatedAt: stringValue(record.updatedAt) || stringValue(record.updateTime) || stringValue(record.createTime) || null,
    url,
    source: user,
  }
}

function normalizeUserMediaAccessUrl(user: unknown, kind: 'avatar' | 'background'): ResourceAccessUrlResponse {
  const url = userMediaUrl(user, kind)
  const metadata = parseAccessUrlMetadata(url)
  return {
    url,
    expire: metadata.expire,
    expires: metadata.expire,
    version: metadata.version,
    visibility: 'PUBLIC',
    status: 'READY',
    source: user,
  }
}

function userMediaUrl(user: unknown, kind: 'avatar' | 'background'): string {
  const record = objectRecord(user)
  return kind === 'avatar'
    ? stringValue(record.avatarUrl) || stringValue(record.avatar)
    : stringValue(record.backgroundUrl) || stringValue(record.background)
}

function requireUserUuid(value: unknown): string {
  const uuid = typeof value === 'object' && value !== null
    ? (value as { uuid?: unknown }).uuid
    : value
  if (typeof uuid !== 'string' || !uuid.trim()) {
    throw new Error('User UUID is required')
  }
  return uuid.trim()
}

function objectRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' ? value as Record<string, unknown> : {}
}

function stringValue(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function primitive(value: unknown): number | string | undefined {
  return typeof value === 'string' || typeof value === 'number' ? value : undefined
}
