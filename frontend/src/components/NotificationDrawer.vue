<template>
  <Teleport to="body">
    <transition name="notification-slide">
      <aside v-if="state.open" class="notification-drawer" aria-label="通知">
        <header class="notification-header">
          <div>
            <h2>通知</h2>
            <p>{{ unreadText }}</p>
          </div>
          <div class="notification-header-actions">
            <el-button text size="small" @click="handleReadAll" :disabled="unreadCount === 0">全部标为已读</el-button>
            <button class="drawer-close" type="button" @click="close" aria-label="关闭通知">×</button>
          </div>
        </header>

        <div class="notification-list" v-loading="loading">
          <div
            v-for="item in notifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: !item.read }"
          >
            <el-checkbox
              class="notification-checkbox"
              :model-value="selectedIds.has(item.id)"
              :aria-label="`选择通知：${item.imageName || '图片'}`"
              @change="toggleSelect(item.id)"
              @click.stop
            />
            <button
              class="notification-item-content"
              type="button"
              @click="openNotification(item)"
            >
              <img :src="item.imagePreviewUrl || fallbackImage" alt="" class="notification-thumb" loading="lazy" decoding="async" />
              <span class="notification-body">
                <span class="notification-title">
                  <UserIdentity :display-name="item.actorName" :username="item.actorUsername" :time="formatRelativeTime(item.createTime)" />
                  <span>{{ actionText(item) }}</span>
                  <strong>《{{ item.imageName || '图片' }}》</strong>
                </span>
                <span v-if="(item.type === 'COMMENT' || item.type === 'COMMENT_LIKE') && item.contentPreview" class="notification-preview">
                  {{ item.contentPreview }}
                </span>
              </span>
            </button>
          </div>

          <div v-if="!loading && notifications.length === 0" class="notification-empty">
            <span class="empty-icon">◇</span>
            <span>暂无通知</span>
          </div>
        </div>

        <footer class="notification-footer" v-if="notifications.length > 0">
          <el-button
            size="small"
            :disabled="selectedIds.size === 0"
            @click="clearSelection"
          >取消选择</el-button>
          <el-button
            size="small"
            type="danger"
            :disabled="selectedIds.size === 0"
            @click="handleDeleteSelected"
          >删除已选（{{ selectedIds.size }}）</el-button>
        </footer>
      </aside>
    </transition>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, watch, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getNotifications, getUnreadNotificationCount, markAllNotificationsRead, markNotificationRead, deleteNotification, deleteNotifications } from '../api/notification'
import { useNotificationDrawer, closeNotificationDrawer, setUnreadCount } from '../composables/useNotificationDrawer'
import { formatRelativeTime } from '../utils/format'
import UserIdentity from './ui/UserIdentity.vue'

const router = useRouter()
const { state } = useNotificationDrawer()
const notifications = ref([])
const loading = ref(false)
const selectedIds = ref(new Set())
const fallbackImage = 'data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs='
const unreadCount = computed(() => state.unreadCount)
const unreadText = computed(() => unreadCount.value > 0 ? `${unreadCount.value} 条未读` : '暂无未读通知')

watch(() => state.open, async (open) => {
  if (open) {
    await refresh()
  }
}, { immediate: true })

async function refresh() {
  loading.value = true
  try {
    const [listRes, countRes] = await Promise.all([
      getNotifications({ page: 1, limit: 20, unreadOnly: false }),
      getUnreadNotificationCount(),
    ])
    notifications.value = listRes.data?.records || []
    setUnreadCount(countRes.data || 0)
  } catch {
  } finally {
    loading.value = false
  }
}

async function openNotification(item) {
  try {
    if (!item.read) {
      await markNotificationRead(item.id)
      item.read = true
      setUnreadCount(Math.max(0, unreadCount.value - 1))
    }
    close()
    await nextTick()
    const target = item.targetUrl || (item.imageUuid ? `/image/${item.imageUuid}` : '')
    if (target) {
      router.push(target)
    }
  } catch {}
}

async function handleReadAll() {
  try {
    await markAllNotificationsRead()
    notifications.value = notifications.value.map(item => ({ ...item, read: true }))
    setUnreadCount(0)
  } catch {}
}

function close() {
  closeNotificationDrawer()
}

function actionText(item) {
  if (item.type === 'LIKE') return '点赞了你的图片'
  if (item.type === 'COMMENT_LIKE') return '点赞了你的评论'
  return '评论了你的图片'
}

function toggleSelect(id) {
  const next = new Set(selectedIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  selectedIds.value = next
}

function clearSelection() {
  selectedIds.value = new Set()
}

async function handleDeleteSelected() {
  if (selectedIds.value.size === 0) return
  try {
    const ids = [...selectedIds.value]
    if (ids.length === 1) {
      await deleteNotification(ids[0])
    } else {
      await deleteNotifications(ids)
    }
    clearSelection()
    await refresh()
  } catch {}
}
</script>

<style scoped>
.notification-drawer {
  position: fixed;
  top: 0;
  right: 0;
  z-index: var(--layer-notification);
  width: var(--notification-drawer-width, 420px);
  height: 100dvh;
  background: var(--color-surface-1);
  border-left: 1px solid var(--color-border-subtle);
  box-shadow: var(--shadow-dialog);
  display: flex;
  flex-direction: column;
}

.notification-header {
  padding: var(--space-6) var(--space-6) var(--space-4);
  border-bottom: 1px solid var(--color-border-subtle);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.notification-header h2 {
  margin: 0;
  font-family: var(--font-title);
  color: var(--color-text-primary);
  font-size: var(--text-2xl);
  letter-spacing: 0.03em;
  line-height: 1;
  font-weight: 600;
}

.notification-header p {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  font-weight: 500;
}

.notification-header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.drawer-close {
  width: var(--control-height-lg);
  height: var(--control-height-lg);
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  font-size: var(--text-xl);
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color var(--duration-fast) var(--ease-standard), background-color var(--duration-fast) var(--ease-standard);
  padding: 0;
}

.drawer-close:hover {
  background: var(--color-surface-2);
  color: var(--color-text-primary);
}

.notification-list {
  flex: 1;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: stretch;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid var(--color-border-subtle);
  border-left: 2px solid transparent;
  background: transparent;
  transition: background-color var(--duration-fast) var(--ease-standard), border-left-color var(--duration-fast) var(--ease-standard), transform var(--duration-fast) var(--ease-standard);
}

.notification-item:hover {
  background: var(--color-surface-2);
  border-left-color: var(--color-vermilion);
  transform: translateX(-4px);
}

.notification-item.unread {
  background: rgba(17, 26, 53, 0.04);
}

.notification-item.unread:hover {
  background: rgba(17, 26, 53, 0.04);
  border-left-color: var(--color-vermilion);
  transform: translateX(-4px);
}

.notification-checkbox {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  padding: 22px 0;
}

.notification-item-content {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  display: grid;
  grid-template-columns: 64px 1fr;
  gap: var(--space-3);
  padding: 0;
  cursor: pointer;
  text-align: left;
  font: inherit;
  color: inherit;
}

.notification-thumb {
  width: 64px;
  height: 64px;
  object-fit: cover;
  background: var(--color-surface-2);
}

.notification-body {
  min-width: 0;
  display: grid;
  grid-template-rows: auto auto;
  gap: var(--space-1);
  padding: 2px 0;
}

.notification-title {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: .28em;
  color: var(--color-text-primary);
  font-size: var(--text-sm);
  line-height: 1.4;
}

.notification-title :deep(.user-identity) { max-width: 100%; }

.notification-preview {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  line-height: 1.3;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notification-time {
  color: var(--color-text-muted);
  font-size: var(--text-xs);
  white-space: nowrap;
}

.notification-empty {
  padding: var(--space-9) var(--space-6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  color: var(--color-text-muted);
  font-size: var(--text-md);
  font-family: var(--font-body);
}

.empty-icon {
  font-size: 32px;
  line-height: 1;
  opacity: 0.4;
}

.notification-footer {
  padding: var(--space-3) var(--space-6);
  border-top: 1px solid var(--color-border-subtle);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
}

.notification-slide-enter-active,
.notification-slide-leave-active {
  transition: transform var(--duration-overlay) var(--ease-standard), opacity var(--duration-overlay) var(--ease-standard);
}

.notification-slide-enter-from,
.notification-slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
</style>
