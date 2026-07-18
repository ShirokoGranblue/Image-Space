type UserIdentitySource = {
  displayName?: unknown
  username?: unknown
}

function textValue(value: unknown): string {
  return typeof value === 'string' ? value.trim() : ''
}

export function formatUserHandle(username: unknown): string {
  const value = textValue(username)
  if (!value) return ''
  return value.startsWith('@') ? value : `@${value}`
}

export function getUserDisplayName(user: UserIdentitySource | null | undefined, fallback = '用户'): string {
  return textValue(user?.displayName) || textValue(user?.username) || fallback
}

export function formatUserIdentityText(user: UserIdentitySource | null | undefined, fallback = '用户'): string {
  const name = getUserDisplayName(user, fallback)
  const handle = formatUserHandle(user?.username)
  return handle ? `${name} ${handle}` : name
}
