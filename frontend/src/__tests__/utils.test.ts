import { describe, it, expect } from 'vitest'
import { formatSize, formatTime } from '../utils/format'

describe('formatSize', () => {
  it('returns "0 B" for falsy or zero values', () => {
    expect(formatSize(0)).toBe('0 B')
    expect(formatSize(null as any)).toBe('0 B')
    expect(formatSize(undefined as any)).toBe('0 B')
  })

  it('formats bytes (< 1024)', () => {
    expect(formatSize(1)).toBe('1 B')
    expect(formatSize(512)).toBe('512 B')
    expect(formatSize(1023)).toBe('1023 B')
  })

  it('formats KB (1024 to 1024*1024-1)', () => {
    expect(formatSize(1024)).toBe('1.0 KB')
    expect(formatSize(1536)).toBe('1.5 KB')
    expect(formatSize(1048575)).toBe('1024.0 KB')
  })

  it('formats MB (>= 1024*1024)', () => {
    expect(formatSize(1048576)).toBe('1.00 MB')
    expect(formatSize(1572864)).toBe('1.50 MB')
    expect(formatSize(104857600)).toBe('100.00 MB')
  })
})

describe('formatTime', () => {
  it('returns empty string for falsy input', () => {
    expect(formatTime('')).toBe('')
    expect(formatTime(null as any)).toBe('')
    expect(formatTime(undefined as any)).toBe('')
  })

  it('formats ISO datetime to "YYYY-MM-DD HH:mm"', () => {
    const result = formatTime('2026-05-17T14:30:00')
    expect(result).toBe('2026-05-17 14:30')
    expect(result.length).toBe(16)
  })

  it('handles already-formatted datetime strings', () => {
    expect(formatTime('2026-05-17 14:30:00')).toBe('2026-05-17 14:30')
  })
})
