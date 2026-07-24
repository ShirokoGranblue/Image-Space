import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

import ImageCard from '../components/ImageCard.vue'
import galleryItemSource from '../components/gallery/GalleryItem.vue?raw'

const mockImage = {
  id: 1,
  uuid: '400a1e49-6990-489e-b4a8-35eb0a02d056',
  imageName: 'test-image.jpg',
  imagePath: 'data:image/png;base64,xxxx',
  width: 1600,
  height: 900,
  fileSize: 102400,
  uploadTime: '2026-05-17T10:30:00',
  categoryName: '风景',
  visibility: 'PUBLIC',
}

function mountCard(overrides = {}) {
  return mount(ImageCard, {
    props: {
      image: mockImage,
      ...overrides,
    },
    global: {
      stubs: {
        'el-icon': { template: '<span class="el-icon-stub" />' },
        'el-popconfirm': {
          template: '<div class="el-popconfirm-stub"><slot name="reference" /></div>',
          emits: ['confirm', 'show', 'hide'],
        },
      },
    },
  })
}

describe('ImageCard', () => {
  describe('Broken image fallback', () => {
    it('renders img element when image has not errored', () => {
      const wrapper = mountCard()
      expect(wrapper.find('img.card-img').exists()).toBe(true)
    })

    it('renders the thumbnail URL before medium or original URLs', () => {
      const wrapper = mountCard({
        image: {
          ...mockImage,
          thumbUrl: 'https://cdn.image-space.app/public/images/a/thumb.jpg?v=2',
          mediumUrl: 'https://cdn.image-space.app/public/images/a/medium.jpg?v=2',
          imageUrl: 'https://cdn.image-space.app/public/images/a/original.png?v=2',
        },
      })

      expect(wrapper.find('img.card-img').attributes('src')).toBe('https://cdn.image-space.app/public/images/a/thumb.jpg?v=2')
    })

    it('uses the original filename when an image name is unavailable', () => {
      const wrapper = mountCard({ image: { ...mockImage, imageName: '', originalFilename: 'camera-original.png' } })

      expect(wrapper.find('img.card-img').attributes('alt')).toBe('camera-original.png')
      expect(wrapper.find('.gallery-item__media-button').attributes('aria-label')).toBe('查看图片：camera-original.png')
    })

    it('reserves the backend-provided aspect ratio and contains the image', () => {
      const wrapper = mountCard()

      expect(wrapper.find('.gallery-item__media').attributes('style')).toContain('--image-aspect-ratio: 1600 / 900')
      expect(wrapper.find('img.card-img').classes()).toContain('fit-contain')
    })

    it.each([
      [2400, 600, '2400 / 600'],
      [600, 2400, '600 / 2400'],
      [1200, 1200, '1200 / 1200'],
    ])('keeps mixed image proportions without rewriting them', (width, height, ratio) => {
      const wrapper = mountCard({ image: { ...mockImage, width, height } })

      expect(wrapper.find('.gallery-item__media').attributes('style')).toContain(`--image-aspect-ratio: ${ratio}`)
    })

    it('uses a stable fallback ratio only when backend dimensions are absent', () => {
      const wrapper = mountCard({ image: { ...mockImage, width: null, height: null } })

      expect(wrapper.find('.gallery-item__media').attributes('style')).toContain('--image-aspect-ratio: 4 / 3')
    })

    it('keeps a loading placeholder until the thumbnail finishes loading', async () => {
      const wrapper = mountCard()

      expect(wrapper.find('.image-placeholder').exists()).toBe(true)
      await wrapper.find('img.card-img').trigger('load')
      expect(wrapper.find('.image-placeholder').exists()).toBe(false)
    })

    it('reveals a loaded image and resets the reveal when its URL changes', async () => {
      const wrapper = mountCard()
      const image = wrapper.get('img.card-img')

      expect(image.classes()).not.toContain('is-loaded')
      await image.trigger('load')
      expect(wrapper.get('img.card-img').classes()).toContain('is-loaded')

      await wrapper.setProps({ image: { ...mockImage, thumbUrl: '/changed-thumb.webp' } })
      expect(wrapper.get('img.card-img').classes()).not.toContain('is-loaded')

      expect(galleryItemSource).toMatch(/\.card-img\s*\{[^}]*opacity:\s*0;[^}]*transform:\s*scale\(0\.97\)/)
      expect(galleryItemSource).toMatch(/\.card-img\.is-loaded\s*\{\s*opacity:\s*1;\s*transform:\s*none/)
      expect(galleryItemSource).toContain('.gallery-item:hover .card-img.is-loaded')
      expect(galleryItemSource).toMatch(/@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.card-img\s*\{\s*transform:\s*none;\s*transition:\s*opacity 200ms ease/)
    })

    it('shows fallback element after image error event', async () => {
      const wrapper = mountCard()

      await wrapper.find('img.card-img').trigger('error')
      await nextTick()

      // After error: img element should be removed (v-if="!imgFailed")
      expect(wrapper.find('img.card-img').exists()).toBe(false)
      // Fallback div should appear
      expect(wrapper.find('.img-fallback').exists()).toBe(true)
    })
  })

  describe('Visibility label', () => {
    it('displays PUBLIC for public images when showActions on hover', async () => {
      const wrapper = mountCard({ showActions: true })
      // Need hover to see the overlay with visibility badge
      await wrapper.find('.image-card').trigger('mouseenter')
      await nextTick()
      expect(wrapper.text()).toContain('公开')
    })

    it('displays PRIVATE for private images', async () => {
      const wrapper = mountCard({
        showActions: true,
        image: { ...mockImage, visibility: 'PRIVATE' },
      })
      await wrapper.find('.image-card').trigger('mouseenter')
      await nextTick()
      expect(wrapper.text()).toContain('仅自己')
    })
  })

  describe('Selection', () => {
    it('emits toggle-select without opening detail when selection button is clicked', async () => {
      const wrapper = mountCard({ selectable: true })

      await wrapper.find('.select-toggle').trigger('click')

      expect(wrapper.emitted('toggle-select')).toEqual([[mockImage.uuid]])
    })

    it('marks the card selected when selected prop is true', () => {
      const wrapper = mountCard({ selectable: true, selected: true })

      expect(wrapper.find('.image-card.selected').exists()).toBe(true)
      expect(wrapper.find('.select-toggle').attributes('aria-pressed')).toBe('true')
    })

    it('draws selection inside a constant-size mark without geometry changes', () => {
      expect(galleryItemSource).toMatch(/\.gallery-item\s*\{[^}]*transition:[^;}]*border-color[^;}]*box-shadow var\(--duration-fast\) ease/)
      expect(galleryItemSource).toMatch(/\.select-mark\s*\{[^}]*position:\s*relative;[^}]*width:\s*12px;[^}]*height:\s*12px;[^}]*transition:[^}]*border-color[^}]*background-color/)
      expect(galleryItemSource).toMatch(/\.select-mark::after\s*\{[^}]*width:\s*5px;[^}]*height:\s*9px;[^}]*opacity:\s*0;[^}]*transform:\s*rotate\(45deg\) scale\(0\.7\)/)
      expect(galleryItemSource).toMatch(/\.select-toggle\.checked \.select-mark::after\s*\{\s*opacity:\s*1;\s*transform:\s*rotate\(45deg\) scale\(1\)/)
      expect(galleryItemSource).not.toMatch(/\.select-toggle\.checked \.select-mark\s*\{[^}]*width:/)
      expect(galleryItemSource).toMatch(/@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.select-mark::after\s*\{[^}]*transition:\s*opacity 200ms ease;[^}]*transform:\s*rotate\(45deg\) scale\(1\)/)
    })
  })

  describe('Accessible actions', () => {
    it('uses a real button with an accessible name to open the image', () => {
      const wrapper = mountCard()

      expect(wrapper.find('.gallery-item__media-button').attributes('aria-label')).toBe('查看图片：test-image.jpg')
    })

    it('does not expose a favorite action without a backend capability', () => {
      const wrapper = mountCard({ variant: 'square' })

      expect(wrapper.find('[aria-label="收藏图片"]').exists()).toBe(false)
    })
  })
})
