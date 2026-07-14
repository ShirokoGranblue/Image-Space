<template>
  <article
    class="image-card gallery-item"
    :class="[`variant-${variant}`, `fit-${fit}`, { selected }]"
  >
    <button
      v-if="selectable"
      class="select-toggle"
      :class="{ checked: selected }"
      type="button"
      :aria-pressed="String(selected)"
      :aria-label="selected ? `取消选择：${imageName}` : `选择图片：${imageName}`"
      @click="toggleSelect"
    >
      <span class="select-mark" aria-hidden="true" />
    </button>

    <div class="gallery-item__media" :style="mediaStyle">
      <button
        class="gallery-item__media-button"
        type="button"
        :aria-label="`查看图片：${imageName}`"
        @click="openImage"
      >
        <span v-if="imageStatus === 'loading'" class="image-placeholder" aria-hidden="true">
          <span class="image-placeholder__line" />
        </span>

        <img
          v-if="imageStatus !== 'error'"
          :src="imageSrc"
          :alt="imageName"
          class="card-img"
          :class="`fit-${fit}`"
          :loading="priority ? 'eager' : 'lazy'"
          :fetchpriority="priority ? 'high' : 'auto'"
          decoding="async"
          @load="handleImgLoad"
          @error="handleImgError"
        />

        <span v-else class="img-fallback" role="img" :aria-label="`图片加载失败：${imageName}`">
          <el-icon :size="32" aria-hidden="true"><PictureFilled /></el-icon>
          <span>图片加载失败</span>
        </span>
      </button>
    </div>

    <div class="card-body">
      <div class="gallery-item__heading">
        <button class="img-name" type="button" :title="imageName" @click="openImage">
          {{ imageName }}
        </button>
        <span v-if="variant !== 'square'" class="visibility-label" :data-visibility="visibilityClass">
          {{ visibilityLabel }}
        </span>
      </div>

      <div class="meta-row">
        <span class="category-label">{{ image.categoryName || '未分类' }}</span>
        <span class="meta-text">{{ variant === 'square' ? authorName : fileSizeText }}</span>
      </div>

      <div v-if="variant === 'square'" class="square-foot">
        <span class="meta-text tags-text">{{ tagsText || '暂无标签' }}</span>
        <button
          class="item-action item-action--like"
          type="button"
          :aria-label="image.likedByMe ? `取消喜欢：${imageName}` : `喜欢图片：${imageName}`"
          :aria-pressed="String(Boolean(image.likedByMe))"
          @click="emit('like', image)"
        >
          <el-icon aria-hidden="true"><Star /></el-icon>
          <span>{{ likeCount }}</span>
        </button>
      </div>

      <div v-if="showActions" class="item-actions" :aria-label="`${imageName} 操作`">
        <button class="item-action" type="button" :aria-label="`复制链接：${imageName}`" @click="emit('copy', image)">
          <el-icon aria-hidden="true"><Link /></el-icon>
          <span>复制</span>
        </button>
        <button class="item-action" type="button" :aria-label="`编辑图片：${imageName}`" @click="emit('edit', image)">
          <el-icon aria-hidden="true"><Edit /></el-icon>
          <span>编辑</span>
        </button>
        <button class="item-action item-action--danger" type="button" :aria-label="`删除图片：${imageName}`" @click="emit('delete', image)">
          <el-icon aria-hidden="true"><Delete /></el-icon>
          <span>删除</span>
        </button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Delete, Edit, Link, PictureFilled, Star } from '@element-plus/icons-vue'
import { getImageAlt, getImageDisplayUrl } from '../../utils/imageRequests'

const props = defineProps({
  image: { type: Object, required: true },
  showActions: { type: Boolean, default: false },
  selectable: { type: Boolean, default: false },
  selected: { type: Boolean, default: false },
  variant: { type: String, default: 'collection', validator: value => ['collection', 'square'].includes(value) },
  openMode: { type: String, default: 'route', validator: value => ['route', 'emit'].includes(value) },
  fit: { type: String, default: 'contain', validator: value => ['contain', 'cover'].includes(value) },
  priority: { type: Boolean, default: false },
})

const emit = defineEmits(['edit', 'delete', 'removed', 'toggle-select', 'view', 'copy', 'like'])
const router = useRouter()

const imageSrc = computed(() => getImageDisplayUrl(props.image))
const imageStatus = ref(imageSrc.value ? 'loading' : 'error')
const imageName = computed(() => getImageAlt(props.image))
const authorName = computed(() => props.image.displayName || props.image.username || '匿名用户')
const likeCount = computed(() => Number(props.image.likeCount || 0))
const tagsText = computed(() => String(props.image.tags || '').split('#').map(tag => tag.trim()).filter(Boolean).join(' / '))
const fileSizeText = computed(() => {
  const size = Number(props.image.fileSize || props.image.originalSize || 0)
  if (!size) return '大小未知'
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
const mediaStyle = computed(() => {
  const width = Number(props.image.width)
  const height = Number(props.image.height)
  const ratio = width > 0 && height > 0 ? `${width} / ${height}` : '4 / 3'
  return { '--image-aspect-ratio': ratio }
})

watch(imageSrc, nextSrc => {
  imageStatus.value = nextSrc ? 'loading' : 'error'
})

function handleImgLoad() {
  imageStatus.value = 'loaded'
}

function handleImgError() {
  imageStatus.value = 'error'
}

function toggleSelect() {
  if (props.image.uuid) emit('toggle-select', props.image.uuid)
}

function openImage() {
  if (!props.image.uuid) return
  emit('view', props.image)
  if (props.openMode === 'route') router.push(`/image/${props.image.uuid}`)
}
</script>

<style scoped>
.gallery-item {
  position: relative;
  min-width: 0;
  align-self: start;
  overflow: hidden;
  border-block: 1px solid var(--color-border-subtle);
  background: transparent;
  color: var(--color-text-primary);
  transition: border-color var(--duration-fast) var(--ease-standard);
}

.gallery-item:hover,
.gallery-item:focus-within,
.gallery-item.selected {
  border-color: var(--color-border-strong);
}

.gallery-item.selected {
  box-shadow: inset 0 0 0 2px var(--color-urban);
}

.gallery-item__media {
  position: relative;
  aspect-ratio: var(--image-aspect-ratio, 4 / 3);
  min-height: 1px;
  overflow: hidden;
  background: #e2e2df;
  isolation: isolate;
}

.gallery-item__media-button {
  width: 100%;
  height: 100%;
  display: block;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: inherit;
  cursor: zoom-in;
}

.card-img {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  display: block;
  background: #e2e2df;
  transition: opacity var(--duration-standard) var(--ease-standard), transform var(--duration-standard) var(--ease-standard);
}

.card-img.fit-contain { object-fit: contain; }
.card-img.fit-cover { object-fit: cover; }

@media (hover: hover) and (pointer: fine) {
  .gallery-item:hover .card-img { transform: scale(1.01); }
  .select-toggle { opacity: 0; transform: translateY(-2px); transition: opacity var(--duration-fast) var(--ease-standard), transform var(--duration-fast) var(--ease-standard); }
  .gallery-item:hover .select-toggle,
  .gallery-item:focus-within .select-toggle,
  .select-toggle.checked { opacity: 1; transform: none; }
}

.image-placeholder {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: var(--color-canvas-muted);
}

.image-placeholder::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(248, 245, 238, 0.7), transparent);
  transform: translateX(-100%);
  animation: gallery-shimmer 1.2s var(--ease-standard) infinite;
}

.image-placeholder__line {
  width: 42%;
  height: 1px;
  background: var(--color-border-strong);
}

.img-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  background: #dededb;
  color: var(--color-text-secondary);
  font-family: var(--font-ui);
  font-size: var(--text-sm);
}

.select-toggle {
  position: absolute;
  top: var(--space-2);
  left: var(--space-2);
  z-index: 5;
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  background: rgba(248, 245, 238, 0.92);
  color: var(--color-text-primary);
  cursor: pointer;
}

.select-toggle.checked {
  border-color: var(--color-night);
  background: var(--color-night);
}

.select-mark {
  width: 12px;
  height: 12px;
  border: 1px solid currentColor;
}

.select-toggle.checked .select-mark {
  width: 7px;
  height: 12px;
  border: 0;
  border-right: 2px solid var(--color-text-inverse);
  border-bottom: 2px solid var(--color-text-inverse);
  transform: rotate(45deg) translate(-1px, -1px);
}

.card-body {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-3) 0 var(--space-4);
}

.gallery-item__heading,
.meta-row,
.square-foot,
.item-actions {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.img-name {
  min-width: 0;
  min-height: 40px;
  display: flex;
  align-items: center;
  overflow: hidden;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-text-primary);
  font-family: var(--font-ui);
  font-size: var(--text-sm);
  font-weight: 650;
  line-height: var(--leading-sm);
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.visibility-label,
.category-label,
.meta-text {
  min-width: 0;
  overflow: hidden;
  color: var(--color-text-secondary);
  font-family: var(--font-ui);
  font-size: var(--text-xs);
  line-height: var(--leading-xs);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.visibility-label {
  flex: 0 0 auto;
  padding-left: var(--space-2);
  border-left: 2px solid var(--color-border-strong);
}

.visibility-label[data-visibility='public'] { border-color: var(--color-success); }
.visibility-label[data-visibility='specified'] { border-color: var(--color-warning); }
.visibility-label[data-visibility='private'] { border-color: var(--color-error); }
.tags-text { max-width: 72%; }

.item-actions {
  justify-content: flex-start;
  padding-top: var(--space-2);
  border-top: 1px solid var(--color-border-subtle);
}

.item-action {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  padding: 0 var(--space-2);
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-secondary);
  font-family: var(--font-ui);
  font-size: var(--text-xs);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-standard), color var(--duration-fast) var(--ease-standard), border-color var(--duration-fast) var(--ease-standard);
}

.item-action:hover,
.item-action:focus-visible,
.item-action[aria-pressed='true'] {
  border-color: var(--color-border-subtle);
  background: var(--color-surface-2);
  color: var(--color-text-primary);
}

.item-action--danger { margin-left: auto; color: var(--color-error); }
.item-action--danger:hover { border-color: var(--color-error); background: var(--color-error); color: var(--color-text-inverse); }
.item-action--like { flex: 0 0 auto; min-height: 40px; }

@media (hover: hover) and (pointer: fine) {
  .item-actions {
    opacity: 0.34;
    transition: opacity var(--duration-fast) var(--ease-standard);
  }
  .gallery-item:hover .item-actions,
  .gallery-item:focus-within .item-actions { opacity: 1; }
}

@media (max-width: 767px) {
  .select-toggle { width: 44px; height: 44px; }
  .img-name { min-height: 44px; }
  .item-action { min-height: 44px; padding-inline: var(--space-3); }
  .card-body { padding-bottom: var(--space-5); }
}

@keyframes gallery-shimmer {
  to { transform: translateX(100%); }
}

@media (prefers-reduced-motion: reduce) {
  .card-img { transition: none; }
  .image-placeholder::after { animation: none; }
}
</style>
