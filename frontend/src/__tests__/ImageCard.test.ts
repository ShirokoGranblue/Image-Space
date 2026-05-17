import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

import ImageCard from '../components/ImageCard.vue'

const mockImage = {
  id: 1,
  imageName: 'test-image.jpg',
  imagePath: 'data:image/png;base64,xxxx',
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
})
