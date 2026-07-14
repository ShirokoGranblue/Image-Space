import { describe, expect, it } from 'vitest'
import { isAllowedAdminDomain } from '../utils/adminDomain'

describe('admin domain utility', () => {
  it('allows only the admin host in production', () => {
    expect(isAllowedAdminDomain('admin.image-space.app', false)).toBe(true)
    expect(isAllowedAdminDomain('image-space.app', false)).toBe(false)
    expect(isAllowedAdminDomain('localhost', false)).toBe(false)
    expect(isAllowedAdminDomain('127.0.0.1', false)).toBe(false)
  })

  it('allows localhost and 127.0.0.1 only with explicit opt-in', () => {
    expect(isAllowedAdminDomain('admin.image-space.app', true)).toBe(true)
    expect(isAllowedAdminDomain('localhost', true)).toBe(true)
    expect(isAllowedAdminDomain('127.0.0.1', true)).toBe(true)
    expect(isAllowedAdminDomain('image-space.app', true)).toBe(false)
  })

  it('normalizes ports and case', () => {
    expect(isAllowedAdminDomain('ADMIN.IMAGE-SPACE.APP:443', false)).toBe(true)
    expect(isAllowedAdminDomain('localhost:3000', true)).toBe(true)
  })
})
