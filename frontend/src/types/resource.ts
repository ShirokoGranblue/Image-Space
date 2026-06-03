import type { MaybeRefOrGetter } from 'vue'

export type ResourceVisibility = 'PUBLIC' | 'PRIVATE' | 'SPECIFIED' | string
export type ResourceStatus = 'PROCESSING' | 'READY' | 'FAILED' | string
export type ResourceVersion = number | string

export interface ResourceStatusResponse {
  id?: number | string
  uuid?: string
  visibility?: ResourceVisibility | null
  version?: ResourceVersion | null
  status?: ResourceStatus | null
  deleted?: boolean
  updatedAt?: string | null
  url?: string | null
  source?: unknown
}

export interface ResourceAccessUrlResponse {
  url: string
  expire?: number | null
  expires?: number | null
  version?: ResourceVersion | null
  visibility?: ResourceVisibility | null
  status?: ResourceStatus | null
  source?: unknown
}

export interface PollingResource {
  id?: number | string
  uuid?: string
  url?: string | null
  visibility?: ResourceVisibility | null
  version?: ResourceVersion | null
  status?: ResourceStatus | null
  deleted?: boolean
}

export interface ResourcePollingOptions {
  resource: MaybeRefOrGetter<PollingResource | null | undefined>
  intervalMs?: MaybeRefOrGetter<number | null | undefined>
  enabled?: MaybeRefOrGetter<boolean>
  immediate?: boolean
  expireThresholdSeconds?: number
  getStatus: (resource: PollingResource) => Promise<ResourceStatusResponse | null | undefined>
  getAccessUrl?: (
    status: ResourceStatusResponse,
    resource: PollingResource
  ) => Promise<ResourceAccessUrlResponse | null | undefined>
  onStatusChange?: (status: ResourceStatusResponse) => void
  onAccessUrl?: (access: ResourceAccessUrlResponse, status: ResourceStatusResponse) => void
  onDeleted?: (status: ResourceStatusResponse) => void
  onError?: (error: unknown) => void
}

export interface AccessUrlMetadata {
  url: string
  auth: string | null
  expire: number | null
  version: ResourceVersion | null
  isPrivate: boolean
  hasAuth: boolean
  hasExpire: boolean
}
