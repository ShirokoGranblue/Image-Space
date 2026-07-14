import { afterEach, describe, expect, it } from 'vitest'
import { nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import SquareImageDrawer from '../components/square/SquareImageDrawer.vue'

const image = {
  uuid: 'image-1',
  imageName: '和纸风景',
  displayName: '作者',
  categoryName: '风景',
  likeCount: 2,
}

function mountDrawer() {
  return mount(SquareImageDrawer, {
    props: {
      visible: true,
      image,
      imageSrc: '/image.jpg',
      tags: [],
    },
    global: {
      stubs: {
        'el-icon': { template: '<span><slot /></span>' },
      },
    },
  })
}

afterEach(() => {
  document.querySelectorAll('.drawer-backdrop').forEach(element => element.remove())
  document.querySelectorAll('.image-drawer').forEach(element => element.remove())
})

describe('SquareImageDrawer', () => {
  it('teleports the non-modal panel to body and focuses its close action', async () => {
    const wrapper = mountDrawer()
    await nextTick()
    await nextTick()

    const drawer = document.body.querySelector('.image-drawer')
    const closeButton = document.body.querySelector('.drawer-close')
    expect(document.body.contains(drawer)).toBe(true)
    expect(drawer?.getAttribute('aria-modal')).toBeNull()
    expect(document.body.querySelector('[inert]')).toBeNull()
    expect(document.activeElement).toBe(closeButton)

    wrapper.unmount()
  })

  it('emits close for Escape from inside the panel', async () => {
    const wrapper = mountDrawer()
    await nextTick()
    await nextTick()

    const drawer = document.body.querySelector('.image-drawer')
    drawer?.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }))

    expect(wrapper.emitted('close')).toHaveLength(1)
    wrapper.unmount()
  })

  it('emits close when the empty backdrop is pressed', async () => {
    const wrapper = mountDrawer()
    await nextTick()
    await nextTick()

    const backdrop = document.body.querySelector('.drawer-backdrop')
    backdrop?.dispatchEvent(new Event('pointerdown', { bubbles: true, cancelable: true }))

    expect(wrapper.emitted('close')).toHaveLength(1)
    wrapper.unmount()
  })
})
