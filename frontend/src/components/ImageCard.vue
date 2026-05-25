<template>
  <article class="image-card" :class="{ selected }" tabindex="0" @mouseenter="onMouseEnter" @mouseleave="onMouseLeave"
    @focus="onFocus" @blur="onBlur" @click="goDetail"
    @keydown.enter.prevent="goDetail" @keydown.space.prevent="goDetail"
    role="button" :aria-label="`查看图片: ${image.imageName}`">
    <div class="card-frame">
      <button
        v-if="selectable"
        class="select-toggle"
        :class="{ checked: selected }"
        type="button"
        :aria-pressed="String(selected)"
        :title="selected ? '取消选择' : '选择图片'"
        @click.stop="emit('toggle-select', image.id)"
      >
        <span class="select-mark"></span>
      </button>
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
import { ElMessageBox } from 'element-plus'

const props = defineProps({
  image: { type: Object, required: true },
  showActions: { type: Boolean, default: false },
  selectable: { type: Boolean, default: false },
  selected: { type: Boolean, default: false }
})

const emit = defineEmits(['edit', 'delete', 'removed', 'toggle-select'])

const router = useRouter()
const hover = ref(false)
const hoverLocked = ref(false)
const imgFailed = ref(false)

const imageSrc = computed(() => {
  if (imgFailed.value) return ''
  return props.image.id ? `/api/image/download/${props.image.id}` : ''
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
async function handleImgError() {
  imgFailed.value = true
  try {
    await ElMessageBox.confirm(
      '该图片似乎已损坏或为空，是否立即删除？',
      '图片加载失败',
      { confirmButtonText: '删除', cancelButtonText: '保留', type: 'warning' }
    )
    emit('delete', props.image.id)
  } catch {
    // user chose to keep
  }
}
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
  border-radius: 14px;
  overflow: hidden;
  background: var(--bg-elevated);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: var(--shadow-sm);
  transition: transform 0.28s var(--ease-out), box-shadow 0.28s var(--ease-out), border-color 0.28s ease;
}

.image-card:hover {
  transform: translateY(-5px);
  border-color: rgba(37, 99, 235, 0.24);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.11);
}

.image-card:active {
  transform: translateY(-1px) scale(0.985);
  transition: transform 0.1s var(--ease-out);
}
.image-card.selected {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.14), 0 18px 36px rgba(15, 23, 42, 0.11);
}

.card-frame {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.select-toggle {
  position: absolute;
  right: var(--space-sm);
  bottom: var(--space-sm);
  z-index: 4;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.78);
  background: rgba(15, 23, 42, 0.58);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  pointer-events: none;
  transform: translateY(-4px);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  transition: opacity 0.18s ease, transform 0.18s ease, background 0.18s ease, border-color 0.18s ease;
}
.image-card:hover .select-toggle,
.image-card:focus-within .select-toggle {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0);
}
.select-toggle:hover {
  transform: translateY(0) scale(1.08);
  background: rgba(37, 99, 235, 0.82);
  border-color: #fff;
}
.select-toggle.checked {
  background: var(--accent);
  border-color: #fff;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.26);
}
.select-mark {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #fff;
  position: relative;
}
.select-toggle.checked .select-mark {
  border: 0;
}
.select-toggle.checked .select-mark::after {
  content: '';
  position: absolute;
  left: 3px;
  top: 0;
  width: 6px;
  height: 10px;
  border-right: 2px solid #fff;
  border-bottom: 2px solid #fff;
  transform: rotate(42deg);
}

.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s var(--ease-out), filter 0.32s ease;
}

.card-img.zoomed {
  transform: scale(1.055);
  filter: brightness(0.72) saturate(1.05);
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
  border-radius: 14px;
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
  padding: 14px;
  background: linear-gradient(
    180deg,
    rgba(15,23,42,0.04) 0%,
    rgba(15,23,42,0.18) 46%,
    rgba(15,23,42,0.78) 100%
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
  padding: 4px 9px;
  border-radius: 7px;
  font-weight: 600;
  font-family: var(--font-body);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}
.category-badge {
  background: rgba(255, 255, 255, 0.9);
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
  text-shadow: 0 1px 10px rgba(15, 23, 42, 0.28);
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
  border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.18);
  background: rgba(15,23,42,0.52);
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
