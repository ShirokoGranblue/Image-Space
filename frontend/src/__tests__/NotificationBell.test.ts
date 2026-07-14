import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import NotificationBell from '../components/NotificationBell.vue'
import { closeNotificationDrawer, setUnreadCount } from '../composables/useNotificationDrawer'

vi.mock('../api/notification', () => ({
  getUnreadNotificationCount: vi.fn(() => Promise.resolve({ data: 0 })),
}))

describe('NotificationBell', () => {
  afterEach(() => {
    setUnreadCount(0)
    closeNotificationDrawer()
  })

  it('has an explicit accessible name and caps the visual badge', () => {
    setUnreadCount(125)
    const wrapper = mount(NotificationBell, { props: { active: false } })

    expect(wrapper.get('button').attributes('aria-label')).toBe('通知，125 条未读')
    expect(wrapper.get('.bell-badge').text()).toBe('99+')
  })

  it('exposes the neutral label when there are no unread notifications', () => {
    const wrapper = mount(NotificationBell, { props: { active: false } })
    expect(wrapper.get('button').attributes('aria-label')).toBe('通知')
  })

  it('restores focus to the trigger after the drawer closes', async () => {
    const wrapper = mount(NotificationBell, { props: { active: false }, attachTo: document.body })
    const trigger = wrapper.get('button')

    await trigger.trigger('click')
    closeNotificationDrawer()
    await new Promise(resolve => window.setTimeout(resolve, 0))

    expect(document.activeElement).toBe(trigger.element)
    wrapper.unmount()
  })
})
