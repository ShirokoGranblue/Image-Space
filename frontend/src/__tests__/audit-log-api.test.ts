import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../api/index', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

import api from '../api/index'
import {
  getAuditLogDetail,
  getAuditLogStatistics,
  getRecentRiskAuditLogs,
  pageAuditLogs,
  createAuditLogEventSource,
} from '../api/auditLog'

describe('audit log api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads audit log page with query params', () => {
    const params = { pageNum: 2, pageSize: 20, username: 'admin' }

    pageAuditLogs(params)

    expect(api.get).toHaveBeenCalledWith('/admin/audit-log/page', { params })
  })

  it('loads detail, statistics, and recent risk endpoints', () => {
    getAuditLogDetail(7)
    getAuditLogStatistics()
    getRecentRiskAuditLogs()

    expect(api.get).toHaveBeenCalledWith('/admin/audit-log/7')
    expect(api.get).toHaveBeenCalledWith('/admin/audit-log/statistics')
    expect(api.get).toHaveBeenCalledWith('/admin/audit-log/recent-risk')
  })

  it('opens the event stream with a short-lived ticket instead of the login token', async () => {
    vi.mocked(api.post).mockResolvedValue({ data: { ticket: 'single-use-ticket' } })
    const EventSourceMock = vi.fn()
    vi.stubGlobal('EventSource', EventSourceMock)

    await createAuditLogEventSource()

    expect(api.post).toHaveBeenCalledWith('/admin/audit-log/stream-ticket')
    expect(EventSourceMock).toHaveBeenCalledWith(
      '/api/admin/audit-log/stream?ticket=single-use-ticket',
    )
    expect(EventSourceMock.mock.calls[0][0]).not.toContain('satoken')
  })
})
