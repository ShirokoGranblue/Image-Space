import { describe, expect, it } from 'vitest'
import { hasSpecifiedUsers } from '../utils/visibility'

describe('visibility utils', () => {
  it('detects whether a SPECIFIED visibility username list has users', () => {
    expect(hasSpecifiedUsers('alice')).toBe(true)
    expect(hasSpecifiedUsers('alice bob,carol')).toBe(true)
    expect(hasSpecifiedUsers(' ,  ')).toBe(false)
    expect(hasSpecifiedUsers('')).toBe(false)
    expect(hasSpecifiedUsers(null)).toBe(false)
  })
})
