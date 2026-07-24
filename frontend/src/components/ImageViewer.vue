<template>
  <teleport to="body">
    <transition name="viewer-fade">
      <div
        v-if="visible"
        ref="overlayRef"
        class="viewer-overlay"
        role="dialog"
        aria-modal="true"
        :aria-label="`图片查看器：${activeTitle}`"
        tabindex="-1"
        @click.self="close"
        @keydown="onKeydown"
        @mousemove="showControls"
        @pointermove="showControls"
      >
        <header class="viewer-header" :class="{ 'is-hidden': !controlsVisible }">
          <div class="viewer-heading">
            <span v-if="hasMultiple" class="viewer-position">{{ activeIndex + 1 }} / {{ normalizedItems.length }}</span>
            <strong>{{ activeTitle }}</strong>
          </div>
          <div class="viewer-header-actions">
            <button
              v-if="activeMetadata.length"
              class="viewer-icon-button"
              type="button"
              :aria-pressed="String(metadataOpen)"
              aria-label="查看图片信息"
              @click="metadataOpen = !metadataOpen"
            ><el-icon><InfoFilled /></el-icon></button>
            <button
              class="viewer-icon-button"
              type="button"
              :aria-label="fullscreen ? '退出全屏' : '进入全屏'"
              :aria-pressed="String(fullscreen)"
              @click="toggleFullscreen"
            >
              <el-icon><FullScreen /></el-icon>
            </button>
            <button class="viewer-icon-button viewer-close" type="button" aria-label="关闭查看器" @click="close">
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </header>

        <button
          v-if="hasMultiple"
          class="viewer-nav viewer-nav--previous"
          :class="{ 'is-hidden': !controlsVisible }"
          type="button"
          aria-label="查看上一张图片"
          @click="previous"
        ><el-icon><ArrowLeft /></el-icon></button>

        <main
          ref="stageRef"
          class="viewer-stage"
          @click.self="close"
          @wheel.prevent="onWheel"
          @pointerdown="startPointer"
          @pointermove="movePointer"
          @pointerup="endPointer"
          @pointercancel="endPointer"
        >
          <div v-if="imageStatus === 'loading'" class="viewer-loading" role="status" aria-live="polite">
            <span class="viewer-loading-mark" aria-hidden="true" />
            <span>正在载入原图</span>
          </div>

          <div v-if="imageStatus === 'error'" class="viewer-error" role="alert">
            <el-icon aria-hidden="true"><PictureFilled /></el-icon>
            <strong>图片无法显示</strong>
            <span>原图加载失败，请检查连接后重试。</span>
            <button type="button" @pointerdown.stop @click.stop="retryImage">重新加载</button>
          </div>

          <img
            ref="imageRef"
            v-show="imageStatus !== 'error'"
            :key="imageKey"
            :data-request-key="imageKey"
            :src="imageRequestSrc"
            :alt="activeAlt"
            class="viewer-img"
            :class="{ 'is-grabbing': dragging, 'is-zoomed': scale > 1 }"
            :style="imageTransform"
            draggable="false"
            loading="eager"
            fetchpriority="high"
            decoding="async"
            @load="handleImageLoad"
            @error="handleImageError"
            @click.stop
          />
        </main>

        <p class="viewer-announcement sr-only" aria-live="polite" aria-atomic="true">
          {{ viewerAnnouncement }}
        </p>

        <button
          v-if="hasMultiple"
          class="viewer-nav viewer-nav--next"
          :class="{ 'is-hidden': !controlsVisible }"
          type="button"
          aria-label="查看下一张图片"
          @click="next"
        ><el-icon><ArrowRight /></el-icon></button>

        <aside v-if="metadataOpen && activeMetadata.length" class="viewer-metadata" aria-label="图片信息">
          <dl>
            <div v-for="row in activeMetadata" :key="row.label" class="viewer-metadata-row">
              <dt>{{ row.label }}</dt>
              <dd>{{ row.value }}</dd>
            </div>
          </dl>
        </aside>

        <footer class="viewer-toolbar" :class="{ 'is-hidden': !controlsVisible }" aria-label="图片缩放操作">
          <button class="viewer-icon-button" type="button" aria-label="缩小图片" :disabled="scale <= MIN_SCALE" @click="zoomOut">
            <el-icon><ZoomOut /></el-icon>
          </button>
          <span class="zoom-level" aria-live="polite">{{ Math.round(scale * 100) }}%</span>
          <button class="viewer-icon-button" type="button" aria-label="放大图片" :disabled="scale >= MAX_SCALE" @click="zoomIn">
            <el-icon><ZoomIn /></el-icon>
          </button>
          <button class="viewer-reset" type="button" :disabled="scale === 1 && translation.x === 0 && translation.y === 0" @click="resetZoom">
            适应屏幕
          </button>
        </footer>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ArrowLeft, ArrowRight, Close, FullScreen, InfoFilled, PictureFilled, ZoomIn, ZoomOut } from '@element-plus/icons-vue'
import { getImageAlt, getImageViewerUrl } from '../utils/imageRequests'
import { formatUserIdentityText } from '../utils/userIdentity'

const MIN_SCALE = 1
const MAX_SCALE = 5
const props = defineProps({
  src: { type: String, default: '' },
  alt: { type: String, default: '' },
  title: { type: String, default: '' },
  items: { type: Array, default: () => [] },
  initialIndex: { type: Number, default: 0 },
})
const emit = defineEmits(['close', 'change'])

const visible = ref(false)
const activeIndex = ref(0)
const scale = ref(1)
const translation = reactive({ x: 0, y: 0 })
const imageStatus = ref('idle')
const metadataOpen = ref(false)
const controlsVisible = ref(true)
const fullscreen = ref(false)
const dragging = ref(false)
const overlayRef = ref(null)
const stageRef = ref(null)
const imageRef = ref(null)
const imageRevision = ref(0)
const retryNonce = ref(0)
const viewerAnnouncement = ref('')
const pointers = new Map()
let previousFocus = null
let isolatedAppRoot = null
let previousAppInert = false
let previousAppAriaHidden = null
let hideControlsTimer = null
let dragOrigin = null
let swipeOrigin = null
let lastPreloadKey = ''
let announcementRevision = 0

const fallbackItem = computed(() => ({ src: props.src, imageName: props.title || props.alt, alt: props.alt }))
const normalizedItems = computed(() => {
  const source = props.items.length ? props.items : [fallbackItem.value]
  return source.map(item => typeof item === 'string' ? { src: item } : item).filter(Boolean)
})
const activeItem = computed(() => normalizedItems.value[activeIndex.value] || fallbackItem.value)
const activeSrc = computed(() => activeItem.value.src || getImageViewerUrl(activeItem.value) || '')
const activeTitle = computed(() => activeItem.value.imageName || activeItem.value.title || props.title || getImageAlt(activeItem.value))
const activeAlt = computed(() => activeItem.value.alt || getImageAlt(activeItem.value) || props.alt || activeTitle.value)
const hasMultiple = computed(() => normalizedItems.value.length > 1)
const imageRequestSrc = computed(() => appendRetryMarker(activeSrc.value, retryNonce.value))
const imageKey = computed(() => `${imageRequestSrc.value}:${imageRevision.value}`)
const imageTransform = computed(() => ({ transform: `translate3d(${translation.x}px, ${translation.y}px, 0) scale(${scale.value})` }))
const activeMetadata = computed(() => {
  const item = activeItem.value
  const rows = [
    ['作者', formatUserIdentityText(item, '')],
    ['分类', item.categoryName],
    ['尺寸', item.width && item.height ? `${item.width} × ${item.height}` : ''],
    ['描述', item.description],
  ]
  return rows.filter(([, value]) => value !== undefined && value !== null && value !== '').map(([label, value]) => ({ label, value }))
})

watch(activeSrc, () => {
  retryNonce.value = 0
  beginImageRequest()
  resetZoom()
  preloadAdjacent()
})

watch(visible, async open => {
  if (open) {
    document.documentElement.classList.add('viewer-open')
    window.addEventListener('resize', clampTranslation, { passive: true })
    document.addEventListener('fullscreenchange', syncFullscreen)
    await nextTick()
    overlayRef.value?.focus()
    isolateBackground()
    showControls()
    preloadAdjacent()
  } else {
    releaseViewerEnvironment()
  }
})

function open(options = {}) {
  previousFocus = options.trigger || document.activeElement
  const previousSrc = activeSrc.value
  retryNonce.value = 0
  const requestedIndex = Number.isInteger(options.index) ? options.index : props.initialIndex
  activeIndex.value = clampIndex(requestedIndex)
  metadataOpen.value = false
  if (activeSrc.value === previousSrc) beginImageRequest()
  resetZoom()
  visible.value = true
  scheduleAnnouncement()
}

function close() {
  if (!visible.value) return
  releaseViewerEnvironment()
  visible.value = false
  clearTimeout(hideControlsTimer)
  if (document.fullscreenElement) document.exitFullscreen?.().catch(() => {})
  emit('close')
  nextTick(() => previousFocus?.focus?.())
}

function clampIndex(index) {
  return Math.min(Math.max(Number(index) || 0, 0), Math.max(normalizedItems.value.length - 1, 0))
}

function changeImage(index) {
  if (!hasMultiple.value) return
  const previousSrc = activeSrc.value
  activeIndex.value = (index + normalizedItems.value.length) % normalizedItems.value.length
  metadataOpen.value = false
  if (activeSrc.value === previousSrc) beginImageRequest()
  resetZoom()
  scheduleAnnouncement()
  emit('change', activeIndex.value, activeItem.value)
}

function previous() { changeImage(activeIndex.value - 1) }
function next() { changeImage(activeIndex.value + 1) }

function zoomIn() { setScale(scale.value + 0.25) }
function zoomOut() { setScale(scale.value - 0.25) }
function setScale(value) {
  scale.value = Math.min(MAX_SCALE, Math.max(MIN_SCALE, Number(value.toFixed(2))))
  if (scale.value === 1) {
    translation.x = 0
    translation.y = 0
  }
  nextTick(clampTranslation)
  showControls()
}
function resetZoom() {
  scale.value = 1
  translation.x = 0
  translation.y = 0
}
function onWheel(event) { setScale(scale.value + (event.deltaY < 0 ? 0.25 : -0.25)) }

function startPointer(event) {
  event.currentTarget.setPointerCapture?.(event.pointerId)
  pointers.set(event.pointerId, { x: event.clientX, y: event.clientY })
  if (pointers.size === 1) {
    swipeOrigin = { x: event.clientX, y: event.clientY, time: Date.now() }
    if (scale.value > 1) {
      dragging.value = true
      dragOrigin = { x: event.clientX - translation.x, y: event.clientY - translation.y }
    }
  }
}

function movePointer(event) {
  if (!pointers.has(event.pointerId)) return
  pointers.set(event.pointerId, { x: event.clientX, y: event.clientY })
  if (dragging.value && pointers.size === 1 && dragOrigin) {
    translation.x = event.clientX - dragOrigin.x
    translation.y = event.clientY - dragOrigin.y
    clampTranslation()
  }
}

function endPointer(event) {
  if (scale.value === 1 && swipeOrigin && Date.now() - swipeOrigin.time < 600) {
    const deltaX = event.clientX - swipeOrigin.x
    const deltaY = event.clientY - swipeOrigin.y
    if (Math.abs(deltaX) > 56 && Math.abs(deltaX) > Math.abs(deltaY) * 1.4) deltaX < 0 ? next() : previous()
  }
  pointers.delete(event.pointerId)
  dragging.value = false
  dragOrigin = null
  swipeOrigin = null
}

function onKeydown(event) {
  showControls()
  if (event.key === 'Escape') return close()
  if (event.key === 'ArrowLeft') return previous()
  if (event.key === 'ArrowRight') return next()
  if (event.key === '+' || event.key === '=') return zoomIn()
  if (event.key === '-') return zoomOut()
  if (event.key === '0') return resetZoom()
  if (event.key !== 'Tab') return
  const focusable = [...overlayRef.value.querySelectorAll('button:not([disabled]), [href], [tabindex]:not([tabindex="-1"])')]
  if (!focusable.length) return event.preventDefault()
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
}

function showControls() {
  controlsVisible.value = true
  clearTimeout(hideControlsTimer)
  hideControlsTimer = setTimeout(() => {
    const focusedControl = document.activeElement !== overlayRef.value && overlayRef.value?.contains(document.activeElement)
    if (scale.value === 1 && !metadataOpen.value && !focusedControl) controlsVisible.value = false
  }, 2400)
}

async function toggleFullscreen() {
  try {
    if (document.fullscreenElement) await document.exitFullscreen()
    else await overlayRef.value?.requestFullscreen?.()
  } catch {}
  syncFullscreen()
}

function retryImage() {
  retryNonce.value += 1
  beginImageRequest()
}

function appendRetryMarker(src, nonce) {
  if (!src || !nonce || src.startsWith('data:')) return src
  try {
    const url = new URL(src, window.location.href)
    url.searchParams.set('__viewer_retry', String(nonce))
    return url.toString()
  } catch {
    return src
  }
}

function beginImageRequest() {
  imageRevision.value += 1
  imageStatus.value = activeSrc.value ? 'loading' : 'error'
}

function isCurrentImageEvent(event) {
  return event?.currentTarget?.dataset?.requestKey === imageKey.value
}

function handleImageLoad(event) {
  if (isCurrentImageEvent(event)) imageStatus.value = 'loaded'
}

function handleImageError(event) {
  if (isCurrentImageEvent(event)) imageStatus.value = 'error'
}

function scheduleAnnouncement() {
  const revision = ++announcementRevision
  viewerAnnouncement.value = ''
  nextTick(() => {
    if (revision !== announcementRevision || !visible.value) return
    viewerAnnouncement.value = `第 ${activeIndex.value + 1} 张，共 ${normalizedItems.value.length} 张，${activeTitle.value}`
  })
}

function isolateBackground() {
  if (isolatedAppRoot) return
  const appRoot = document.getElementById('app')
  if (!appRoot) return
  isolatedAppRoot = appRoot
  previousAppInert = Boolean(appRoot.inert)
  previousAppAriaHidden = appRoot.getAttribute('aria-hidden')
  appRoot.inert = true
  appRoot.setAttribute('aria-hidden', 'true')
}

function restoreBackground() {
  if (!isolatedAppRoot) return
  isolatedAppRoot.inert = previousAppInert
  if (previousAppAriaHidden === null) isolatedAppRoot.removeAttribute('aria-hidden')
  else isolatedAppRoot.setAttribute('aria-hidden', previousAppAriaHidden)
  isolatedAppRoot = null
}

function syncFullscreen() {
  fullscreen.value = document.fullscreenElement === overlayRef.value
}

function releaseViewerEnvironment() {
  document.documentElement.classList.remove('viewer-open')
  window.removeEventListener('resize', clampTranslation)
  document.removeEventListener('fullscreenchange', syncFullscreen)
  restoreBackground()
  fullscreen.value = false
}

function preloadAdjacent() {
  if (!visible.value || normalizedItems.value.length < 2 || typeof Image === 'undefined') return
  const nextIndex = (activeIndex.value + 1) % normalizedItems.value.length
  const previousIndex = (activeIndex.value - 1 + normalizedItems.value.length) % normalizedItems.value.length
  const compact = typeof window !== 'undefined' && window.matchMedia?.('(max-width: 767px)').matches
  const indexes = compact ? [nextIndex] : [previousIndex, nextIndex]
  const urls = indexes.map(index => {
    const item = normalizedItems.value[index]
    return item?.src || getImageViewerUrl(item || {})
  }).filter(Boolean)
  const preloadKey = urls.join('|')
  if (!preloadKey || preloadKey === lastPreloadKey) return
  lastPreloadKey = preloadKey
  urls.forEach(url => {
    new Image().src = url
  })
}

function clampTranslation() {
  if (scale.value <= 1 || !stageRef.value || !imageRef.value) {
    if (scale.value <= 1) {
      translation.x = 0
      translation.y = 0
    }
    return
  }
  const maxX = Math.max(0, (imageRef.value.offsetWidth * scale.value - stageRef.value.clientWidth) / 2)
  const maxY = Math.max(0, (imageRef.value.offsetHeight * scale.value - stageRef.value.clientHeight) / 2)
  translation.x = Math.min(maxX, Math.max(-maxX, translation.x))
  translation.y = Math.min(maxY, Math.max(-maxY, translation.y))
}

onBeforeUnmount(() => {
  clearTimeout(hideControlsTimer)
  releaseViewerEnvironment()
})

defineExpose({ open, close, previous, next, zoomIn, zoomOut, resetZoom, scale, translation, activeIndex, imageStatus })
</script>

<style scoped>
.viewer-overlay {
  position: fixed;
  inset: 0;
  z-index: var(--layer-viewer);
  overflow: hidden;
  background: var(--color-viewer-bg, #0e1216);
  color: var(--color-text-inverse, #eef0ec);
  outline: none;
  touch-action: none;
}
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
.viewer-stage { position: absolute; inset: 0; display: grid; place-items: center; overflow: hidden; }
.viewer-img { max-width: 92vw; max-height: 90vh; width: auto; height: auto; object-fit: contain; user-select: none; cursor: default; transform-origin: center; transition: transform var(--duration-fast, 140ms) var(--ease-standard, ease); }
.viewer-img.is-zoomed { cursor: grab; }
.viewer-img.is-grabbing { cursor: grabbing; transition: none; }
.viewer-header { position: absolute; z-index: 4; inset: 0 0 auto; min-height: 72px; display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 12px 20px; border-bottom: 1px solid rgba(238,240,236,.12); background: rgba(14,18,22,.88); transition: opacity var(--duration-standard, 180ms) ease; }
.viewer-heading { min-width: 0; display: flex; align-items: baseline; gap: 12px; }
.viewer-heading strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font: 500 var(--text-md, 16px)/1.4 var(--font-body, sans-serif); }
.viewer-position { flex: none; color: rgba(238,240,236,.64); font-variant-numeric: tabular-nums; font-size: 13px; }
.viewer-header-actions { display: flex; gap: 8px; }
.viewer-icon-button, .viewer-reset, .viewer-error button { min-width: 44px; min-height: 44px; border: 1px solid rgba(238,240,236,.18); border-radius: var(--radius-sm, 4px); background: var(--color-viewer-surface, #171d22); color: inherit; cursor: pointer; }
.viewer-icon-button { display: grid; place-items: center; padding: 0; font-size: 19px; }
.viewer-icon-button:hover, .viewer-icon-button:focus-visible, .viewer-reset:hover, .viewer-reset:focus-visible, .viewer-error button:hover, .viewer-error button:focus-visible { border-color: rgba(238,240,236,.52); background: #202830; outline: 2px solid var(--color-urban, #4f7e8c); outline-offset: 2px; }
.viewer-icon-button:disabled, .viewer-reset:disabled { opacity: .36; cursor: not-allowed; }
.viewer-close { margin-left: 4px; }
.viewer-nav { position: absolute; z-index: 3; top: 50%; width: 52px; height: 72px; border: 1px solid rgba(238,240,236,.16); background: rgba(23,29,34,.82); color: inherit; transform: translateY(-50%); cursor: pointer; transition: opacity var(--duration-standard, 180ms) ease, border-color var(--duration-fast, 140ms) ease; }
.viewer-nav--previous { left: 16px; }
.viewer-nav--next { right: 16px; }
.viewer-nav:hover, .viewer-nav:focus-visible { border-color: rgba(238,240,236,.55); outline: 2px solid var(--color-urban, #4f7e8c); outline-offset: 2px; }
.viewer-toolbar { position: absolute; z-index: 4; left: 50%; bottom: 20px; display: flex; align-items: center; gap: 8px; padding: 8px; border: 1px solid rgba(238,240,236,.16); border-radius: var(--radius-md, 6px); background: var(--color-viewer-surface, #171d22); transform: translateX(-50%); transition: opacity var(--duration-standard, 180ms) ease; }
.viewer-reset { padding: 0 14px; font: inherit; }
.zoom-level { min-width: 58px; text-align: center; color: rgba(238,240,236,.72); font-variant-numeric: tabular-nums; font-size: 13px; }
.viewer-metadata { position: absolute; z-index: 3; top: 88px; right: 20px; width: min(320px, calc(100vw - 40px)); max-height: calc(100vh - 180px); overflow: auto; padding: 18px; border: 1px solid rgba(238,240,236,.16); background: var(--color-viewer-surface, #171d22); }
.viewer-metadata dl { margin: 0; }
.viewer-metadata-row { display: grid; grid-template-columns: 64px 1fr; gap: 12px; padding: 10px 0; border-bottom: 1px solid rgba(238,240,236,.1); }
.viewer-metadata-row:last-child { border-bottom: 0; }
.viewer-metadata dt { color: rgba(238,240,236,.56); font-size: 12px; }
.viewer-metadata dd { margin: 0; overflow-wrap: anywhere; color: rgba(238,240,236,.9); font-size: 14px; line-height: 1.6; }
.viewer-loading, .viewer-error { position: relative; z-index: 2; display: grid; place-items: center; gap: 12px; max-width: 360px; padding: 28px; text-align: center; background: var(--color-viewer-bg, #0e1216); color: rgba(238,240,236,.72); }
.viewer-loading-mark { width: 40px; height: 1px; overflow: hidden; background: rgba(238,240,236,.18); }
.viewer-loading-mark::after { content: ''; display: block; width: 50%; height: 100%; background: var(--color-urban, #4f7e8c); animation: viewer-load 1s linear infinite; }
.viewer-error :deep(.el-icon) { font-size: 32px; color: rgba(238,240,236,.48); }
.viewer-error strong { color: var(--color-text-inverse, #eef0ec); }
.viewer-error button { padding: 0 16px; margin-top: 4px; }
.is-hidden { opacity: 0; pointer-events: none; }
.viewer-fade-enter-active, .viewer-fade-leave-active { transition: opacity var(--duration-overlay, 240ms) var(--ease-standard, ease); }
.viewer-fade-enter-from, .viewer-fade-leave-to { opacity: 0; }
@keyframes viewer-load { from { transform: translateX(-100%); } to { transform: translateX(200%); } }

@media (max-width: 767px) {
  .viewer-header { min-height: 64px; padding: 10px 12px; }
  .viewer-header-actions { gap: 4px; }
  .viewer-heading strong { font-size: 14px; }
  .viewer-position { display: none; }
  .viewer-img { max-width: 100vw; max-height: calc(100vh - 132px); }
  .viewer-nav { top: auto; bottom: 18px; width: 48px; height: 48px; transform: none; }
  .viewer-nav--previous { left: 12px; }
  .viewer-nav--next { right: 12px; }
  .viewer-toolbar { bottom: 18px; max-width: calc(100vw - 128px); }
  .viewer-reset { display: none; }
  .viewer-metadata { top: auto; right: 0; bottom: 0; left: 0; width: auto; max-height: 48vh; padding: 18px 20px 24px; border-width: 1px 0 0; }
}

@media (prefers-reduced-motion: reduce) {
  .viewer-img { transition: none; }
  .viewer-header, .viewer-nav, .viewer-toolbar { transition: opacity 200ms ease; }
  .viewer-fade-enter-active, .viewer-fade-leave-active { transition: opacity 200ms ease; }
  .viewer-loading-mark::after { animation: none; }
}
</style>
