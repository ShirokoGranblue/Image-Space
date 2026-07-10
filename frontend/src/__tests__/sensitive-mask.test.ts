import { describe, expect, it } from 'vitest'
import { formatJsonForDisplay, maskSensitiveText } from '../utils/sensitiveMask'

describe('sensitive mask utility', () => {
  it('masks sensitive json fields', () => {
    const result = maskSensitiveText('{"password":"secret","email":"a@example.com","safe":"ok"}')

    expect(result).toContain('"password": "[FILTERED]"')
    expect(result).toContain('"email": "[FILTERED]"')
    expect(result).toContain('"safe": "ok"')
    expect(result).not.toContain('secret')
    expect(result).not.toContain('a@example.com')
  })

  it('masks non-json key value text', () => {
    const result = maskSensitiveText('token=abc123&path=/image/1')

    expect(result).toContain('token=[FILTERED]')
    expect(result).not.toContain('abc123')
  })

  it('formats json before display', () => {
    expect(formatJsonForDisplay('{"a":1}')).toBe('{\n  "a": 1\n}')
    expect(formatJsonForDisplay('plain text')).toBe('plain text')
  })
})
