export function hasSpecifiedUsers(value) {
  return String(value || '')
    .split(/[,\s]+/)
    .map(username => username.trim())
    .some(Boolean)
}
