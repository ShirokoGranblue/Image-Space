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
  border-radius: 16px;
  overflow: hidden;
  background: rgba(255, 253, 248, 0.9);
  border: 1px solid rgba(255, 253, 248, 0.6);
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.16);
  transition: border-color .22s ease, transform .24s var(--ease-cinema), box-shadow .28s ease, background .22s ease;
  animation: fadeUp 0.42s var(--ease-cinema);
  backdrop-filter: blur(14px);
}

.image-card:hover,
.image-card:focus-visible {
  border-color: rgba(239, 159, 39, 0.5);
  transform: translateY(-5px);
  box-shadow: 0 28px 72px rgba(0, 0, 0, 0.26);
}

.image-card:active {
  transform: translateY(-2px) scale(0.99);
}

.image-card.selected {
  border-color: rgba(239, 159, 39, 0.86);
  box-shadow: 0 0 0 3px rgba(239, 159, 39, 0.14), 0 24px 62px rgba(0, 0, 0, 0.24);
}

.card-frame {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--cinema2);
}

.variant-square .card-frame {
  aspect-ratio: 4 / 5;
}

.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.56s var(--ease-cinema), filter 0.28s ease;
}

.card-img.zoomed {
  transform: scale(1.06);
  filter: brightness(0.76) saturate(1.05);
}

.img-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 50% 24%, rgba(55, 138, 221, 0.28), transparent 42%),
    var(--cinema2);
  color: rgba(247, 243, 232, 0.64);
}

.card-vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(180deg, transparent 52%, rgba(7, 17, 31, 0.42));
  opacity: 0.5;
  transition: opacity 0.24s ease;
}

.image-card:hover .card-vignette,
.image-card:focus-visible .card-vignette {
  opacity: 0.86;
}

.card-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: radial-gradient(circle at 50% 44%, rgba(7, 17, 31, 0.12), rgba(7, 17, 31, 0.68));
}

.icon-action {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: rgba(255, 253, 248, 0.14);
  border: 1px solid rgba(255, 253, 248, 0.28);
  color: var(--paper);
  display: inline-grid;
  place-items: center;
  cursor: pointer;
  font-size: 14px;
  backdrop-filter: blur(10px);
  transition: background 0.15s, transform 0.1s, border-color 0.15s, color 0.15s;
}

.icon-action:hover {
  background: var(--paper);
  color: var(--cinema);
  border-color: rgba(255, 253, 248, 0.7);
}

.icon-action:active {
  transform: scale(0.95);
}

.icon-action.danger:hover {
  background: rgba(214, 80, 80, 0.92);
  color: #fff;
  border-color: rgba(255, 255, 255, 0.3);
}

.select-toggle {
  position: absolute;
  top: 9px;
  left: 9px;
  z-index: 3;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  border: 1px solid rgba(255, 253, 248, 0.72);
  background: rgba(7, 17, 31, 0.42);
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
  background: var(--gold2);
  border-color: var(--gold2);
}

.select-mark {
  display: none;
}

.select-toggle.checked .select-mark {
  display: block;
  width: 5px;
  height: 9px;
  border: 0;
  border-right: 2px solid #fff;
  border-bottom: 2px solid #fff;
  transform: rotate(45deg) translate(-1px, -2px);
}

.card-body {
  padding: 12px 13px 13px;
}

.img-name {
  color: var(--ink);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: 0.01em;
}

.meta-row,
.square-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 7px;
  min-width: 0;
}

.category-badge,
.visibility-badge {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  max-width: 70%;
  min-height: 20px;
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.category-badge {
  color: var(--ink);
  background: rgba(230, 241, 251, 0.78);
  border: 1px solid rgba(4, 44, 83, 0.08);
}

.category-badge.muted {
  color: var(--ink3);
  background: rgba(247, 243, 232, 0.8);
  border: 1px solid rgba(4, 44, 83, 0.08);
}

.visibility-badge {
  flex-shrink: 0;
  color: var(--cinema);
  background: rgba(239, 159, 39, 0.2);
}

.meta-text,
.like-text {
  min-width: 0;
  color: var(--ink3);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.like-text {
  display: inline-flex;
  align-items: center;
  gap: 3px;
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

/* Anime paper override */
.image-card {
  background: #f0eee6;
  border-color: rgba(17, 26, 53, 0.08);
  box-shadow: 0 18px 44px rgba(17, 26, 53, 0.12);
  -webkit-user-select: none;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}

.image-card:hover,
.image-card:focus-visible {
  border-color: rgba(255, 122, 184, 0.56);
  transform: translateY(-5px) rotate(-0.35deg);
  box-shadow: 0 28px 72px rgba(88, 184, 255, 0.18), 0 12px 28px rgba(17, 26, 53, 0.18);
}

.image-card.selected {
  border-color: rgba(255, 122, 184, 0.92);
  box-shadow: 0 0 0 4px rgba(255, 238, 142, 0.34), 0 24px 62px rgba(88, 184, 255, 0.22);
}

.card-frame {
  background: linear-gradient(135deg, #fff4de, #f5ecff);
}

.card-vignette {
  background:
    radial-gradient(circle at 82% 14%, rgba(255,255,255,0.38), transparent 23%),
    linear-gradient(180deg, transparent 52%, rgba(17, 26, 53, 0.5));
}

.card-body {
  background: #f0eee6;
}

.meta-row .meta-text,
.square-foot .meta-text {
  display: none;
}

.square-foot {
  justify-content: flex-end;
}

.category-badge {
  background: rgba(88, 184, 255, 0.16);
  border-color: rgba(88, 184, 255, 0.22);
}

.visibility-badge {
  background: rgba(255, 122, 184, 0.18);
}

.like-text {
  color: var(--cinema);
}
</style>
