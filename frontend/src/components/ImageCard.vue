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
      <el-dropdown
        v-if="showActions"
        class="card-actions"
        trigger="click"
        @command="(cmd) => emit(cmd, image.id)"
        @visible-change="(v) => v ? onPopShow() : onPopHide()"
        popper-class="card-action-dropdown"
        @click.stop="() => {}"
      >
        <button class="actions-trigger" type="button">
          <span class="dots">···</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="edit">编辑</el-dropdown-item>
            <el-dropdown-item command="delete">删除</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
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
          <p class="img-tags" v-if="image.tags">{{ image.tags }}</p>
        </div>
      </div>
    </transition>
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
  selected: { type: Boolean, default: false }
})

const emit = defineEmits(['edit', 'delete', 'removed', 'toggle-select'])

const router = useRouter()
const hover = ref(false)
const hoverLocked = ref(false)
const imgFailed = ref(false)

const imageSrc = computed(() => {
  if (imgFailed.value) return ''
  return getImageDownloadUrl(props.image)
})

const visibilityLabel = computed(() => {
  if (props.image.visibility === 'PUBLIC') return '公开'
  if (props.image.visibility === 'SPECIFIED') return '指定用户'
  return '仅自己'
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
function handleImgError() {
  imgFailed.value = true
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
  border-radius: 2px;
  overflow: hidden;
  background: var(--bg-elevated);
  border: 1px solid var(--gray2);
  transition: transform 0.35s var(--ease-out), border-color 0.3s ease;
}

/* Orange bottom bar on hover */
.image-card::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 3px;
  background: var(--accent);
  z-index: 5;
  transition: width 0.35s var(--ease-out);
}

.image-card:hover {
  transform: translateY(-4px);
  border-color: var(--black);
}

.image-card:hover::after {
  width: 100%;
}

.image-card:active {
  transform: translateY(-2px);
}

/* Selected state */
.image-card.selected {
  border-color: var(--accent);
  border-width: 2px;
}

.image-card.selected:hover {
  border-color: var(--accent);
}

/* --- Card frame --- */
.card-frame {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

/* --- Selection toggle (top-right, show on hover) --- */
.select-toggle,
.card-actions {
  position: absolute;
  top: var(--space-sm);
  z-index: 4;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.select-toggle {
  right: calc(var(--space-sm) + 36px);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.65);
  background: rgba(10, 10, 10, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transform: translateY(-4px) scale(0.92);
}

.select-toggle:hover {
  transform: scale(1.08);
  background: var(--text-primary);
  border-color: #fff;
}

.select-toggle.checked {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0) scale(1);
  background: var(--accent);
  border-color: var(--accent);
  box-shadow: none;
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

.image-card:hover .select-toggle,
.image-card:focus-within .select-toggle {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0) scale(1);
}

/* --- "···" dropdown (top-right, show on hover) --- */
.card-actions {
  right: var(--space-sm);
  transform: translateY(-4px);
  line-height: 0;
}

.image-card:hover .card-actions,
.image-card:focus-within .card-actions {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0);
}

.actions-trigger {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.65);
  background: rgba(10, 10, 10, 0.45);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  letter-spacing: 1px;
  transition: background 0.18s ease, border-color 0.18s ease;
}

.actions-trigger:hover {
  background: var(--accent);
  border-color: var(--accent);
}

.dots {
  position: relative;
  top: -2px;
}

/* --- Image --- */
.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s var(--ease-out), filter 0.35s ease;
}

.card-img.zoomed {
  transform: scale(1.06);
  filter: brightness(0.58) saturate(1.05);
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

/* --- Border overlay --- */
.card-border {
  position: absolute;
  inset: 0;
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 2px;
  pointer-events: none;
  transition: border-color 0.3s ease;
}
.image-card:hover .card-border {
  border-color: rgba(0, 0, 0, 0.15);
}

/* --- Overlay (info on hover) --- */
.card-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 14px;
  background: linear-gradient(
    180deg,
    rgba(10, 10, 10, 0.02) 0%,
    rgba(10, 10, 10, 0.15) 46%,
    rgba(10, 10, 10, 0.72) 100%
  );
}

/* --- Badges --- */
.badge-stack {
  position: absolute;
  top: var(--space-sm);
  left: var(--space-sm);
  right: var(--space-sm);
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  z-index: 3;
  pointer-events: none;
}

.category-badge {
  font-size: 11px;
  padding: 4px 9px;
  border-radius: 2px;
  font-weight: 500;
  font-family: var(--font-body);
  background: var(--white);
  color: var(--black);
  border: 1px solid var(--gray2);
}

.visibility-badge {
  font-size: 11px;
  padding: 4px 9px;
  border-radius: 2px;
  font-weight: 500;
  font-family: var(--font-body);
  background: var(--accent);
  color: var(--white);
}

/* --- Info text --- */
.card-info {
  width: 100%;
}

.img-name {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 400;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
  text-shadow: 0 1px 10px rgba(10, 10, 10, 0.28);
}

.img-tags {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.65);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: var(--font-body);
}

/* --- Reveal transitions --- */
.reveal-enter-active { transition: opacity 0.25s var(--ease-out); }
.reveal-enter-from { opacity: 0; }
.reveal-leave-active { transition: opacity 0.12s ease; }
.reveal-leave-to { opacity: 0; }
</style>

<style>
/* Dropdown menu — teleported, so unscoped */
.card-action-dropdown {
  min-width: 100px;
  border-radius: 2px !important;
  border: 1px solid var(--gray2) !important;
  box-shadow: var(--shadow-dialog) !important;
  padding: 4px 0;
}
.card-action-dropdown .el-dropdown-menu__item {
  font-family: var(--font-body);
  font-size: 13px;
  color: var(--black);
  padding: 6px 16px;
  line-height: 1.6;
}
.card-action-dropdown .el-dropdown-menu__item:hover {
  background: var(--gray1);
  color: var(--black);
}
.card-action-dropdown .el-dropdown-menu__item:not(.is-disabled):focus {
  background: var(--gray1);
  color: var(--black);
}
</style>
