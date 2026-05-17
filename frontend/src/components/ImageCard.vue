<template>
  <article class="image-card" tabindex="0" @mouseenter="onMouseEnter" @mouseleave="onMouseLeave"
    @focus="onFocus" @blur="onBlur" @click="goDetail"
    @keydown.enter.prevent="goDetail" @keydown.space.prevent="goDetail"
    role="button" :aria-label="`查看图片: ${image.imageName}`">
    <div class="card-frame">
      <img
        v-if="!imgFailed"
        :src="imageSrc"
        :alt="image.imageName"
        class="card-img"
        :class="{ zoomed: hover }"
        loading="lazy"
        @error="handleImgError"
      />
      <div v-else class="img-fallback">
        <el-icon :size="40"><PictureFilled /></el-icon>
      </div>
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
import { formatSize, formatTime } from '../utils/format'

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
  if (imgFailed.value) return ''
  return props.image.imageUrl || props.image.imagePath || ''
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
function onFocus() {
  if (!hoverLocked.value) hover.value = true
}
function onBlur() {
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

</script>

<style scoped>
.image-card {
  position: relative;
  cursor: pointer;
  aspect-ratio: 1;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
  box-shadow: var(--shadow-sm);
  transition: transform 0.3s var(--ease-out), box-shadow 0.3s var(--ease-out);
}

.image-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.image-card:active {
  transform: translateY(-1px) scale(0.985);
  transition: transform 0.1s var(--ease-out);
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
  transition: transform 0.45s var(--ease-out), filter 0.4s ease;
}

.card-img.zoomed {
  transform: scale(1.06);
  filter: brightness(0.6);
}

/* Fallback when image fails */
.img-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  background: var(--bg-elevated);
}

.card-border {
  position: absolute;
  inset: 0;
  border: 1px solid rgba(255,255,255,0.25);
  border-radius: var(--radius-lg);
  pointer-events: none;
  transition: border-color 0.3s ease;
}
.image-card:hover .card-border {
  border-color: rgba(37, 99, 235, 0.3);
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
    transparent 35%,
    rgba(15,23,42,0.4) 68%,
    rgba(15,23,42,0.85) 100%
  );
}

.badge-stack {
  position: absolute;
  top: var(--space-sm);
  left: var(--space-sm);
  right: var(--space-sm);
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.category-badge,
.visibility-badge {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 999px;
  font-weight: 600;
  font-family: var(--font-body);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}
.category-badge {
  background: rgba(255, 255, 255, 0.88);
  color: var(--text-primary);
}
.visibility-badge {
  background: rgba(37, 99, 235, 0.88);
  color: #fff;
}

.card-info { width: 100%; }

.img-name {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 650;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}

.img-meta {
  font-size: 12px;
  color: rgba(255,255,255,0.55);
  display: flex;
  gap: var(--space-xs);
  font-family: var(--font-body);
}
.meta-divider { opacity: 0.35; }

.card-actions {
  position: absolute;
  top: var(--space-sm);
  right: var(--space-sm);
  display: flex;
  gap: 6px;
}

.action-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.18);
  background: rgba(15,23,42,0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 15px;
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  transition: all 0.2s ease;
}
.action-btn:hover {
  background: rgba(37, 99, 235, 0.9);
  border-color: var(--accent);
  transform: scale(1.08);
}
.action-btn.danger:hover {
  background: rgba(220, 38, 38, 0.85);
  border-color: var(--danger);
}
.action-btn:active {
  transform: scale(0.95);
  transition: transform 0.08s ease;
}

/* Reveal transitions */
.reveal-enter-active { transition: opacity 0.25s var(--ease-out); }
.reveal-enter-from { opacity: 0; }
.reveal-leave-active { transition: opacity 0.12s ease; }
.reveal-leave-to { opacity: 0; }
</style>
