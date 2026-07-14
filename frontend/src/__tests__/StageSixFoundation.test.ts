import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'

const sourceRoot = 'src'
const tokens = readFileSync(join(sourceRoot, 'styles/tokens.css'), 'utf8')

function listProductionFiles(directory: string): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) return listProductionFiles(path)
    return /\.(vue|css)$/.test(entry.name) ? [path] : []
  })
}

const productionFiles = listProductionFiles(sourceRoot)
const productionSource = productionFiles.map(path => readFileSync(path, 'utf8')).join('\n')
const tokenNames = new Set([...tokens.matchAll(/^\s*(--[A-Za-z0-9_-]+)\s*:/gm)].map(match => match[1]))
const localVariableNames = new Set([
  '--avatar-color',
  '--gallery-min-width',
  '--image-aspect-ratio',
  '--skeleton-ratio',
])

describe('stage six design foundation', () => {
  it('keeps production var references in tokens or an explicit local-variable allowlist', () => {
    const undefinedVariables = [...productionSource.matchAll(/var\(\s*(--[A-Za-z0-9_-]+)/g)]
      .map(match => match[1])
      .filter((name, index, names) => names.indexOf(name) === index)
      .filter(name => !tokenNames.has(name) && !localVariableNames.has(name))

    expect(undefinedVariables).toEqual([])
  })

  it('freezes the stage one dimensions, panel widths and layer hierarchy', () => {
    expect(tokens).toMatch(/--control-height-sm:\s*36px/)
    expect(tokens).toMatch(/--control-height-md:\s*40px/)
    expect(tokens).toMatch(/--control-height-lg:\s*44px/)
    expect(tokens).toMatch(/--nav-height:\s*72px/)
    expect(tokens).toMatch(/--nav-height:\s*64px/)
    expect(tokens).toMatch(/--panel-aside-width:\s*320px/)
    expect(tokens).toMatch(/--panel-drawer-width:\s*420px/)
    expect(tokens).toMatch(/--auth-form-narrow:\s*480px/)
    expect(tokens).toMatch(/--auth-form-wide:\s*640px/)
    expect(tokens).toMatch(/--layer-nav:\s*100/)
    expect(tokens).toMatch(/--layer-floating:\s*1100/)
    expect(tokens).toMatch(/--layer-drawer:\s*1200/)
    expect(tokens).toMatch(/--layer-notification:\s*2000/)
    expect(tokens).toMatch(/--layer-viewer:\s*3000/)
    expect(tokens).toMatch(/--el-component-size-large:\s*44px/)
    expect(tokens).toMatch(/--el-component-size:\s*40px/)
    expect(tokens).toMatch(/--el-component-size-small:\s*32px/)
  })

  it('does not reintroduce the retired token names or the app-local fixed drawer', () => {
    const retiredNames = [
      '--black', '--blue-soft', '--color-viewer-stage', '--duration-normal', '--ease-out',
      '--gray1', '--gray2', '--gray3', '--leading-body', '--shadow-1', '--shadow-3',
      '--text-caption', '--text-body', '--text-small', '--z-drawer', '--bg-base',
      '--bg-surface', '--bg-elevated', '--bg-hover', '--bg-inverse', '--border-subtle',
      '--border-visible', '--border-strong', '--text-primary', '--text-secondary',
      '--text-muted', '--text-inverse', '--accent', '--accent-hover', '--accent-dim',
      '--accent-warm', '--danger', '--success', '--font-display',
    ]

    for (const name of retiredNames) {
      expect(productionSource).not.toContain(`var(${name}`)
    }
    expect(productionSource).toContain('<Teleport to="body">')
    expect(productionSource).toContain('var(--skeleton-ratio, 4 / 3)')
    expect(productionSource).toContain('var(--avatar-color, var(--color-urban))')
  })
})
