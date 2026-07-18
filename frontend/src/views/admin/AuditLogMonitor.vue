<template>
  <main class="audit-page">
    <section class="audit-header">
      <div>
        <p class="section-label">ADMIN / AUDIT</p>
        <h1>审计事件总览</h1>
      </div>
      <el-tag :type="connectionTagType" effect="plain">{{ connectionLabel }}</el-tag>
    </section>

    <section class="stat-grid">
      <article v-for="item in statCards" :key="item.label" class="stat-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="filter-panel">
      <el-form :model="filters" label-position="top" class="filter-form">
        <el-form-item label="用户名">
          <el-input v-model="filters.username" clearable placeholder="username" />
        </el-form-item>
        <el-form-item label="操作模块">
          <el-input v-model="filters.module" clearable placeholder="IMAGE" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-input v-model="filters.operationType" clearable placeholder="IMAGE_DELETE" />
        </el-form-item>
        <el-form-item label="请求状态">
          <el-select v-model="filters.status" clearable placeholder="全部">
            <el-option label="SUCCESS" value="SUCCESS" />
            <el-option label="FAILED" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="filters.riskLevel" clearable placeholder="全部">
            <el-option label="LOW" value="LOW" />
            <el-option label="MEDIUM" value="MEDIUM" />
            <el-option label="HIGH" value="HIGH" />
          </el-select>
        </el-form-item>
        <el-form-item label="IP 地址">
          <el-input v-model="filters.ip" clearable placeholder="127.0.0.1" />
        </el-form-item>
        <el-form-item label="请求路径">
          <el-input v-model="filters.path" clearable placeholder="/image" />
        </el-form-item>
        <el-form-item label="时间范围" class="time-range">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
          />
        </el-form-item>
      </el-form>
      <div class="filter-actions">
        <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </section>

    <section class="table-panel">
      <el-table v-loading="loading" :data="logs" class="audit-table" height="560">
        <el-table-column prop="createTime" label="操作时间" min-width="168" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="userId" label="用户ID" width="92" />
        <el-table-column prop="module" label="操作模块" width="110" />
        <el-table-column prop="operationType" label="操作类型" min-width="150" />
        <el-table-column prop="method" label="方法" width="82" />
        <el-table-column prop="path" label="请求路径" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP 地址" min-width="132" />
        <el-table-column label="状态" width="104">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" effect="plain">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险等级" width="112">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)" effect="plain">{{ row.riskLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="110" />
        <el-table-column label="操作" fixed="right" width="112">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchLogs"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-drawer v-model="detailOpen" title="审计记录详情" size="46%" :lock-scroll="false">
      <el-descriptions v-if="selectedLog" :column="1" border class="detail-descriptions">
        <el-descriptions-item label="用户ID">{{ selectedLog.userId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ selectedLog.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ selectedLog.module || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ selectedLog.operationType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ selectedLog.method || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求路径">{{ selectedLog.path || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ selectedLog.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="User-Agent">{{ selectedLog.userAgent || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ selectedLog.status || '-' }}</el-descriptions-item>
        <el-descriptions-item label="风险等级">{{ selectedLog.riskLevel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="接口耗时">{{ selectedLog.costTime ?? 0 }} ms</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ selectedLog.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="selectedLog" class="detail-blocks">
        <section>
          <h2>请求参数</h2>
          <pre>{{ displayJson(selectedLog.requestParams) }}</pre>
        </section>
        <section>
          <h2>响应结果</h2>
          <pre>{{ displayJson(selectedLog.responseResult) }}</pre>
        </section>
        <section>
          <h2>异常信息</h2>
          <pre>{{ displayJson(selectedLog.errorMsg) }}</pre>
        </section>
      </div>
    </el-drawer>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import {
  createAuditLogEventSource,
  getAuditLogDetail,
  getAuditLogStatistics,
  pageAuditLogs,
} from '../../api/auditLog'
import { maskSensitiveText } from '../../utils/sensitiveMask'

const router = useRouter()

const filters = reactive({
  username: '',
  module: '',
  operationType: '',
  status: '',
  riskLevel: '',
  ip: '',
  path: '',
})
const timeRange = ref([])
const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const statistics = reactive({
  todayOperationCount: 0,
  todayFailedCount: 0,
  highRiskCount: 0,
  averageCostTime: 0,
})
const logs = ref([])
const loading = ref(false)
const detailOpen = ref(false)
const selectedLog = ref(null)
const connectionState = ref('disconnected')
const seenEventIds = new Set()
const seenEventIdQueue = []
const MAX_SEEN_EVENT_IDS = 500

let refreshTimer = null
let reconnectTimer = null
let eventSource = null
let reconnectAttempts = 0
let unmounted = false
let accessBlocked = false

const statCards = computed(() => [
  { label: '今日操作次数', value: statistics.todayOperationCount },
  { label: '今日失败次数', value: statistics.todayFailedCount },
  { label: '高风险操作次数', value: statistics.highRiskCount },
  { label: '平均响应耗时', value: `${statistics.averageCostTime || 0} ms` },
])

const connectionLabel = computed(() => {
  if (connectionState.value === 'connected') return '实时通道已连接'
  if (connectionState.value === 'fallback') return '实时通道不可用，已启用轮询'
  return '实时通道未连接'
})

const connectionTagType = computed(() => {
  if (connectionState.value === 'connected') return 'success'
  if (connectionState.value === 'fallback') return 'warning'
  return 'info'
})

onMounted(async () => {
  await Promise.all([fetchStatistics(), fetchLogs()])
  if (accessBlocked) return
  refreshTimer = window.setInterval(handleRefreshTick, 10000)
  connectSse()
})

onUnmounted(() => {
  unmounted = true
  if (refreshTimer) window.clearInterval(refreshTimer)
  if (reconnectTimer) window.clearTimeout(reconnectTimer)
  closeEventSource()
})

async function fetchStatistics() {
  try {
    const res = await getAuditLogStatistics()
    Object.assign(statistics, res.data || {})
  } catch (error) {
    if (handleAccessBlocked(error)) return
    connectionState.value = connectionState.value === 'connected' ? 'connected' : 'fallback'
  }
}

async function fetchLogs() {
  loading.value = true
  try {
    const params = buildQueryParams()
    const res = await pageAuditLogs(params)
    const page = res.data || {}
    logs.value = page.records || []
    pagination.total = Number(page.total || 0)
  } catch (error) {
    if (handleAccessBlocked(error)) return
    ElMessage.error(error?.message || '审计日志加载失败')
  } finally {
    loading.value = false
  }
}

async function handleRefreshTick() {
  if (accessBlocked || connectionState.value === 'connected') return
  await fetchStatistics()
  if (canRefreshTable()) {
    await fetchLogs()
  }
}

function canRefreshTable() {
  return pagination.pageNum === 1 && !detailOpen.value
}

function handleSearch() {
  pagination.pageNum = 1
  fetchLogs()
  fetchStatistics()
}

function handleReset() {
  Object.assign(filters, {
    username: '',
    module: '',
    operationType: '',
    status: '',
    riskLevel: '',
    ip: '',
    path: '',
  })
  timeRange.value = []
  pagination.pageNum = 1
  fetchLogs()
  fetchStatistics()
}

function handleSizeChange() {
  pagination.pageNum = 1
  fetchLogs()
}

async function openDetail(row) {
  try {
    const res = await getAuditLogDetail(row.id)
    selectedLog.value = res.data || row
    detailOpen.value = true
  } catch {
    selectedLog.value = row
    detailOpen.value = true
  }
}

function buildQueryParams() {
  const [startTime, endTime] = Array.isArray(timeRange.value) ? timeRange.value : []
  return {
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    ...Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== '')),
    startTime,
    endTime,
  }
}

async function connectSse() {
  if (accessBlocked) return
  closeEventSource()
  try {
    eventSource = await createAuditLogEventSource()
    if (unmounted) {
      closeEventSource()
      return
    }
    eventSource.onopen = () => {
      connectionState.value = 'connected'
      reconnectAttempts = 0
    }
    eventSource.addEventListener('audit-risk', handleSseMessage)
    eventSource.onerror = () => {
      connectionState.value = 'fallback'
      closeEventSource()
      scheduleReconnect()
    }
  } catch (error) {
    if (handleAccessBlocked(error)) return
    connectionState.value = 'fallback'
    const status = error?.response?.status
    if (status !== 401 && status !== 403) {
      scheduleReconnect()
    }
  }
}

function scheduleReconnect() {
  if (unmounted || accessBlocked || reconnectTimer) return
  const delay = Math.min(30000, 1000 * (2 ** reconnectAttempts))
  reconnectAttempts += 1
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = null
    connectSse()
  }, delay)
}

async function handleSseMessage(event) {
  const id = event.lastEventId || safeEventId(event.data)
  if (id && seenEventIds.has(id)) return
  if (id) {
    seenEventIds.add(id)
    seenEventIdQueue.push(id)
    if (seenEventIdQueue.length > MAX_SEEN_EVENT_IDS) {
      seenEventIds.delete(seenEventIdQueue.shift())
    }
  }

  const payload = parseEventData(event.data)
  if (payload?.riskLevel === 'HIGH') {
    ElNotification({
      title: '高风险审计事件',
      message: `${payload.username || '未知用户'} ${payload.operationType || payload.action || ''}`,
      type: 'warning',
      position: 'top-right',
    })
  }
  await fetchStatistics()
  if (canRefreshTable()) {
    await fetchLogs()
  }
}

function closeEventSource() {
  if (!eventSource) return
  eventSource.removeEventListener?.('audit-risk', handleSseMessage)
  eventSource.close()
  eventSource = null
}

function handleAccessBlocked(error) {
  if (error?.response?.status !== 403) return false
  if (!accessBlocked) {
    accessBlocked = true
    connectionState.value = 'disconnected'
    if (refreshTimer) window.clearInterval(refreshTimer)
    refreshTimer = null
    if (reconnectTimer) window.clearTimeout(reconnectTimer)
    reconnectTimer = null
    closeEventSource()
    router.replace('/403')
  }
  return true
}

function parseEventData(data) {
  try {
    return JSON.parse(data)
  } catch {
    return null
  }
}

function safeEventId(data) {
  return parseEventData(data)?.id ? String(parseEventData(data).id) : ''
}

function displayJson(value) {
  return maskSensitiveText(value) || '-'
}

function riskTagType(level) {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'success'
}
</script>

<style scoped>
.audit-page { min-height: 100vh; padding: calc(var(--nav-height) + var(--space-5)) var(--page-gutter) var(--space-8); color: var(--color-text-primary); }
.audit-header,.stat-grid,.filter-panel,.table-panel { width: min(100%, var(--page-wide)); margin-inline: auto; }
.audit-header { display: flex; align-items: end; justify-content: space-between; gap: var(--space-4); margin-bottom: var(--space-4); }
.section-label { margin: 0 0 var(--space-2); color: var(--color-vermilion); font-size: var(--text-xs); font-weight: 600; letter-spacing: .12em; }
.audit-header h1 { margin: 0; font-family: var(--font-title); font-size: var(--text-2xl); font-weight: 500; }
.stat-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--space-3); margin-bottom: var(--space-4); }
.stat-card,.filter-panel,.table-panel { border: 1px solid var(--color-border-subtle); border-radius: var(--radius-sm); background: var(--color-surface-1); box-shadow: none; }
.stat-card { min-height: 96px; display: grid; align-content: space-between; padding: var(--space-4); }
.stat-card span { color: var(--color-text-muted); font-size: var(--text-xs); font-weight: 600; }
.stat-card strong { color: var(--color-text-primary); font-family: var(--font-title); font-size: var(--text-xl); line-height: 1; }
.filter-panel { margin-bottom: var(--space-4); padding: var(--space-4); }
.filter-form { display: grid; grid-template-columns: repeat(4, minmax(180px, 1fr)); gap: var(--space-3) var(--space-4); }
.filter-form :deep(.el-form-item) { margin-bottom: 0; }
.filter-form :deep(.el-input__wrapper),.filter-form :deep(.el-select__wrapper),.time-range :deep(.el-date-editor) { min-height: var(--control-height-md); height: var(--control-height-md); }
.filter-form :deep(.el-select),.time-range :deep(.el-date-editor) { width: 100%; }
.filter-actions { display: flex; justify-content: flex-end; gap: var(--space-2); margin-top: var(--space-4); }
.filter-actions :deep(.el-button) { min-height: var(--control-height-md); }
.table-panel { padding: var(--space-3); }
.audit-table { overflow: hidden; border-radius: var(--radius-sm); }
.pagination-row { display: flex; justify-content: flex-end; padding: var(--space-3) var(--space-1) var(--space-1); }
.detail-descriptions { margin-bottom: var(--space-5); }
.detail-blocks { display: grid; gap: var(--space-5); }
.detail-blocks h2 { margin: 0 0 var(--space-2); color: var(--color-text-primary); font-size: var(--text-md); font-weight: 600; }
pre { max-height: 260px; overflow: auto; margin: 0; padding: var(--space-4); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-sm); background: var(--color-viewer-bg); color: var(--color-text-inverse); white-space: pre-wrap; word-break: break-word; font-family: var(--font-mono); font-size: var(--text-xs); line-height: var(--leading-md); }

@media (max-width: 1100px) {
  .stat-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .filter-form { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 640px) {
  .audit-page { padding: calc(var(--nav-height) + var(--space-4)) var(--page-gutter) var(--space-6); }
  .audit-header { align-items: start; flex-direction: column; }
  .stat-grid,.filter-form { grid-template-columns: 1fr; }
  .filter-actions { justify-content: stretch; }
  .filter-actions :deep(.el-button) { flex: 1; min-height: var(--control-height-lg); }
  .filter-form :deep(.el-input__wrapper),.filter-form :deep(.el-select__wrapper),.time-range :deep(.el-date-editor) { min-height: var(--control-height-lg); height: var(--control-height-lg); }
}
</style>
