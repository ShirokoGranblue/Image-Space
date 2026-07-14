import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'

import HomeBatchQueue from '../components/home/HomeBatchQueue.vue'

const displayUrl = vi.fn(() => '/thumb.jpg')

describe('HomeBatchQueue', () => {
  it('stays out of the document until selection mode is active', () => {
    const wrapper = mount(HomeBatchQueue, {
      props: { visible: false, displayUrl },
    })

    expect(wrapper.find('.batch-queue').exists()).toBe(false)
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
