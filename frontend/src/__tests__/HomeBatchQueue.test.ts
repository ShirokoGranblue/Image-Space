import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'

import HomeBatchQueue from '../components/home/HomeBatchQueue.vue'
import batchQueueSource from '../components/home/HomeBatchQueue.vue?raw'
import homeToolbarSource from '../components/home/HomeToolbar.vue?raw'

const displayUrl = vi.fn(() => '/thumb.jpg')

describe('HomeBatchQueue', () => {
  it('stays out of the document until selection mode is active', () => {
    const wrapper = mount(HomeBatchQueue, {
      props: { visible: false, displayUrl },
    })

    expect(wrapper.find('.batch-queue').exists()).toBe(false)
  })

  it('reveals the selection queue with a reduced-safe named transition', () => {
    expect(batchQueueSource).toContain('<Transition name="batch-queue">')
    expect(batchQueueSource).toMatch(/\.batch-queue-enter-active,\.batch-queue-leave-active\{transition:opacity var\(--duration-standard\) var\(--ease-out\),transform var\(--duration-standard\) var\(--ease-out\)\}/)
    expect(batchQueueSource).toMatch(/\.batch-queue-enter-from,\.batch-queue-leave-to\{opacity:0;transform:translateY\(12px\)\}/)
    expect(batchQueueSource).toMatch(/@media\(prefers-reduced-motion:reduce\)\{\.batch-queue-enter-active,\.batch-queue-leave-active\{transition:opacity 200ms ease\}\.batch-queue-enter-from,\.batch-queue-leave-to\{opacity:0;transform:none\}\}/)
  })

  it('reveals only the conditional destructive filter chip', () => {
    expect(homeToolbarSource).toContain('<Transition name="destructive-chip"><button v-if="selectedCount>0"')
    expect(homeToolbarSource).toMatch(/\.destructive-chip-enter-active,\.destructive-chip-leave-active\{transition:opacity var\(--duration-fast\) var\(--ease-out\),transform var\(--duration-fast\) var\(--ease-out\)\}/)
    expect(homeToolbarSource).toMatch(/\.destructive-chip-enter-from,\.destructive-chip-leave-to\{opacity:0;transform:scale\(0\.97\)\}/)
    expect(homeToolbarSource).toMatch(/@media\(prefers-reduced-motion:reduce\)[\s\S]*?\.destructive-chip-enter-from,\.destructive-chip-leave-to\{opacity:0;transform:none\}/)
  })

  it('exposes selection actions only for the selected image queue', async () => {
    const wrapper = mount(HomeBatchQueue, {
      props: {
        visible: true,
        images: [{ uuid: 'image-1', imageName: '图片一', imageUrl: '/image.jpg' }],
        selectedCount: 1,
        displayUrl,
      },
    })

    expect(wrapper.get('.batch-queue').text()).toContain('已选图片')
    await wrapper.get('button.secondary-command').trigger('click')
    await wrapper.get('button.primary-command').trigger('click')
    expect(wrapper.emitted('clear')).toHaveLength(1)
    expect(wrapper.emitted('delete')).toHaveLength(1)
  })
})
