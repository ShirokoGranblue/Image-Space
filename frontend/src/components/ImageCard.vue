<template>
  <article
    class="image-card"
    :class="[`variant-${variant}`, { selected }]"
    tabindex="0"
    @mouseenter="hover = true"
    @mouseleave="hover = false"
    @focus="hover = true"
    @blur="hover = false"
    @click="goDetail"
    @keydown.enter.prevent="goDetail"
    @keydown.space.prevent="goDetail"
    role="button"
    :aria-label="`查看图片: ${image.imageName}`"
  >
    <div class="card-frame">
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
        @error="handleImgError"
      />
      <div v-else class="img-fallback">
        <el-icon :size="40"><PictureFilled /></el-icon>
      </div>

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
        <span v-else class="visibility-badge">{{ visibilityLabel }}</span>
      </div>
      <div v-if="variant === 'square'" class="square-foot">
        <span class="meta-text">{{ tagsText || '无标签' }}</span>
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
import { getImageDownloadUrl } from '../utils/imageRequests'

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

const imageSrc = computed(() => imgFailed.value ? '' : getImageDownloadUrl(props.image))
const authorName = computed(() => props.image.displayName || props.image.username || 'Unknown')
const likeCount = computed(() => Number(props.image.likeCount || 0))
const tagsText = computed(() => String(props.image.tags || '').split('#').map(tag => tag.trim()).filter(Boolean).join(' / '))

const visibilityLabel = computed(() => {
  if (props.image.visibility === 'PUBLIC') return '公开'
  if (props.image.visibility === 'SPECIFIED') return '指定用户'
  return '仅自己'
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
  cursor: pointer;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  border: 0.5px solid var(--paper3);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04), 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: border-color .2s, transform .2s, box-shadow .3s;
  animation: fadeUp 0.34s var(--ease-out);
}

.image-card:hover,
.image-card:focus-visible {
  border-color: var(--ink4);
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08), 0 4px 8px rgba(0, 0, 0, 0.04);
}

.image-card:active {
  transform: translateY(-1px) scale(0.99);
}

.image-card.selected {
  border-color: var(--ink4);
}

.card-frame {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--paper);
}

.variant-square .card-frame {
  aspect-ratio: 1 / 1;
}

.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.42s var(--ease-out), filter 0.24s ease;
}

.card-img.zoomed {
  transform: scale(1.04);
}

.img-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--ink3);
  background: var(--paper3);
}

.card-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(4, 44, 83, 0.6);
  border-radius: 10px;
}

.icon-action {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.2);
  border: 0.5px solid rgba(255, 255, 255, 0.35);
  color: #fff;
  display: inline-grid;
  place-items: center;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.15s, transform 0.1s;
}

.icon-action:hover {
  background: rgba(255, 255, 255, 0.35);
}

.icon-action:active {
  transform: scale(0.95);
}

.icon-action.danger:hover {
  background: rgba(214, 80, 80, 0.9);
  border-color: rgba(255, 255, 255, 0.3);
}

.select-toggle {
  position: absolute;
  top: 7px;
  left: 7px;
  z-index: 3;
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1.5px solid rgba(255, 255, 255, 0.6);
  background: transparent;
  cursor: pointer;
  opacity: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s, transform 0.2s, background 0.15s, border-color 0.15s;
}

.image-card:hover .select-toggle,
.image-card:focus-within .select-toggle,
.select-toggle.checked {
  opacity: 1;
}

.select-toggle.checked {
  background: var(--ink);
  border-color: var(--ink);
}

.select-mark {
  display: none;
}

.select-toggle.checked .select-mark {
  display: block;
  width: 4px;
  height: 8px;
  border-radius: 0;
  border: 0;
  border-right: 2px solid #fff;
  border-bottom: 2px solid #fff;
  transform: rotate(45deg) translate(-1px, -2px);
}

.card-body {
  padding: 8px 10px;
}

.img-name {
  color: var(--ink);
  font-size: 16px;
  font-weight: 600;
  line-height: 1.35;
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
  margin-top: 4px;
  min-width: 0;
}

.category-badge,
.visibility-badge {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  max-width: 70%;
  height: 18px;
  padding: 0 6px;
  border-radius: 4px;
  font-size: 15px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.category-badge {
  color: var(--ink3);
  background: #fff;
  border: 0.5px solid var(--paper3);
}

.category-badge.muted {
  color: var(--ink3);
  background: var(--paper);
  border: 0.5px solid var(--paper3);
}

.visibility-badge {
  flex-shrink: 0;
  color: var(--ink3);
  background: var(--ink7);
}

.meta-text,
.like-text {
  min-width: 0;
  color: var(--ink3);
  font-size: 15px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.like-text {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  flex-shrink: 0;
  color: var(--ink3);
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
