import { normalizeImagePageSize } from './imageRequests'

export const SQUARE_SESSION_KEY = 'picture-square-query'

const ALLOWED_SORT_FIELDS = new Set(['upload_time', 'file_size', 'image_name'])

export function createRandomSeed() {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export function buildSquareParams(query) {
  const hasExplicitSort = ALLOWED_SORT_FIELDS.has(query.sortField)
  return {
    page: query.page,
    limit: normalizeImagePageSize(query.limit),
    keyword: String(query.keyword || '').trim(),
    tags: Array.isArray(query.tags) ? query.tags.join('#') : '',
    sortMode: hasExplicitSort ? 'latest' : 'random',
    sortField: hasExplicitSort ? query.sortField : 'upload_time',
    sortOrder: query.sortField === 'image_name' ? 'asc' : 'desc',
    randomSeed: hasExplicitSort ? undefined : query.randomSeed
  }
}

export function loadSquareSession(storage = getSessionStorage()) {
  if (!storage) {
    return {}
  }
  try {
    const raw = storage.getItem(SQUARE_SESSION_KEY)
    if (!raw) {
      return {}
    }
    const parsed = JSON.parse(raw)
    return {
      page: normalizePage(parsed.page),
      limit: normalizeImagePageSize(parsed.limit),
      keyword: typeof parsed.keyword === 'string' ? parsed.keyword : '',
      tags: Array.isArray(parsed.tags) ? parsed.tags.filter(Boolean) : [],
      sortField: ''
    }
  } catch {
    storage.removeItem(SQUARE_SESSION_KEY)
    return {}
  }
}

export function saveSquareSession(query, storage = getSessionStorage()) {
  if (!storage) {
    return
  }
  const sessionQuery = {
    page: normalizePage(query.page),
    limit: normalizeImagePageSize(query.limit),
    keyword: String(query.keyword || ''),
    tags: Array.isArray(query.tags) ? query.tags : [],
    sortField: ''
  }
  storage.setItem(SQUARE_SESSION_KEY, JSON.stringify(sessionQuery))
}

function normalizePage(page) {
  const value = Number(page)
  return Number.isInteger(value) && value > 0 ? value : 1
}

function getSessionStorage() {
  if (typeof window === 'undefined') {
    return null
  }
  return window.sessionStorage
}
