import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
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

const items = [
  { uuid: 'a', imageName: '横图', mediumUrl: 'https://cdn.test/a.jpg', width: 1600, height: 900, username: '作者甲' },
  { uuid: 'b', imageName: '竖图', mediumUrl: 'https://cdn.test/b.jpg', width: 900, height: 1500, categoryName: '人像' },
  { uuid: 'c', imageName: '方图', mediumUrl: 'https://cdn.test/c.jpg', width: 1200, height: 1200 },
]

describe('ImageViewer', () => {
  beforeEach(() => {
    document.body.style.overflow = ''
  })

  afterEach(() => {
    document.body.innerHTML = ''
    vi.restoreAllMocks()
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

    it('restores focus to the triggering element after close', async () => {
      const trigger = document.createElement('button')
      document.body.appendChild(trigger)
      trigger.focus()
      const wrapper = mountViewer()

      // @ts-ignore
      wrapper.vm.open({ trigger })
      await nextTick()
      await nextTick()
      // @ts-ignore
      wrapper.vm.close()
      await nextTick()

      expect(document.activeElement).toBe(trigger)
    })

    it('isolates the app while open and restores its previous accessibility state', async () => {
      const appRoot = document.createElement('div')
      appRoot.id = 'app'
      appRoot.setAttribute('aria-hidden', 'false')
      document.body.appendChild(appRoot)
      const wrapper = mountViewer()

      // @ts-ignore
      wrapper.vm.open()
      await nextTick()
      await nextTick()

      expect(appRoot.inert).toBe(true)
      expect(appRoot.getAttribute('aria-hidden')).toBe('true')
      expect(document.documentElement.classList.contains('viewer-open')).toBe(true)

      // @ts-ignore
      wrapper.vm.close()
      await nextTick()

      expect(appRoot.inert).toBe(false)
      expect(appRoot.getAttribute('aria-hidden')).toBe('false')
      expect(document.documentElement.classList.contains('viewer-open')).toBe(false)
    })

    it('restores app isolation when unmounted while open', async () => {
      const appRoot = document.createElement('div')
      appRoot.id = 'app'
      appRoot.inert = true
      appRoot.setAttribute('aria-hidden', 'menu-open')
      document.body.appendChild(appRoot)
      const wrapper = mountViewer()

      // @ts-ignore
      wrapper.vm.open()
      await nextTick()
      await nextTick()
      wrapper.unmount()

      expect(appRoot.inert).toBe(true)
      expect(appRoot.getAttribute('aria-hidden')).toBe('menu-open')
      expect(document.documentElement.classList.contains('viewer-open')).toBe(false)
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

    it('switches images with arrow keys and emits change', async () => {
      const wrapper = mount(ImageViewer, {
        props: { items },
        global: { stubs: { 'el-icon': { template: '<span />' }, teleport: { template: '<div><slot /></div>' } } },
        attachTo: document.body,
      })
      // @ts-ignore
      wrapper.vm.open({ index: 1 })
      await nextTick()

      await wrapper.find('.viewer-overlay').trigger('keydown', { key: 'ArrowRight' })
      expect(wrapper.find('.viewer-heading strong').text()).toBe('方图')
      expect(wrapper.emitted('change')?.[0]?.[0]).toBe(2)

      await wrapper.find('.viewer-overlay').trigger('keydown', { key: 'ArrowLeft' })
      expect(wrapper.find('.viewer-heading strong').text()).toBe('竖图')
    })

    it('announces only the latest image after rapid switches', async () => {
      const wrapper = mount(ImageViewer, {
        props: { items },
        global: { stubs: { 'el-icon': { template: '<span />' }, teleport: { template: '<div><slot /></div>' } } },
      })
      // @ts-ignore
      wrapper.vm.open({ index: 0 })
      // @ts-ignore
      wrapper.vm.next()
      // @ts-ignore
      wrapper.vm.next()
      await nextTick()
      await nextTick()

      expect(wrapper.find('.viewer-announcement').text()).toBe('第 3 张，共 3 张，方图')
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

  describe('Image states and information', () => {
    it('requests native fullscreen from the viewer control', async () => {
      const wrapper = mountViewer()
      // @ts-ignore
      wrapper.vm.open()
      await nextTick()
      const requestFullscreen = vi.fn().mockResolvedValue(undefined)
      Object.defineProperty(wrapper.get('.viewer-overlay').element, 'requestFullscreen', { configurable: true, value: requestFullscreen })

      await wrapper.get('[aria-label="进入全屏"]').trigger('click')

      expect(requestFullscreen).toHaveBeenCalledOnce()
    })

    it('tracks fullscreen changes from the browser and exposes the button state', async () => {
      const wrapper = mountViewer()
      // @ts-ignore
      wrapper.vm.open()
      await nextTick()
      await nextTick()
      const overlay = wrapper.get('.viewer-overlay').element
      let fullscreenElement = null
      Object.defineProperty(document, 'fullscreenElement', {
        configurable: true,
        get: () => fullscreenElement,
      })
      Object.defineProperty(overlay, 'requestFullscreen', {
        configurable: true,
        value: vi.fn(async () => {
          fullscreenElement = overlay
          document.dispatchEvent(new Event('fullscreenchange'))
        }),
      })

      await wrapper.get('[aria-label="进入全屏"]').trigger('click')
      await nextTick()

      expect(wrapper.get('[aria-label="退出全屏"]').attributes('aria-pressed')).toBe('true')

      fullscreenElement = null
      document.dispatchEvent(new Event('fullscreenchange'))
      await nextTick()

      expect(wrapper.get('[aria-label="进入全屏"]').attributes('aria-pressed')).toBe('false')
    })

    it('uses contain presentation and exposes only real metadata fields', async () => {
      const wrapper = mount(ImageViewer, {
        props: { items },
        global: { stubs: { 'el-icon': { template: '<span />' }, teleport: { template: '<div><slot /></div>' } } },
      })
      // @ts-ignore
      wrapper.vm.open({ index: 0 })
      await nextTick()

      expect(wrapper.find('.viewer-img').attributes('src')).toBe('https://cdn.test/a.jpg')
      expect(wrapper.find('.viewer-img').attributes('alt')).toBe('横图')
      expect(wrapper.find('.viewer-img').classes()).toContain('viewer-img')
      await wrapper.get('[aria-label="查看图片信息"]').trigger('click')
      expect(wrapper.find('.viewer-metadata').text()).toContain('作者甲')
      expect(wrapper.find('.viewer-metadata').text()).toContain('1600 × 900')
      expect(wrapper.find('.viewer-metadata').text()).not.toContain('拍摄时间')
    })

    it('uses originalFilename when the active item has no image name', async () => {
      const wrapper = mount(ImageViewer, {
        props: { items: [{ uuid: 'original-only', originalFilename: 'camera-original.png', mediumUrl: 'https://cdn.test/original-only.jpg' }] },
        global: { stubs: { 'el-icon': { template: '<span />' }, teleport: { template: '<div><slot /></div>' } } },
      })

      // @ts-ignore
      wrapper.vm.open()
      await nextTick()

      expect(wrapper.find('.viewer-img').attributes('alt')).toBe('camera-original.png')
      expect(wrapper.find('.viewer-heading strong').text()).toBe('camera-original.png')
    })

    it('shows an error with retry when the original image fails', async () => {
      const wrapper = mountViewer('https://cdn.test/retry.jpg')
      // @ts-ignore
      wrapper.vm.open()
      await nextTick()
      await wrapper.find('.viewer-img').trigger('error')

      expect(wrapper.find('.viewer-error').attributes('role')).toBe('alert')
      expect(wrapper.find('.viewer-error').text()).toContain('原图加载失败')
      await wrapper.get('.viewer-error button').trigger('click')
      // @ts-ignore
      expect(wrapper.vm.imageStatus).toBe('loading')
      expect(wrapper.find('.viewer-img').attributes('src')).toContain('__viewer_retry=1')
    })

    it('ignores a stale error after switching to a different image', async () => {
      const wrapper = mount(ImageViewer, {
        props: { items },
        global: { stubs: { 'el-icon': { template: '<span />' }, teleport: { template: '<div><slot /></div>' } } },
      })
      // @ts-ignore
      wrapper.vm.open({ index: 0 })
      await nextTick()
      const staleImage = wrapper.get('.viewer-img').element as HTMLImageElement
      staleImage.dispatchEvent(new Event('load'))
      // @ts-ignore
      expect(wrapper.vm.imageStatus).toBe('loaded')

      // @ts-ignore
      wrapper.vm.next()
      await nextTick()
      // @ts-ignore
      expect(wrapper.vm.imageStatus).toBe('loading')

      staleImage.dispatchEvent(new Event('error'))
      // @ts-ignore
      expect(wrapper.vm.imageStatus).toBe('loading')

      wrapper.get('.viewer-img').element.dispatchEvent(new Event('load'))
      // @ts-ignore
      expect(wrapper.vm.imageStatus).toBe('loaded')
    })

    it('isolates stale events when consecutive items share the same URL', async () => {
      const sameUrlItems = [
        { uuid: 'same-a', imageName: '同源图 A', mediumUrl: 'https://cdn.test/shared.jpg' },
        { uuid: 'same-b', imageName: '同源图 B', mediumUrl: 'https://cdn.test/shared.jpg' },
      ]
      const wrapper = mount(ImageViewer, {
        props: { items: sameUrlItems },
        global: { stubs: { 'el-icon': { template: '<span />' }, teleport: { template: '<div><slot /></div>' } } },
      })
      // @ts-ignore
      wrapper.vm.open({ index: 0 })
      await nextTick()
      const staleImage = wrapper.get('.viewer-img').element as HTMLImageElement

      // @ts-ignore
      wrapper.vm.next()
      await nextTick()
      const currentImage = wrapper.get('.viewer-img').element as HTMLImageElement
      expect(currentImage.dataset.requestKey).not.toBe(staleImage.dataset.requestKey)

      currentImage.dispatchEvent(new Event('error'))
      // @ts-ignore
      expect(wrapper.vm.imageStatus).toBe('error')
      staleImage.dispatchEvent(new Event('load'))
      // @ts-ignore
      expect(wrapper.vm.imageStatus).toBe('error')
    })

    it('ignores pre-retry events after starting a new request revision', async () => {
      const wrapper = mountViewer()
      // @ts-ignore
      wrapper.vm.open()
      await nextTick()
      const staleImage = wrapper.get('.viewer-img').element
      staleImage.dispatchEvent(new Event('error'))
      await nextTick()
      expect(wrapper.find('.viewer-error').exists()).toBe(true)

      await wrapper.get('.viewer-error button').trigger('click')
      await nextTick()
      // @ts-ignore
      expect(wrapper.vm.imageStatus).toBe('loading')

      staleImage.dispatchEvent(new Event('load'))
      // @ts-ignore
      expect(wrapper.vm.imageStatus).toBe('loading')
      wrapper.get('.viewer-img').element.dispatchEvent(new Event('load'))
      // @ts-ignore
      expect(wrapper.vm.imageStatus).toBe('loaded')
    })

    it('preloads adjacent image URLs when opening a sequence', async () => {
      const assigned = []
      const OriginalImage = globalThis.Image
      // @ts-ignore
      globalThis.Image = class { set src(value) { assigned.push(value) } }
      const wrapper = mount(ImageViewer, {
        props: { items },
        global: { stubs: { 'el-icon': { template: '<span />' }, teleport: { template: '<div><slot /></div>' } } },
      })
      // @ts-ignore
      wrapper.vm.open({ index: 1 })
      await nextTick()

      expect(assigned).toContain('https://cdn.test/a.jpg')
      expect(assigned).toContain('https://cdn.test/c.jpg')
      globalThis.Image = OriginalImage
    })

    it('preloads only the next image on a compact mobile viewport', async () => {
      const assigned = []
      const OriginalImage = globalThis.Image
      const originalMatchMedia = window.matchMedia
      // @ts-ignore
      window.matchMedia = vi.fn(() => ({ matches: true }))
      // @ts-ignore
      globalThis.Image = class { set src(value) { assigned.push(value) } }
      const wrapper = mount(ImageViewer, {
        props: { items },
        global: { stubs: { 'el-icon': { template: '<span />' }, teleport: { template: '<div><slot /></div>' } } },
      })
      // @ts-ignore
      wrapper.vm.open({ index: 1 })
      await nextTick()

      expect(assigned).toEqual(['https://cdn.test/c.jpg'])
      globalThis.Image = OriginalImage
      window.matchMedia = originalMatchMedia
    })
  })
})
