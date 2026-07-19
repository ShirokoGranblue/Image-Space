import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import UserIdentity from '../components/ui/UserIdentity.vue'
import { formatUserHandle, formatUserIdentityText } from '../utils/userIdentity'

describe('user identity presentation', () => {
  it('formats a real username with exactly one at sign', () => {
    expect(formatUserHandle('creator')).toBe('@creator')
    expect(formatUserHandle('@creator')).toBe('@creator')
  })

  it('keeps display name, username and relative time on one identity line', () => {
    const wrapper = mount(UserIdentity, {
      props: { displayName: 'Oxtrendy', username: 'MontondoOr', time: '7小时' },
    })

    expect(wrapper.text()).toContain('Oxtrendy')
    expect(wrapper.text()).toContain('@MontondoOr')
    expect(wrapper.text()).toContain('·')
    expect(wrapper.text()).toContain('7小时')
  })

  it('formats compact text for non-template metadata', () => {
    expect(formatUserIdentityText({ displayName: 'Creator', username: 'creator' })).toBe('Creator @creator')
  })
})
