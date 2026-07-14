import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

describe('frontend Docker configuration', () => {
  it('keeps Turnstile disabled unless compose explicitly enables both sides', () => {
    const dockerfile = readFileSync('Dockerfile', 'utf8')
    const compose = readFileSync('../docker-compose.yaml', 'utf8')
    const composeExample = readFileSync('../docker-compose.example.yaml', 'utf8')

    expect(dockerfile).toContain('ARG VITE_TURNSTILE_ENABLED=false')
    expect(compose).toContain('VITE_TURNSTILE_ENABLED: ${VITE_TURNSTILE_ENABLED:-false}')
    expect(compose).toContain('VITE_TURNSTILE_SITE_KEY: ${VITE_TURNSTILE_SITE_KEY:-0x4AAAAAADXRE_jtv9_OBFRo}')
    expect(composeExample).toContain('VITE_TURNSTILE_ENABLED: ${VITE_TURNSTILE_ENABLED:-false}')
    expect(composeExample).toContain('VITE_TURNSTILE_SITE_KEY: ${VITE_TURNSTILE_SITE_KEY:-0x4AAAAAADXRE_jtv9_OBFRo}')
  })
})
