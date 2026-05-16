<template>
  <article class="image-card" @mouseenter="onMouseEnter" @mouseleave="onMouseLeave" @click="goDetail">
    <div class="card-frame">
      <img
        :src="imageSrc"
        :alt="image.imageName"
        class="card-img"
        :class="{ zoomed: hover }"
        loading="lazy"
        @error="handleImgError"
      />
      <div class="card-border"></div>
    </div>
    <transition name="reveal">
      <div class="card-overlay" v-if="hover && !imgFailed">
        <div class="badge-stack">
          <span class="category-badge" v-if="image.categoryName">{{ image.categoryName }}</span>
          <span class="visibility-badge" v-if="showActions">{{ visibilityLabel }}</span>
        </div>
        <div class="card-info">
          <p class="img-name">{{ image.imageName }}</p>
          <div class="img-meta">
            <span>{{ formatSize(image.fileSize) }}</span>
            <span class="meta-divider">·</span>
            <span>{{ formatTime(image.uploadTime) }}</span>
          </div>
        </div>
        <div class="card-actions" v-if="showActions" @click.stop>
          <button class="action-btn" @click.stop="$emit('edit', image)" title="编辑">
            <el-icon><Edit /></el-icon>
          </button>
          <el-popconfirm
            title="确定删除？"
            @show="onPopShow"
            @hide="onPopHide"
            @confirm="$emit('delete', image.id)"
          >
            <template #reference>
              <button class="action-btn danger" @click.stop title="删除">
                <el-icon><Delete /></el-icon>
              </button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </transition>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  image: { type: Object, required: true },
  showActions: { type: Boolean, default: false }
})

defineEmits(['edit', 'delete'])

const router = useRouter()
const hover = ref(false)
const hoverLocked = ref(false)
const imgFailed = ref(false)

const imageSrc = computed(() => {
  if (imgFailed.value || !props.image.imagePath) return ''
  return props.image.imagePath
})

const visibilityLabel = computed(() => {
  const map = {
    PRIVATE: '仅自己',
    PUBLIC: '公开',
    SPECIFIED: '指定用户'
  }
  return map[props.image.visibility] || '仅自己'
})

function onMouseEnter() { hover.value = true }
function onMouseLeave() {
  if (!hoverLocked.value) hover.value = false
}
function onPopShow() { hoverLocked.value = true }
function onPopHide() {
  hoverLocked.value = false
  hover.value = false
}
function handleImgError() { imgFailed.value = true }
function goDetail() {
  if (hoverLocked.value) return
  router.push(`/image/${props.image.id}`)
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 10)
}
</script>

<style scoped>
.image-card {
  position: relative;
  cursor: pointer;
  aspect-ratio: 1;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
  transition: transform 0.35s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.image-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.1);
}

.card-frame {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s cubic-bezier(0.25, 0.1, 0.25, 1), filter 0.5s ease;
}

.card-img.zoomed {
  transform: scale(1.06);
  filter: brightness(0.65);
}

.card-border {
  position: absolute;
  inset: 0;
  border: 1px solid rgba(255,255,255,0.28);
  border-radius: var(--radius-md);
  pointer-events: none;
  transition: border-color 0.35s ease;
}

.image-card:hover .card-border {
  border-color: rgba(37, 99, 235, 0.35);
}

.card-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: var(--space-md);
  background: linear-gradient(
    180deg,
    transparent 40%,
    rgba(15,23,42,0.42) 70%,
    rgba(15,23,42,0.82) 100%
  );
}

.badge-stack {
  position: absolute;
  top: var(--space-md);
  left: var(--space-md);
  right: var(--space-md);
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.category-badge,
.visibility-badge {
  background: rgba(255, 255, 255, 0.88);
  color: var(--text-primary);
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 999px;
  font-weight: 600;
  letter-spacing: 0;
  text-transform: none;
  font-family: var(--font-body);
  backdrop-filter: blur(6px);
}

.visibility-badge {
  background: rgba(37, 99, 235, 0.9);
  color: #fff;
}

.card-info {
  width: 100%;
}

.img-name {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 650;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
  letter-spacing: 0;
}

.img-meta {
  font-size: 12px;
  color: rgba(255,255,255,0.6);
  display: flex;
  gap: var(--space-xs);
  font-family: var(--font-body);
}

.meta-divider {
  opacity: 0.4;
}

.card-actions {
  position: absolute;
  top: var(--space-md);
  right: var(--space-md);
  display: flex;
  gap: var(--space-xs);
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.2);
  background: rgba(15,23,42,0.48);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  backdrop-filter: blur(4px);
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: rgba(37, 99, 235, 0.92);
  border-color: var(--accent);
  color: #fff;
}

.action-btn.danger:hover {
  background: rgba(196,92,74,0.7);
  border-color: var(--danger);
  color: #fff;
}

.reveal-enter-active {
  transition: opacity 0.3s ease;
}

.reveal-enter-from {
  opacity: 0;
}

.reveal-leave-active {
  transition: opacity 0.15s ease;
}

.reveal-leave-to {
  opacity: 0;
}
</style>
