import { describe, it, expect, beforeEach } from 'vitest'
import { formatSize, formatTime } from '../utils/format'
import { getToken, setToken, removeToken } from '../utils/token'
import { isSafeOAuthUrl, ALLOWED_OAUTH_DOMAINS } from '../utils/oauth'
import { prepareRegisterPayload } from '../utils/auth'

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

describe('token utility', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })

  it('getToken returns empty string when no token exists', () => {
    expect(getToken()).toBe('')
  })

  it('setToken stores token in sessionStorage', () => {
    setToken('test-token')
    expect(sessionStorage.getItem('satoken')).toBe('test-token')
    expect(getToken()).toBe('test-token')
  })

  it('removeToken clears token from sessionStorage', () => {
    setToken('test-token')
    removeToken()
    expect(sessionStorage.getItem('satoken')).toBeNull()
    expect(getToken()).toBe('')
  })
})

describe('oauth utility', () => {
  it('has github.com and accounts.google.com in allowed domains', () => {
    expect(ALLOWED_OAUTH_DOMAINS).toContain('github.com')
    expect(ALLOWED_OAUTH_DOMAINS).toContain('accounts.google.com')
  })

  it('accepts valid GitHub OAuth URL', () => {
    expect(isSafeOAuthUrl('https://github.com/login/oauth/authorize?client_id=xxx')).toBe(true)
  })

  it('accepts valid Google OAuth URL', () => {
    expect(isSafeOAuthUrl('https://accounts.google.com/o/oauth2/v2/auth?client_id=xxx')).toBe(true)
  })

  it('rejects non-https URLs', () => {
    expect(isSafeOAuthUrl('http://github.com/login/oauth')).toBe(false)
    expect(isSafeOAuthUrl('ftp://github.com/login/oauth')).toBe(false)
  })

  it('rejects URLs with non-allowed domains', () => {
    expect(isSafeOAuthUrl('https://evil.com/login')).toBe(false)
    expect(isSafeOAuthUrl('https://github.com.evil.com/login')).toBe(false)
  })

  it('rejects invalid URLs', () => {
    expect(isSafeOAuthUrl('not-a-url')).toBe(false)
    expect(isSafeOAuthUrl('')).toBe(false)
  })
})

describe('auth utility', () => {
  it('prepareRegisterPayload removes confirmPassword field', () => {
    const form = {
      username: 'testuser',
      password: 'password123',
      confirmPassword: 'password123',
      email: 'test@example.com',
      phone: '1234567890'
    }
    const payload = prepareRegisterPayload(form)
    expect(payload).not.toHaveProperty('confirmPassword')
    expect(payload).toEqual({
      username: 'testuser',
      password: 'password123',
      email: 'test@example.com',
      phone: '1234567890'
    })
  })

  it('prepareRegisterPayload preserves all other fields', () => {
    const form = {
      username: 'testuser',
      password: 'password123',
      confirmPassword: 'different'
    }
    const payload = prepareRegisterPayload(form)
    expect(payload.username).toBe('testuser')
    expect(payload.password).toBe('password123')
    expect(payload).not.toHaveProperty('confirmPassword')
  })
})
