import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  replace: vi.fn(),
  page: vi.fn(),
  statistics: vi.fn(),
  eventSource: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: mocks.replace }),
}))

vi.mock('../api/auditLog', () => ({
  createAuditLogEventSource: mocks.eventSource,
  getAuditLogDetail: vi.fn(),
  getAuditLogStatistics: mocks.statistics,
  pageAuditLogs: mocks.page,
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: mocks.messageError },
  ElNotification: vi.fn(),
}))

import AuditLogMonitor from '../views/admin/AuditLogMonitor.vue'

describe('AuditLogMonitor access blocking', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    const forbidden = { response: { status: 403 } }
    mocks.statistics.mockRejectedValue(forbidden)
    mocks.page.mockRejectedValue(forbidden)
  })

  it('navigates once and does not start SSE or polling after a host 403', async () => {
    const wrapper = shallowMount(AuditLogMonitor, {
      global: {
        stubs: {
          'el-tag': true,
          'el-input': true,
          'el-form-item': true,
          'el-option': true,
          'el-select': true,
          'el-date-picker': true,
          'el-form': true,
          'el-button': true,
          'el-table-column': true,
          'el-table': true,
          'el-pagination': true,
          'el-descriptions-item': true,
          'el-descriptions': true,
          'el-drawer': true,
        },
        directives: { loading: {} },
      },
    })
    await flushPromises()

    expect(mocks.replace).toHaveBeenCalledTimes(1)
    expect(mocks.replace).toHaveBeenCalledWith('/403')
    expect(mocks.eventSource).not.toHaveBeenCalled()
    expect(mocks.messageError).not.toHaveBeenCalled()

    vi.advanceTimersByTime(30000)
    await flushPromises()

    expect(mocks.page).toHaveBeenCalledTimes(1)
    expect(mocks.statistics).toHaveBeenCalledTimes(1)
    wrapper.unmount()
    vi.useRealTimers()
  })
})
