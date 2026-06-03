import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))

describe('Profile resource polling integration', () => {
  it('does not append Date.now cache busters to avatar or background URLs', () => {
    const source = readFileSync(resolve(__dirname, '../views/Profile.vue'), 'utf8')

    expect(source).not.toContain('mediaUrlWithVersion')
    expect(source).not.toContain('avatarVersion')
    expect(source).not.toContain('backgroundVersion')
    expect(source).not.toContain('Date.now()')
    expect(source).toContain('useResourcePolling')
    expect(source).toContain('getUserMediaResourceStatus')
  })
})
