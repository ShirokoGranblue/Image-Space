import { describe, expect, it, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { h } from 'vue'

import GalleryGrid from '../components/gallery/GalleryGrid.vue'
import galleryGridSource from '../components/gallery/GalleryGrid.vue?raw'

const images = [
  {
    uuid: '400a1e49-6990-489e-b4a8-35eb0a02d056',
    imageName: 'wide.jpg',
    width: 1600,
    height: 900,
    thumbUrl: '/wide-thumb.jpg',
    visibility: 'PUBLIC',
  },
  {
    uuid: '973271da-2d93-4ea2-b252-d56cdb356119',
    imageName: 'portrait.jpg',
    width: 900,
    height: 1600,
    thumbUrl: '/portrait-thumb.jpg',
    visibility: 'PUBLIC',
  },
]

function mountGrid(props = {}, slots = {}) {
  return mount(GalleryGrid, {
    props: { items: images, ...props },
    slots,
    global: {
      stubs: {
        'el-icon': { template: '<span class="el-icon-stub" />' },
      },
    },
  })
}

describe('GalleryGrid', () => {
  afterEach(() => {
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 1024 })
    window.dispatchEvent(new Event('resize'))
  })

  it('renders one gallery item per image and marks initial items as eager', () => {
    const wrapper = mountGrid({ initialEagerCount: 1 })
    const cards = wrapper.findAll('.image-card')
    const imageElements = wrapper.findAll('img.card-img')

    expect(cards).toHaveLength(2)
    expect(imageElements[0].attributes('loading')).toBe('eager')
    expect(imageElements[0].attributes('fetchpriority')).toBe('high')
    expect(imageElements[1].attributes('loading')).toBe('lazy')
  })

  it('renders stable ratio skeletons while loading', () => {
    const wrapper = mountGrid({ items: [], loading: true, loadingCount: 4 })

    expect(wrapper.get('[role="status"]').attributes('aria-label')).toBe('图片加载中')
    expect(wrapper.findAll('.gallery-skeleton')).toHaveLength(4)
    expect(wrapper.findAll('.image-card')).toHaveLength(0)
  })

  it('limits eager image loading to the first item on a compact phone', async () => {
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 390 })
    const wrapper = mountGrid({ initialEagerCount: 4 })
    await wrapper.vm.$nextTick()
    const imageElements = wrapper.findAll('img.card-img')

    expect(imageElements[0].attributes('loading')).toBe('eager')
    expect(imageElements[1].attributes('loading')).toBe('lazy')
  })

  it('keeps existing images in place while a page refresh is in progress', () => {
    const wrapper = mountGrid({ loading: true })

    expect(wrapper.findAll('.image-card')).toHaveLength(2)
    expect(wrapper.findAll('.gallery-skeleton')).toHaveLength(0)
    expect(wrapper.text()).toContain('正在更新图片')
  })

  it('renders the shared empty state when no images exist', () => {
    const wrapper = mountGrid({ items: [], emptyTitle: '暂无公开图片' })

    expect(wrapper.text()).toContain('暂无公开图片')
    expect(wrapper.find('.gallery-grid').exists()).toBe(false)
  })

  it('renders a retryable request error instead of an empty result', async () => {
    const wrapper = mountGrid({ error: true, errorDescription: '图片请求失败' })

    expect(wrapper.get('[role="alert"]').text()).toContain('图片请求失败')
    await wrapper.get('[role="alert"] button').trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
  })

  it('forwards item view events without owning navigation', async () => {
    const wrapper = mountGrid({ openMode: 'emit' })
    await wrapper.find('.gallery-item__media-button').trigger('click')

    expect(wrapper.emitted('view')?.[0]).toEqual([images[0]])
  })

  it('wraps result entries with stable keys, preserved slot props and a capped stagger', () => {
    const page = Array.from({ length: 6 }, (_, index) => ({
      ...images[index % images.length],
      uuid: `image-${index}`,
    }))
    const wrapper = mountGrid(
      { items: page, initialEagerCount: 2 },
      {
        item: ({ item, index, priority }) => h('span', {
          class: 'slot-probe',
          'data-index': String(index),
          'data-priority': String(priority),
        }, item.uuid),
      },
    )
    const entries = wrapper.findAll('.gallery-entry')
    const probes = wrapper.findAll('.slot-probe')

    expect(entries.map(entry => entry.attributes('style'))).toEqual([
      '--gallery-entry-delay: 0ms;',
      '--gallery-entry-delay: 40ms;',
      '--gallery-entry-delay: 80ms;',
      '--gallery-entry-delay: 120ms;',
      '--gallery-entry-delay: 120ms;',
      '--gallery-entry-delay: 120ms;',
    ])
    expect(probes.map(probe => probe.attributes('data-index'))).toEqual(['0', '1', '2', '3', '4', '5'])
    expect(probes.map(probe => probe.attributes('data-priority'))).toEqual(['true', 'true', 'false', 'false', 'false', 'false'])
    expect(galleryGridSource).toContain(':key="item.uuid || item.id || index"')
    expect(galleryGridSource).toContain('<TransitionGroup v-else key="content" name="gallery-list"')
    expect(galleryGridSource).toMatch(/\.gallery-list-enter-from\s*\{\s*opacity:\s*0;\s*transform:\s*translateY\(12px\) scale\(0\.97\)/)
    expect(galleryGridSource).toMatch(/@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.gallery-list-enter-from\s*\{\s*opacity:\s*0;\s*transform:\s*none/)
  })

  it('keeps a 100-item page bounded to its page size and lazily loads noncritical thumbnails', () => {
    const page = Array.from({ length: 100 }, (_, index) => ({
      uuid: `image-${index}`,
      imageName: `图片 ${index}`,
      thumbUrl: `/thumbs/${index}.webp`,
      mediumUrl: `/medium/${index}.webp`,
      imageUrl: `/original/${index}.png`,
      width: index % 2 ? 900 : 1600,
      height: index % 2 ? 1600 : 900,
      visibility: 'PUBLIC',
    }))
    const wrapper = mountGrid({ items: page, initialEagerCount: 4 })
    const imageElements = wrapper.findAll('img.card-img')

    expect(wrapper.findAll('.image-card')).toHaveLength(100)
    expect(imageElements).toHaveLength(100)
    expect(imageElements.slice(0, 4).every(image => image.attributes('loading') === 'eager')).toBe(true)
    expect(imageElements.slice(4).every(image => image.attributes('loading') === 'lazy')).toBe(true)
    expect(imageElements.every(image => image.attributes('src').startsWith('/thumbs/'))).toBe(true)
  })
})
