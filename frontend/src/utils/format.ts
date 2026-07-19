export function formatSize(bytes: number | null | undefined): string {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

export function formatTime(time: string | null | undefined): string {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

export function formatRelativeTime(
  time: string | null | undefined,
  now: Date | string | number = new Date(),
): string {
  if (!time) return ''
  const timestamp = new Date(time).getTime()
  const currentTimestamp = now instanceof Date ? now.getTime() : new Date(now).getTime()
  if (!Number.isFinite(timestamp) || !Number.isFinite(currentTimestamp)) return ''

  const elapsed = Math.max(0, currentTimestamp - timestamp)
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (elapsed < minute) return '刚刚'
  if (elapsed < hour) return `${Math.floor(elapsed / minute)}分钟前`
  if (elapsed < day) return `${Math.floor(elapsed / hour)}小时前`
  return `${Math.floor(elapsed / day)}天前`
}
