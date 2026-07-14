import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import AppShell from '../components/layout/AppShell.vue'
import BaseButton from '../components/ui/BaseButton.vue'
import BaseIconButton from '../components/ui/BaseIconButton.vue'
import LoadingState from '../components/states/LoadingState.vue'
import EmptyState from '../components/states/EmptyState.vue'
import ErrorState from '../components/states/ErrorState.vue'

describe('design foundation', () => {
  it('provides an application shell and stable content target', () => {
    const wrapper = mount(AppShell, {
      props: { width: 'reading' },
      slots: { default: '<p>内容</p>' },
    })

    expect(wrapper.classes()).toContain('app-shell--default')
    expect(wrapper.find('.app-shell__content').attributes('data-width')).toBe('reading')
    expect(wrapper.find('#app-content').attributes('tabindex')).toBe('-1')
    expect(wrapper.find('#app-content').text()).toContain('内容')
  })

  it('prevents BaseButton actions while loading', async () => {
    const wrapper = mount(BaseButton, {
      props: { loading: true },
      slots: { default: '保存' },
    })

    expect(wrapper.get('button').attributes('aria-busy')).toBe('true')
    expect(wrapper.get('button').attributes('disabled')).toBeDefined()
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('requires an accessible name for icon-only actions', () => {
    const wrapper = mount(BaseIconButton, {
      props: { label: '关闭查看器', pressed: false },
      slots: { default: '<span aria-hidden="true">×</span>' },
    })

    expect(wrapper.get('button').attributes('aria-label')).toBe('关闭查看器')
    expect(wrapper.get('button').attributes('aria-pressed')).toBe('false')
  })

  it('exposes loading, empty and error semantics', async () => {
    const loading = mount(LoadingState, { props: { label: '正在读取图片' } })
    const empty = mount(EmptyState, { props: { title: '暂无图片', description: '稍后再来看看' } })
    const error = mount(ErrorState, { props: { description: '请求失败' } })

    expect(loading.get('[role="status"]').text()).toContain('正在读取图片')
    expect(empty.text()).toContain('暂无图片')
    expect(error.get('[role="alert"]').text()).toContain('请求失败')

    await error.get('button').trigger('click')
    expect(error.emitted('retry')).toHaveLength(1)
  })

  it('does not render retry action when no retry label is supplied', () => {
    const wrapper = mount(ErrorState, {
      props: { description: '请求失败', retryLabel: '' },
    })

    expect(wrapper.find('button').exists()).toBe(false)
  })
})
