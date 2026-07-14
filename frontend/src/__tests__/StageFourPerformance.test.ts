import { describe, expect, it } from 'vitest'
import { readFileSync, statSync } from 'node:fs'

const main = readFileSync('src/main.js', 'utf8')
const viteConfig = readFileSync('vite.config.js', 'utf8')
const navBar = readFileSync('src/components/NavBar.vue', 'utf8')
const indexHtml = readFileSync('index.html', 'utf8')
const imageHelpers = readFileSync('src/utils/imageRequests.js', 'utf8')

describe('stage four image delivery and bundle contract', () => {
  it('uses Element Plus component resolution instead of global component and icon registration', () => {
    expect(main).not.toContain('app.use(ElementPlus')
    expect(main).not.toContain('ElementPlusIconsVue')
    expect(main).not.toContain("element-plus/dist/index.css")
    expect(main).toContain('provideGlobalConfig({ locale: zhCn }, app, true)')
    expect(viteConfig).toContain("unplugin-vue-components/vite")
    expect(viteConfig).toContain('ElementPlusResolver')
  })

  it('keeps navigation and favicon on the compact derived logo asset', () => {
    expect(navBar).toContain("../logo/logo-nav.webp")
    expect(indexHtml).toContain('/src/logo/logo-nav.webp')
    expect(statSync('src/logo/logo-nav.webp').size).toBeLessThan(100 * 1024)
  })

  it('locks the display, preview, viewer and alt fallback responsibilities to real fields', () => {
    expect(imageHelpers).toContain('if (imageOrId.thumbUrl) return imageOrId.thumbUrl')
    expect(imageHelpers).toContain('if (imageOrId.mediumUrl) return imageOrId.mediumUrl')
    expect(imageHelpers).toContain('export function getImageViewerUrl')
    expect(imageHelpers).toContain('export function getImageAlt')
    expect(imageHelpers).toContain("originalFilename || '未命名图片'")
  })
})
