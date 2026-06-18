<template>
  <article
    class="image-card"
    :class="[`variant-${variant}`, { selected }]"
    tabindex="0"
    role="button"
    :aria-label="`查看图片：${image.imageName}`"
    @mouseenter="hover = true"
    @mouseleave="hover = false"
    @focus="hover = true"
    @blur="hover = false"
    @click="goDetail"
    @keydown.enter.prevent="goDetail"
    @keydown.space.prevent="goDetail"
  >
      <div class="card-frame">
        <div class="asset-topline">
        <span class="visibility-chip" :class="visibilityClass">{{ visibilityLabel }}</span>
      </div>

      <button
        v-if="selectable"
        class="select-toggle"
        :class="{ checked: selected }"
        type="button"
        :aria-pressed="String(selected)"
        :title="selected ? '取消选择' : '选择图片'"
        @click.stop="toggleSelect"
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
        decoding="async"
        @error="handleImgError"
      />
      <div v-else class="img-fallback">
        <el-icon :size="38"><PictureFilled /></el-icon>
      </div>

      <div class="card-vignette"></div>

      <transition name="overlay-fade">
        <div class="card-overlay" v-if="hover && !imgFailed">
          <button class="icon-action" type="button" title="查看" @click.stop="goDetail">
            <el-icon><View /></el-icon>
          </button>
          <button v-if="variant === 'square'" class="icon-action" type="button" title="喜欢" @click.stop="emit('like', image)">
            <el-icon><Star /></el-icon>
          </button>
          <button v-if="variant === 'square'" class="icon-action" type="button" title="收藏" @click.stop="emit('favorite', image)">
            <el-icon><Collection /></el-icon>
          </button>
          <button v-if="showActions" class="icon-action" type="button" title="复制链接" @click.stop="emit('copy', image)">
            <el-icon><Link /></el-icon>
          </button>
          <button v-if="showActions" class="icon-action" type="button" title="编辑" @click.stop="emit('edit', image)">
            <el-icon><Edit /></el-icon>
          </button>
          <button v-if="showActions" class="icon-action danger" type="button" title="删除" @click.stop="emit('delete', image)">
            <el-icon><Delete /></el-icon>
          </button>
        </div>
      </transition>
    </div>

    <div class="card-body">
      <p class="img-name" :title="image.imageName">{{ image.imageName }}</p>
      <div class="meta-row">
        <span class="category-badge" v-if="image.categoryName">{{ image.categoryName }}</span>
        <span class="category-badge muted" v-else>未分类</span>
        <span v-if="variant === 'square'" class="meta-text">{{ authorName }}</span>
        <span v-else class="meta-text">{{ fileSizeText }}</span>
      </div>
      <div v-if="variant === 'square'" class="square-foot">
        <span class="meta-text">{{ tagsText || '暂无标签' }}</span>
        <span class="like-text">
          <el-icon><StarFilled /></el-icon>
          {{ likeCount }}
        </span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getImageDisplayUrl } from '../utils/imageRequests'

const props = defineProps({
  image: { type: Object, required: true },
  showActions: { type: Boolean, default: false },
  selectable: { type: Boolean, default: false },
  selected: { type: Boolean, default: false },
  variant: { type: String, default: 'collection' },
  openMode: { type: String, default: 'route' }
})

const emit = defineEmits(['edit', 'delete', 'removed', 'toggle-select', 'view', 'copy', 'like', 'favorite'])

const router = useRouter()
const hover = ref(false)
const imgFailed = ref(false)

const imageSrc = computed(() => imgFailed.value ? '' : getImageDisplayUrl(props.image))
const authorName = computed(() => props.image.displayName || props.image.username || '匿名用户')
const likeCount = computed(() => Number(props.image.likeCount || 0))
const tagsText = computed(() => String(props.image.tags || '').split('#').map(tag => tag.trim()).filter(Boolean).join(' / '))
const fileSizeText = computed(() => {
  const size = Number(props.image.fileSize || 0)
  if (!size) return '--'
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  return `${Math.max(1, Math.round(size / 1024))} KB`
})

const visibilityLabel = computed(() => {
  if (props.image.visibility === 'PUBLIC') return '公开'
  if (props.image.visibility === 'SPECIFIED') return '指定用户'
  return '仅自己'
})
const visibilityClass = computed(() => {
  if (props.image.visibility === 'PUBLIC') return 'public'
  if (props.image.visibility === 'SPECIFIED') return 'specified'
  return 'private'
})

function handleImgError() {
  imgFailed.value = true
}

function toggleSelect() {
  if (!props.image.uuid) return
  emit('toggle-select', props.image.uuid)
}

function goDetail() {
  if (!props.image.uuid) return
  emit('view', props.image)
  if (props.openMode === 'emit') return
  router.push(`/image/${props.image.uuid}`)
}
</script>

<style scoped>
.image-card {
  position: relative;
  min-width: 0;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--ad-line);
  border-radius: 14px;
  background: rgba(21, 25, 34, 0.86);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.18);
  transition: transform 0.2s var(--ad-ease), border-color 0.16s ease, box-shadow 0.2s ease;
  -webkit-user-select: none;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}

.image-card:hover,
.image-card:focus-visible,
.image-card.selected {
  transform: translateY(-2px);
  border-color: rgba(183, 255, 60, 0.5);
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.24), 0 0 0 1px rgba(183, 255, 60, 0.12);
  outline: none;
}

.image-card.selected {
  box-shadow: 0 0 0 3px rgba(183, 255, 60, 0.16), 0 18px 42px rgba(0, 0, 0, 0.26);
}

.card-frame {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(244, 241, 232, 0.08), transparent 38%),
    linear-gradient(135deg, rgba(56, 213, 255, 0.44), rgba(155, 140, 255, 0.34) 48%, rgba(13, 16, 22, 0.96));
}

.variant-square .card-frame {
  aspect-ratio: 4 / 5;
}

.card-frame::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: inherit;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06), transparent 34%);
  pointer-events: none;
}

.asset-topline {
  position: absolute;
  top: 10px;
  left: 10px;
  right: 10px;
  z-index: 3;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.asset-id,
.visibility-chip,
.category-badge,
.like-text {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  background: rgba(7, 10, 15, 0.68);
  color: rgba(244, 241, 232, 0.84);
  font-size: 11px;
  font-weight: 780;
  line-height: 1;
  white-space: nowrap;
}

.visibility-chip.public {
  background: rgba(183, 255, 60, 0.9);
  border-color: rgba(183, 255, 60, 0.92);
  color: #071014;
}

.visibility-chip.private {
  background: rgba(255, 107, 87, 0.9);
  border-color: rgba(255, 107, 87, 0.92);
  color: #fff;
}

.visibility-chip.specified {
  background: rgba(245, 184, 75, 0.92);
  border-color: rgba(245, 184, 75, 0.94);
  color: #17100a;
}

.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.42s var(--ad-ease), filter 0.24s ease;
}

.card-img.zoomed {
  transform: scale(1.025);
  filter: brightness(0.82) saturate(1.02);
}

.img-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  background:
    linear-gradient(180deg, rgba(244, 241, 232, 0.08), transparent 38%),
    linear-gradient(135deg, rgba(56, 213, 255, 0.46), rgba(13, 16, 22, 0.94));
  color: rgba(244, 241, 232, 0.64);
}

.card-vignette {
  position: absolute;
  inset: auto 0 0;
  z-index: 2;
  height: 42%;
  pointer-events: none;
  background: linear-gradient(180deg, transparent, rgba(7, 10, 15, 0.62));
}

.card-overlay {
  position: absolute;
  inset: 0;
  z-index: 4;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(7, 10, 15, 0.5);
}

.icon-action {
  width: 38px;
  height: 38px;
  border: 1px solid var(--ad-line-strong);
  border-radius: 10px;
  background: rgba(21, 25, 34, 0.9);
  color: var(--ad-text);
  display: inline-grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.15s ease, transform 0.1s ease, border-color 0.15s ease, color 0.15s ease;
}

.icon-action:hover {
  background: var(--ad-green);
  border-color: var(--ad-green);
  color: #071014;
}

.icon-action:active {
  transform: scale(0.96);
}

.icon-action.danger:hover {
  background: var(--ad-coral);
  color: #fff;
  border-color: var(--ad-coral);
}

.select-toggle {
  position: absolute;
  top: 44px;
  left: 10px;
  z-index: 5;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  border: 1px solid rgba(244, 241, 232, 0.58);
  background: rgba(7, 10, 15, 0.68);
  cursor: pointer;
  opacity: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.18s ease, background 0.15s ease, border-color 0.15s ease;
}

.image-card:hover .select-toggle,
.image-card:focus-within .select-toggle,
.select-toggle.checked {
  opacity: 1;
}

.select-toggle.checked {
  background: var(--ad-green);
  border-color: var(--ad-green);
}

.select-mark {
  display: none;
}

.select-toggle.checked .select-mark {
  display: block;
  width: 5px;
  height: 9px;
  border-right: 2px solid #071014;
  border-bottom: 2px solid #071014;
  transform: rotate(45deg) translate(-1px, -2px);
}

.card-body {
  display: grid;
  gap: 8px;
  padding: 12px;
}

.img-name {
  margin: 0;
  color: var(--ad-text);
  font-size: 15px;
  font-weight: 820;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-row,
.square-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.category-badge {
  max-width: 70%;
  background: rgba(244, 241, 232, 0.06);
  border-color: var(--ad-line);
  color: var(--ad-text-soft);
  overflow: hidden;
  text-overflow: ellipsis;
}

.category-badge.muted {
  color: var(--ad-muted);
}

.meta-text {
  min-width: 0;
  color: var(--ad-muted);
  font-size: 12px;
  font-weight: 650;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.like-text {
  gap: 4px;
  flex-shrink: 0;
}

.overlay-fade-enter-active,
.overlay-fade-leave-active {
  transition: opacity 0.18s ease;
}
.overlay-fade-enter-from,
.overlay-fade-leave-to {
  opacity: 0;
}
</style>
