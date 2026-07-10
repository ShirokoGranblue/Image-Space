import api from './index'

export interface AuditLogQuery {
  pageNum?: number
  pageSize?: number
  username?: string
  module?: string
  operationType?: string
  status?: string
  riskLevel?: string
  ip?: string
  path?: string
  startTime?: string
  endTime?: string
}

export function pageAuditLogs(params: AuditLogQuery = {}) {
  return api.get('/admin/audit-log/page', { params })
}

export function getAuditLogDetail(id: number | string) {
  return api.get(`/admin/audit-log/${id}`)
}

export function getAuditLogStatistics() {
  return api.get('/admin/audit-log/statistics')
}

export function getRecentRiskAuditLogs() {
  return api.get('/admin/audit-log/recent-risk')
}

export async function createAuditLogEventSource() {
  const response = await api.post('/admin/audit-log/stream-ticket')
  const ticket = response.data?.ticket
  if (!ticket) {
    throw new Error('审计流凭证签发失败')
  }
  return new EventSource(
    `/api/admin/audit-log/stream?ticket=${encodeURIComponent(ticket)}`,
  )
}
