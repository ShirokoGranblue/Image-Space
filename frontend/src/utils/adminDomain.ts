const ADMIN_DOMAIN = 'admin.image-space.app'
const DEV_DOMAINS = new Set(['localhost', '127.0.0.1'])

export function isAllowedAdminDomain(
  hostname = window.location.hostname,
  isDev = import.meta.env.DEV,
): boolean {
  const normalized = normalizeHost(hostname)
  if (normalized === ADMIN_DOMAIN) {
    return true
  }
  return isDev && DEV_DOMAINS.has(normalized)
}

function normalizeHost(hostname: string): string {
  const first = String(hostname || '').split(',')[0].trim().toLowerCase()
  if (first.startsWith('[')) {
    const end = first.indexOf(']')
    return end >= 0 ? first.slice(1, end) : first
  }
  const portIndex = first.indexOf(':')
  return portIndex >= 0 ? first.slice(0, portIndex) : first
}
