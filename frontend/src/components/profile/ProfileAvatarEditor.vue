<template>
  <el-dialog
    :model-value="visible"
    title="编辑头像"
    width="760px"
    :lock-scroll="false"
    class="profile-dialog avatar-dialog"
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="avatar-editor">
      <div class="avatar-editor-layout">
        <div class="avatar-crop-side">
          <div
            ref="cropContainer"
            class="crop-container"
            @mousedown="startDragCrop"
            @mousemove="onDragCrop"
            @mouseup="stopDragCrop"
            @mouseleave="stopDragCrop"
          >
            <img v-if="previewUrl" :src="previewUrl" class="crop-img" :style="cropImgStyle" draggable="false" />
            <el-icon v-else :size="80" class="avatar-empty"><UserFilled /></el-icon>
            <div v-if="previewUrl" class="crop-frame" :style="cropFrameStyle"></div>
            <div v-if="previewUrl" class="crop-grid" :style="cropGridStyle"></div>
            <template v-if="previewUrl">
              <div class="crop-handle crop-handle-ns" :style="handlePosition('top')" @mousedown.stop="startResize($event, 'top')"></div>
              <div class="crop-handle crop-handle-ns" :style="handlePosition('bottom')" @mousedown.stop="startResize($event, 'bottom')"></div>
              <div class="crop-handle crop-handle-ew" :style="handlePosition('left')" @mousedown.stop="startResize($event, 'left')"></div>
              <div class="crop-handle crop-handle-ew" :style="handlePosition('right')" @mousedown.stop="startResize($event, 'right')"></div>
              <div class="crop-handle crop-handle-corner crop-handle-nwse" :style="handlePosition('tl')" @mousedown.stop="startResize($event, 'tl')"></div>
              <div class="crop-handle crop-handle-corner crop-handle-nesw" :style="handlePosition('tr')" @mousedown.stop="startResize($event, 'tr')"></div>
              <div class="crop-handle crop-handle-corner crop-handle-nesw" :style="handlePosition('bl')" @mousedown.stop="startResize($event, 'bl')"></div>
              <div class="crop-handle crop-handle-corner crop-handle-nwse" :style="handlePosition('br')" @mousedown.stop="startResize($event, 'br')"></div>
            </template>
          </div>
          <div class="crop-controls">
            <span class="slider-label">裁剪尺寸</span>
            <el-slider :model-value="cropRatio" :min="0.25" :max="1" :step="0.01" @update:model-value="onSliderChange" />
            <span class="slider-val">{{ Math.round(cropRatio * 100) }}%</span>
          </div>
        </div>

        <div class="avatar-preview-side">
          <p class="preview-label">头像预览</p>
          <div class="preview-circle-lg">
            <img v-if="previewUrl" :src="previewUrl" class="preview-img" :style="previewLgImgStyle" />
            <el-icon v-else :size="48" class="avatar-empty"><UserFilled /></el-icon>
          </div>
          <div class="preview-circle-sm">
            <img v-if="previewUrl" :src="previewUrl" class="preview-img" :style="previewSmImgStyle" />
            <el-icon v-else :size="24" class="avatar-empty"><UserFilled /></el-icon>
          </div>
        </div>
      </div>

      <el-upload
        :auto-upload="false"
        :show-file-list="false"
        :on-change="onFileChange"
        accept="image/jpeg,image/png,image/webp,image/gif"
        class="avatar-replace-upload"
      >
        <el-button>更换图片</el-button>
      </el-upload>
      <p v-if="fileName" class="upload-hint">{{ fileName }}</p>
      <p v-if="loadError" class="avatar-load-error" role="alert">图片加载失败，请更换图片</p>
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="!previewUrl || saving" @click="submitCrop">确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { UserFilled } from '@element-plus/icons-vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  sourceUrl: { type: String, default: '' },
  saving: { type: Boolean, default: false },
})

const emit = defineEmits(['update:visible', 'submit'])

const cropContainer = ref(null)
const previewUrl = ref('')
const selectedFile = ref(null)
const fileName = ref('')
const loadError = ref(false)
const crop = ref({ x: 0, y: 0, size: 200 })
const cropRatio = ref(0.8)
const imgW = ref(400)
const imgH = ref(400)
const imgNatural = ref({ w: 1, h: 1 })

let dragging = false
let resizing = false
let resizeDir = ''
let dragStart = { x: 0, y: 0, cx: 0, cy: 0, cSize: 0 }
let loadToken = 0

function syncSizeFromRatio() {
  const max = Math.min(imgW.value, imgH.value)
  crop.value.size = Math.round(cropRatio.value * max)
  clampCrop()
}

function syncRatioFromSize() {
  const max = Math.min(imgW.value, imgH.value)
  cropRatio.value = Math.round(crop.value.size / max * 100) / 100
}

function clampCrop() {
  const radius = crop.value.size / 2
  crop.value.x = Math.max(radius, Math.min(imgW.value - radius, crop.value.x))
  crop.value.y = Math.max(radius, Math.min(imgH.value - radius, crop.value.y))
}

const imageDisplay = computed(() => {
  const nw = imgNatural.value.w || 1
  const nh = imgNatural.value.h || 1
  const cw = imgW.value || 1
  const ch = imgH.value || 1
  const scale = Math.max(cw / nw, ch / nh)
  const width = nw * scale
  const height = nh * scale
  return { width, height, left: (cw - width) / 2, top: (ch - height) / 2 }
})

const cropBox = computed(() => {
  const radius = crop.value.size / 2
  return { left: crop.value.x - radius, top: crop.value.y - radius, size: crop.value.size }
})

const cropImgStyle = computed(() => {
  const display = imageDisplay.value
  return {
    width: `${display.width}px`,
    height: `${display.height}px`,
    left: `${display.left}px`,
    top: `${display.top}px`,
  }
})

const cropFrameStyle = computed(() => ({
  left: `${cropBox.value.left}px`,
  top: `${cropBox.value.top}px`,
  width: `${cropBox.value.size}px`,
  height: `${cropBox.value.size}px`,
}))

const cropGridStyle = computed(() => ({
  ...cropFrameStyle.value,
  backgroundImage: [
    'linear-gradient(to right, transparent 33.333%, rgba(255,255,255,0.55) 33.333%, rgba(255,255,255,0.55) calc(33.333% + 1px), transparent calc(33.333% + 1px))',
    'linear-gradient(to right, transparent 66.666%, rgba(255,255,255,0.55) 66.666%, rgba(255,255,255,0.55) calc(66.666% + 1px), transparent calc(66.666% + 1px))',
    'linear-gradient(to bottom, transparent 33.333%, rgba(255,255,255,0.55) 33.333%, rgba(255,255,255,0.55) calc(33.333% + 1px), transparent calc(33.333% + 1px))',
    'linear-gradient(to bottom, transparent 66.666%, rgba(255,255,255,0.55) 66.666%, rgba(255,255,255,0.55) calc(66.666% + 1px), transparent calc(66.666% + 1px))',
  ].join(', '),
}))

function previewImgStyle(previewSize) {
  if (!previewUrl.value) return { display: 'none' }
  const display = imageDisplay.value
  const scale = previewSize / crop.value.size
  return {
    width: `${display.width * scale}px`,
    height: `${display.height * scale}px`,
    left: `${(display.left - cropBox.value.left) * scale}px`,
    top: `${(display.top - cropBox.value.top) * scale}px`,
  }
}

const previewLgImgStyle = computed(() => previewImgStyle(120))
const previewSmImgStyle = computed(() => previewImgStyle(56))

function handlePosition(direction) {
  const radius = crop.value.size / 2
  const positions = {
    top: { left: `${crop.value.x}px`, top: `${crop.value.y - radius}px` },
    bottom: { left: `${crop.value.x}px`, top: `${crop.value.y + radius}px` },
    left: { left: `${crop.value.x - radius}px`, top: `${crop.value.y}px` },
    right: { left: `${crop.value.x + radius}px`, top: `${crop.value.y}px` },
    tl: { left: `${crop.value.x - radius}px`, top: `${crop.value.y - radius}px` },
    tr: { left: `${crop.value.x + radius}px`, top: `${crop.value.y - radius}px` },
    bl: { left: `${crop.value.x - radius}px`, top: `${crop.value.y + radius}px` },
    br: { left: `${crop.value.x + radius}px`, top: `${crop.value.y + radius}px` },
  }
  return positions[direction]
}

function initCrop() {
  crop.value.x = imgW.value / 2
  crop.value.y = imgH.value / 2
  cropRatio.value = 0.8
  syncSizeFromRatio()
}

function updateCropBounds() {
  if (!cropContainer.value) return
  imgW.value = cropContainer.value.clientWidth || 400
  imgH.value = cropContainer.value.clientHeight || 400
}

function loadSource(source) {
  const token = ++loadToken
  previewUrl.value = source || ''
  loadError.value = false
  imgNatural.value = { w: 1, h: 1 }

  if (!source) {
    nextTick(() => {
      if (token !== loadToken) return
      updateCropBounds()
      initCrop()
    })
    return
  }

  const img = new Image()
  img.onload = async () => {
    if (token !== loadToken) return
    imgNatural.value = { w: img.naturalWidth, h: img.naturalHeight }
    await nextTick()
    if (token !== loadToken) return
    updateCropBounds()
    initCrop()
  }
  img.onerror = () => {
    if (token !== loadToken) return
    previewUrl.value = ''
    loadError.value = true
  }
  img.src = source
}

function onFileChange(file) {
  if (!file?.raw) return
  selectedFile.value = file.raw
  fileName.value = file.name || file.raw.name || ''
  const reader = new FileReader()
  reader.onload = (event) => loadSource(event.target?.result || '')
  reader.onerror = () => {
    previewUrl.value = ''
    loadError.value = true
  }
  reader.readAsDataURL(file.raw)
}

function onSliderChange(value) {
  cropRatio.value = Number(value)
  syncSizeFromRatio()
}

function onResize(event) {
  const dx = event.clientX - dragStart.x
  const dy = event.clientY - dragStart.y
  let newSize = dragStart.cSize
  let newX = dragStart.cx
  let newY = dragStart.cy
  const fromLeft = ['left', 'tl', 'bl'].includes(resizeDir)
  const fromRight = ['right', 'tr', 'br'].includes(resizeDir)
  const fromTop = ['top', 'tl', 'tr'].includes(resizeDir)
  const fromBottom = ['bottom', 'bl', 'br'].includes(resizeDir)

  if (fromRight) newSize = dragStart.cSize + dx * 2
  if (fromLeft) newSize = dragStart.cSize - dx * 2
  if (fromBottom) newSize = Math.max(newSize, dragStart.cSize + dy * 2)
  if (fromTop) newSize = Math.max(newSize, dragStart.cSize - dy * 2)

  newSize = Math.max(80, Math.min(Math.min(imgW.value, imgH.value), newSize))
  const radius = newSize / 2
  crop.value.size = newSize
  syncRatioFromSize()
  if (fromLeft) newX = dragStart.cx + (dragStart.cSize - newSize) / 2
  if (fromRight) newX = dragStart.cx - (dragStart.cSize - newSize) / 2
  if (fromTop) newY = dragStart.cy + (dragStart.cSize - newSize) / 2
  if (fromBottom) newY = dragStart.cy - (dragStart.cSize - newSize) / 2
  crop.value.x = Math.max(radius, Math.min(imgW.value - radius, newX))
  crop.value.y = Math.max(radius, Math.min(imgH.value - radius, newY))
}

function startDragCrop(event) {
  if (resizing || event.target.classList.contains('crop-handle')) return
  dragging = true
  document.body.style.cursor = 'move'
  document.body.style.userSelect = 'none'
  dragStart = { x: event.clientX, y: event.clientY, cx: crop.value.x, cy: crop.value.y, cSize: crop.value.size }
}

function onDragCrop(event) {
  if (!dragging) return
  const radius = crop.value.size / 2
  crop.value.x = Math.max(radius, Math.min(imgW.value - radius, dragStart.cx + event.clientX - dragStart.x))
  crop.value.y = Math.max(radius, Math.min(imgH.value - radius, dragStart.cy + event.clientY - dragStart.y))
}

function stopDragCrop() {
  dragging = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

const cursorMap = { top: 'ns-resize', bottom: 'ns-resize', left: 'ew-resize', right: 'ew-resize', tl: 'nwse-resize', br: 'nwse-resize', tr: 'nesw-resize', bl: 'nesw-resize' }

function startResize(event, direction) {
  resizing = true
  resizeDir = direction
  dragStart = { x: event.clientX, y: event.clientY, cx: crop.value.x, cy: crop.value.y, cSize: crop.value.size }
  document.body.style.cursor = cursorMap[direction]
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onResize)
  window.addEventListener('mouseup', stopResize)
}

function stopResize() {
  resizing = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onResize)
  window.removeEventListener('mouseup', stopResize)
}

function isGifFile(file) {
  return !!file && (file.type === 'image/gif' || /\.gif$/i.test(file.name || ''))
}

function cropImage() {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      const nw = imgNatural.value.w || img.naturalWidth
      const nh = imgNatural.value.h || img.naturalHeight
      const display = imageDisplay.value
      const sx = (cropBox.value.left - display.left) / display.width * nw
      const sy = (cropBox.value.top - display.top) / display.height * nh
      const sw = crop.value.size / display.width * nw
      const sh = crop.value.size / display.height * nh
      const canvas = document.createElement('canvas')
      canvas.width = 400
      canvas.height = 400
      const context = canvas.getContext('2d')
      context.imageSmoothingEnabled = true
      context.imageSmoothingQuality = 'high'
      context.drawImage(img, sx, sy, sw, sh, 0, 0, 400, 400)
      canvas.toBlob(blob => {
        if (blob) resolve(blob)
        else reject(new Error('Canvas toBlob failed'))
      }, 'image/png')
    }
    img.onerror = reject
    img.src = previewUrl.value
  })
}

async function submitCrop() {
  if (!previewUrl.value || props.saving) return
  try {
    const gif = isGifFile(selectedFile.value)
    const blob = gif ? selectedFile.value : await cropImage()
    emit('submit', {
      blob,
      filename: gif ? 'avatar.gif' : 'avatar.png',
      sourceUrl: previewUrl.value,
    })
  } catch {
    loadError.value = true
  }
}

watch(() => [props.visible, props.sourceUrl], ([visible, sourceUrl]) => {
  if (!visible) {
    stopDragCrop()
    stopResize()
    return
  }
  selectedFile.value = null
  fileName.value = ''
  loadSource(sourceUrl)
}, { immediate: true })

onBeforeUnmount(() => {
  stopDragCrop()
  stopResize()
})
</script>

<style scoped>
.avatar-editor-layout { display: grid; grid-template-columns: minmax(0, 1fr) 220px; gap: var(--space-5); }
.crop-container { position: relative; width: 400px; height: 400px; max-width: 100%; overflow: hidden; border: 1px solid var(--color-border-subtle); background: var(--color-viewer-bg); }
.crop-img, .preview-img { position: absolute; max-width: none; pointer-events: none; user-select: none; }
.avatar-empty { position: absolute; inset: 0; margin: auto; color: var(--color-text-muted); }
.crop-frame { position: absolute; border: 2px solid var(--color-vermilion); box-shadow: 0 0 0 999px rgba(0,0,0,.48); cursor: move; }
.crop-grid { position: absolute; background-image: linear-gradient(to right, transparent 33.1%, rgba(255,255,255,.45) 33.3%, transparent 33.5%, transparent 66.4%, rgba(255,255,255,.45) 66.6%, transparent 66.8%), linear-gradient(to bottom, transparent 33.1%, rgba(255,255,255,.45) 33.3%, transparent 33.5%, transparent 66.4%, rgba(255,255,255,.45) 66.6%, transparent 66.8%); pointer-events: none; }
.crop-handle { position: absolute; z-index: 2; width: 16px; height: 16px; transform: translate(-50%, -50%); border: 2px solid var(--color-text-inverse); background: var(--color-vermilion); }
.crop-handle-ns { cursor: ns-resize; }.crop-handle-ew { cursor: ew-resize; }.crop-handle-nwse { cursor: nwse-resize; }.crop-handle-nesw { cursor: nesw-resize; }
.crop-controls { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: var(--space-3); margin-top: var(--space-4); color: var(--color-text-secondary); font-size: var(--text-sm); }
.avatar-preview-side { display: flex; flex-direction: column; align-items: center; gap: var(--space-4); }.preview-label { align-self: flex-start; margin: 0; color: var(--color-text-muted); font-size: var(--text-xs); }
.preview-circle-lg, .preview-circle-sm { position: relative; overflow: hidden; border: 1px solid var(--color-border-subtle); border-radius: 50%; background: var(--color-surface-2); }.preview-circle-lg { width: 160px; height: 160px; }.preview-circle-sm { width: 64px; height: 64px; }
.avatar-replace-upload { margin-top: var(--space-4); }.upload-hint, .avatar-load-error { margin: var(--space-2) 0 0; color: var(--color-text-muted); font-size: var(--text-sm); overflow-wrap: anywhere; }.avatar-load-error { color: var(--color-error); }
@media (max-width: 820px) { .avatar-editor-layout { grid-template-columns: 1fr; } .avatar-preview-side { display: none; } }
@media (max-width: 520px) { .crop-container { width: 100%; height: min(80vw, 400px); } }
</style>
