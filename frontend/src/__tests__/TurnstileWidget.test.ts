import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { readFileSync } from 'node:fs'

import TurnstileWidget from '../components/TurnstileWidget.vue'

const turnstileSource = readFileSync('src/components/TurnstileWidget.vue', 'utf8')

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

  it('returns "turnstile-disabled" sentinel when disabled so callers can distinguish from unverified', () => {
    const wrapper = mount(TurnstileWidget, {
      props: { enabled: false },
    })

    // getToken is exposed via defineExpose
    const token = (wrapper.vm as any).getToken()
    expect(token).toBe('turnstile-disabled')
  })

  it('returns empty string when enabled but not yet verified', async () => {
    const wrapper = mount(TurnstileWidget, {
      props: { enabled: true, sitekey: 'test-key' },
    })
    await nextTick()

    const token = (wrapper.vm as any).getToken()
    expect(token).toBe('')
  })

  it('reserves one stable 65px slot for loading, failure, and rendered states', () => {
    expect(turnstileSource).toMatch(/\.turnstile-shell\s*\{[\s\S]*min-height:\s*65px/)
    expect(turnstileSource).toMatch(/\.turnstile-wrap\s*\{[\s\S]*min-height:\s*65px/)
    expect(turnstileSource).toMatch(/\.turnstile-status\s*\{[\s\S]*min-height:\s*65px/)
    expect(turnstileSource).not.toMatch(/\.turnstile-wrap\.is-empty\s*\{[\s\S]*min-height:\s*0/)
  })
})
