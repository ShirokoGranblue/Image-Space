import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

import ImageViewer from '../components/ImageViewer.vue'

function mountViewer(src = 'data:image/png;base64,xxxx') {
  return mount(ImageViewer, {
    props: { src },
    global: {
      stubs: {
        'el-icon': { template: '<span class="el-icon-stub" />' },
        'el-button': {
          name: 'ElButton',
          props: ['circle', 'disabled'],
          template: '<button :disabled="disabled" :class="$attrs.class"><slot /></button>',
        },
        'teleport': { template: '<div><slot /></div>' },
      },
    },
    attachTo: document.body,
  })
}

describe('ImageViewer', () => {
  beforeEach(() => {
    document.body.style.overflow = ''
  })

  describe('Open/Close', () => {
    it('is hidden by default', () => {
      const wrapper = mountViewer()
      expect(wrapper.find('.viewer-overlay').exists()).toBe(false)
    })

    it('becomes visible when open() is called', async () => {
      const wrapper = mountViewer()

      // @ts-ignore - expose
      wrapper.vm.open()
      await nextTick()

      expect(wrapper.find('.viewer-overlay').exists()).toBe(true)
    })

    it('hides when close() is called', async () => {
      const wrapper = mountViewer()

      // @ts-ignore
      wrapper.vm.open()
      await nextTick()
      expect(wrapper.find('.viewer-overlay').exists()).toBe(true)

      // @ts-ignore
      wrapper.vm.close()
      await nextTick()
      expect(wrapper.find('.viewer-overlay').exists()).toBe(false)
    })

    it('closes when clicking overlay background', async () => {
      const wrapper = mountViewer()

      // @ts-ignore
      wrapper.vm.open()
      await nextTick()

      await wrapper.find('.viewer-overlay').trigger('click')
      await nextTick()

      expect(wrapper.find('.viewer-overlay').exists()).toBe(false)
    })
  })

  describe('Keyboard controls', () => {
    it('closes on Escape key', async () => {
      const wrapper = mountViewer()

      // @ts-ignore
      wrapper.vm.open()
      await nextTick()

      await wrapper.find('.viewer-overlay').trigger('keydown', { key: 'Escape' })
      await nextTick()

      expect(wrapper.find('.viewer-overlay').exists()).toBe(false)
    })

    it('resets zoom on 0 key', async () => {
      const wrapper = mountViewer()

      // @ts-ignore
      wrapper.vm.open()
      await nextTick()

      // Zoom in first
      await wrapper.find('.viewer-overlay').trigger('keydown', { key: '=' })
      await nextTick()
      // @ts-ignore - check scale
      expect(wrapper.vm.scale).toBeGreaterThan(1)

      // Reset
      await wrapper.find('.viewer-overlay').trigger('keydown', { key: '0' })
      await nextTick()
      // Should be back to 1
      // @ts-ignore
      expect(wrapper.vm.scale).toBe(1)
    })
  })

  describe('Body scroll lock', () => {
    it('locks body scroll when opened', async () => {
      const wrapper = mountViewer()

      // @ts-ignore
      wrapper.vm.open()
      // The watch is async and has its own await nextTick before setting overflow
      await nextTick()
      await nextTick()

      expect(document.body.style.overflow).toBe('hidden')
    })

    it('restores body scroll when closed', async () => {
      const wrapper = mountViewer()

      // @ts-ignore
      wrapper.vm.open()
      await nextTick()
      await nextTick()
      expect(document.body.style.overflow).toBe('hidden')

      // @ts-ignore
      wrapper.vm.close()
      await nextTick()
      expect(document.body.style.overflow).toBe('')
    })
  })

  describe('Zoom', () => {
    it('starts at 100%', async () => {
      const wrapper = mountViewer()

      // @ts-ignore
      wrapper.vm.open()
      await nextTick()

      // @ts-ignore
      expect(wrapper.vm.scale).toBe(1)
    })

    it('zooms in via method', async () => {
      const wrapper = mountViewer()

      // @ts-ignore
      wrapper.vm.open()
      await nextTick()

      // @ts-ignore
      wrapper.vm.zoomIn()
      await nextTick()

      // @ts-ignore
      expect(wrapper.vm.scale).toBeGreaterThan(1)
    })

    it('does not exceed max zoom', async () => {
      const wrapper = mountViewer()

      // @ts-ignore
      wrapper.vm.open()
      await nextTick()

      // Zoom in many times
      for (let i = 0; i < 30; i++) {
        // @ts-ignore
        wrapper.vm.zoomIn()
      }

      // @ts-ignore
      expect(wrapper.vm.scale).toBeLessThanOrEqual(5)
    })
  })
})
