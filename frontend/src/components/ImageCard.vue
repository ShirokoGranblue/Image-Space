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
  border-radius: 18px;
  overflow: hidden;
  background: #fbfdff;
  border: 1px solid var(--gray2);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  animation: fadeUp 0.34s var(--ease-out);
}

.image-card:hover,
.image-card:focus-visible {
  transform: translateY(-4px);
  border-color: #bfd6e8;
  box-shadow: 0 18px 32px rgba(30, 41, 59, 0.08);
}

.image-card:active {
  transform: translateY(-2px) scale(0.99);
}

.image-card.selected {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(29, 93, 155, 0.12);
}

.card-frame {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: #e8f1fa;
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
  transform: scale(1.045);
  filter: saturate(1.03);
}

.img-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--accent);
  background: linear-gradient(135deg, #e8f1fa, #f7f5ed);
}

.card-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(3, 25, 47, 0.42);
}

.icon-action {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  display: inline-grid;
  place-items: center;
  cursor: pointer;
  transition: transform 0.16s ease, background 0.18s ease, border-color 0.18s ease;
}

.icon-action:hover {
  background: rgba(255, 255, 255, 0.26);
  border-color: rgba(255, 255, 255, 0.42);
}

.icon-action:active {
  transform: scale(0.96);
}

.icon-action.danger:hover {
  background: rgba(214, 80, 80, 0.9);
  border-color: rgba(255, 255, 255, 0.3);
}

.select-toggle {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 3;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.75);
  background: rgba(3, 25, 47, 0.34);
  display: grid;
  place-items: center;
  cursor: pointer;
  opacity: 0;
  transform: translateY(-4px);
  transition: opacity 0.18s ease, transform 0.18s ease, background 0.18s ease;
}

.image-card:hover .select-toggle,
.image-card:focus-within .select-toggle,
.select-toggle.checked {
  opacity: 1;
  transform: translateY(0);
}

.select-toggle.checked {
  background: var(--accent);
  border-color: var(--accent);
}

.select-mark {
  width: 13px;
  height: 13px;
  border-radius: 50%;
  border: 2px solid #fff;
}

.select-toggle.checked .select-mark {
  width: 8px;
  height: 13px;
  border-radius: 0;
  border: 0;
  border-right: 2px solid #fff;
  border-bottom: 2px solid #fff;
  transform: rotate(42deg) translate(-1px, -1px);
}

.card-body {
  padding: 13px 14px 14px;
}

.img-name {
  color: var(--black);
  font-size: 14px;
  font-weight: 400;
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
  margin-top: 8px;
  min-width: 0;
}

.category-badge,
.visibility-badge {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  max-width: 70%;
  height: 24px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 300;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.category-badge {
  color: var(--accent);
  background: var(--blue-soft);
}

.category-badge.muted {
  color: var(--gray3);
  background: #f2f0e8;
}

.visibility-badge {
  flex-shrink: 0;
  color: #6d5fb8;
  background: #eeeaf8;
}

.meta-text,
.like-text {
  min-width: 0;
  color: var(--gray3);
  font-size: 12px;
  font-weight: 300;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.like-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  color: var(--gray4);
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
