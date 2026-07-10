const FILTERED = '[FILTERED]'
const SENSITIVE_KEYS = [
  'password',
  'oldPassword',
  'newPassword',
  'token',
  'accessToken',
  'refreshToken',
  'authorization',
  'cookie',
  'phone',
  'email',
]

export function formatJsonForDisplay(text: unknown): string {
  if (text === null || text === undefined || text === '') return ''
  if (typeof text !== 'string') {
    return JSON.stringify(maskValue('', text), null, 2)
  }
  try {
    return JSON.stringify(maskValue('', JSON.parse(text)), null, 2)
  } catch {
    return text
  }
}

export function maskSensitiveText(text: unknown): string {
  if (text === null || text === undefined || text === '') return ''
  if (typeof text !== 'string') {
    return JSON.stringify(maskValue('', text), null, 2)
  }
  try {
    return JSON.stringify(maskValue('', JSON.parse(text)), null, 2)
  } catch {
    return maskKeyValueText(text)
  }
}

function maskValue(key: string, value: unknown): unknown {
  if (isSensitiveKey(key)) return FILTERED
  if (Array.isArray(value)) return value.map((item) => maskValue(key, item))
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>).map(([childKey, childValue]) => [
        childKey,
        maskValue(childKey, childValue),
      ]),
    )
  }
  return value
}

function maskKeyValueText(text: string): string {
  return text.replace(
    /(password|oldPassword|newPassword|token|accessToken|refreshToken|authorization|cookie|phone|email)(\s*[=:]\s*)([^&\s,}\]]+)/gi,
    (_match, key, separator) => `${key}${separator}${FILTERED}`,
  )
}

function isSensitiveKey(key: string): boolean {
  const lower = String(key || '').toLowerCase()
  return SENSITIVE_KEYS.some((item) => lower.includes(item.toLowerCase()))
}
