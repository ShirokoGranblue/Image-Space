import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

import TurnstileWidget from '../components/TurnstileWidget.vue'

describe('TurnstileWidget', () => {
  it('shows a visible loading state while the Turnstile script is loading', async () => {
    window.__turnstileLoading = new Promise(() => {})

    const wrapper = mount(TurnstileWidget, {
      props: { enabled: true },
    })
    await nextTick()

    expect(wrapper.find('.turnstile-shell').isVisible()).toBe(true)
    expect(wrapper.find('.turnstile-status').text()).toBe('人机验证加载中')

    delete window.__turnstileLoading
  })
})
