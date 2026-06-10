export const ALLOWED_OAUTH_DOMAINS = [
  'github.com',
  'accounts.google.com',
]

export function isSafeOAuthUrl(url) {
  try {
    const parsed = new URL(url)
    if (parsed.protocol !== 'https:') return false
    return ALLOWED_OAUTH_DOMAINS.some(domain => parsed.hostname === domain)
  } catch {
    return false
  }
}
