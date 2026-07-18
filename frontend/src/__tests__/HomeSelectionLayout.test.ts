import { describe, expect, it } from 'vitest'
import homePage from '../views/Home.vue?raw'
import homeToolbar from '../components/home/HomeToolbar.vue?raw'
import galleryItem from '../components/gallery/GalleryItem.vue?raw'

describe('home selection layout contracts', () => {
  it('keeps selected cards on a bounded track instead of stretching the grid', () => {
    expect(homePage).toMatch(/\.gallery-stage :deep\(\.gallery-grid\[data-density='compact'\]\)\s*\{[^}]*238px[^}]*justify-content:\s*start/)
    expect(galleryItem).toMatch(/\.gallery-item\.selected\s*\{\s*box-shadow:\s*inset/)
  })

  it('keeps inspector text inside a padded, wrapping content area', () => {
    expect(homePage).toMatch(/\.inspect-body\s*\{\s*padding:\s*var\(--space-5\)/)
    expect(homePage).toMatch(/\.inspect-head > div\s*\{[^}]*min-width:\s*0[^}]*flex:\s*1/)
    expect(homePage).toMatch(/\.route-line code\s*\{[^}]*min-width:\s*0[^}]*overflow-wrap:\s*anywhere/)
  })

  it('reserves two description lines so opening the inspector does not move the gallery', () => {
    expect(homeToolbar).toMatch(/p\{min-height:3\.3em;/)
  })

  it('forwards card edit actions to the existing image edit dialog', () => {
    expect(homePage).toContain('@edit="handleEdit"')
    expect(homePage).toContain('function handleEdit(img)')
    expect(homePage).toContain('<el-dialog v-model="editVisible"')
  })
})
