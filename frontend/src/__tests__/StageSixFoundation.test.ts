import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'
import appRoot from '../App.vue?raw'
import imageUpload from '../components/ImageUpload.vue?raw'
import navBar from '../components/NavBar.vue?raw'
import tagInput from '../components/TagInput.vue?raw'
import astralEnvironment from '../components/astral/AstralEnvironment.vue?raw'
import astralModeControl from '../components/astral/AstralModeControl.vue?raw'
import galleryGrid from '../components/gallery/GalleryGrid.vue?raw'
import galleryItem from '../components/gallery/GalleryItem.vue?raw'
import baseButton from '../components/ui/BaseButton.vue?raw'
import homeToolbar from '../components/home/HomeToolbar.vue?raw'
import imageViewer from '../components/ImageViewer.vue?raw'
import homePage from '../views/Home.vue?raw'
import profilePage from '../views/Profile.vue?raw'
import squareHeroFilters from '../components/square/SquareHeroFilters.vue?raw'

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
  '--astral-backdrop',
  '--astral-brand-position',
  '--avatar-color',
  '--gallery-entry-delay',
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

  it('publishes semantic motion curves while keeping the standard compatibility alias', () => {
    expect(tokens).toMatch(/--ease-out:\s*cubic-bezier\(0\.23,\s*1,\s*0\.32,\s*1\)/)
    expect(tokens).toMatch(/--ease-in-out:\s*cubic-bezier\(0\.77,\s*0,\s*0\.175,\s*1\)/)
    expect(tokens).toMatch(/--ease-drawer:\s*cubic-bezier\(0\.32,\s*0\.72,\s*0,\s*1\)/)
    expect(tokens).toMatch(/--ease-standard:\s*var\(--ease-out\)/)
  })

  it('uses the shared motion duration for upload and tag-input transitions', () => {
    for (const source of [imageUpload, tagInput]) {
      expect(source).not.toContain('0.2s ease')
      expect(source).toContain('var(--duration-standard) ease')
    }
  })

  it('gives the Home toolbar controls shared press feedback', () => {
    expect(homeToolbar).toMatch(/\.select-all,\.primary-command,\.filter-chip\{[^}]*transition:\s*transform var\(--duration-fast\) var\(--ease-out\)/)
    expect(homeToolbar).toMatch(/\.select-all:active,\.primary-command:active,\.filter-chip:active\{transform:\s*scale\(0\.97\)/)
  })

  it('gives square sort and active-filter controls shared press feedback', () => {
    expect(squareHeroFilters).toMatch(/\.sort-segment button\s*\{[^}]*transition:\s*transform var\(--duration-fast\) var\(--ease-out\)/)
    expect(squareHeroFilters).toMatch(/\.active-filters button\s*\{[^}]*transition:\s*transform var\(--duration-fast\) var\(--ease-out\)/)
    expect(squareHeroFilters).toMatch(/\.sort-segment button:active,\.active-filters button:active\s*\{\s*transform:\s*scale\(0\.97\)/)
  })

  it('shares one linear gallery shimmer across grid and item placeholders', () => {
    expect(tokens.match(/@keyframes media-shimmer/g) ?? []).toHaveLength(1)
    expect(tokens).toMatch(/@keyframes media-shimmer\s*\{\s*from\s*\{\s*transform:\s*translateX\(-100%\);\s*\}\s*to\s*\{\s*transform:\s*translateX\(100%\);\s*\}\s*\}/)
    for (const source of [galleryGrid, galleryItem]) {
      expect(source).toContain('animation: media-shimmer 1.2s linear infinite')
    }
    expect(galleryGrid).not.toContain('grid-shimmer')
    expect(galleryItem).not.toContain('gallery-shimmer')
  })

  it('keeps the viewer loading sweep unidirectional and linear', () => {
    expect(imageViewer).toContain('animation: viewer-load 1s linear infinite')
    expect(imageViewer).not.toContain('viewer-load 1s ease-in-out infinite alternate')
    expect(imageViewer).toMatch(/@keyframes viewer-load\s*\{\s*from\s*\{\s*transform:\s*translateX\(-100%\);\s*\}\s*to\s*\{\s*transform:\s*translateX\(200%\);\s*\}\s*\}/)
  })

  it('does not reintroduce the retired token names or the app-local fixed drawer', () => {
    const retiredNames = [
      '--black', '--blue-soft', '--color-viewer-stage', '--duration-normal',
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

  it('keeps admin routes neutral while non-admin routes own one Astral environment and mode control', () => {
    expect(appRoot).toContain("if (route.path.startsWith('/admin/')) return null")
    expect(appRoot).toMatch(/<AstralEnvironment\s+[\s\S]*?v-if="astralIntensity"/)
    expect(appRoot).toMatch(/<AstralModeControl\s+[\s\S]*?v-if="astralIntensity"/)
  })

  it('keeps viewport controls, modes and decorative colors under one Astral source of truth', () => {
    expect(astralModeControl).toMatch(/<Teleport to="body">[\s\S]*class="astral-mode-control"/)
    expect(appRoot).toContain("from './components/astral/astralSystem'")
    expect(astralEnvironment).toContain("from './astralSystem'")
    expect(astralModeControl).toContain("from './astralSystem'")

    for (const source of [astralEnvironment, navBar, profilePage]) {
      for (const color of ['#f4f1ed', '#b9a36f', '#5d8f8b', '#556d91', '#a77b83', '#695879']) {
        expect(source.toLowerCase()).not.toContain(color)
      }
    }
  })

  it('separates semantic states and neutral visibility categories from decorative colors', () => {
    for (const token of ['success', 'warning', 'error', 'info']) {
      expect(tokens).toMatch(new RegExp(`--color-${token}:\\s*#[0-9a-f]{6}`, 'i'))
      expect(tokens).not.toMatch(new RegExp(`--color-${token}:\\s*var\\(--astral-`, 'i'))
    }
    for (const visibility of ['public', 'specified', 'private']) {
      expect(tokens).toMatch(new RegExp(`--color-visibility-${visibility}:\\s*#[0-9a-f]{6}`, 'i'))
      expect(galleryItem).toContain(`var(--color-visibility-${visibility})`)
      expect(homePage).toContain(`var(--color-visibility-${visibility})`)
    }
  })

  it('keeps the reviewed controls within the frozen radius and weight scale', () => {
    expect(imageUpload).not.toMatch(/border-radius:\s*(?:18|20)px/)
    expect(imageUpload).toContain('border-radius: var(--radius-lg)')
    expect(baseButton).not.toContain('font-weight: 650')
    expect(galleryItem).not.toContain('font-weight: 650')
  })
})
