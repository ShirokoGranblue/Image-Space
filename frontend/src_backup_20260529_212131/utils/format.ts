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
