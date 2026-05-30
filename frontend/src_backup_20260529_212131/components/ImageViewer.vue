<template>
  <teleport to="body">
    <div class="viewer-overlay" v-if="visible" @click.self="close" @wheel.prevent="onWheel" @keydown="onKeydown" tabindex="0" ref="overlayRef">
      <div class="viewer-toolbar">
        <el-button circle @click="zoomOut" :disabled="scale <= 0.25" aria-label="缩小"><el-icon><ZoomOut /></el-icon></el-button>
        <span class="zoom-level">{{ Math.round(scale * 100) }}%</span>
        <el-button circle @click="zoomIn" :disabled="scale >= 5" aria-label="放大"><el-icon><ZoomIn /></el-icon></el-button>
        <el-button circle @click="resetZoom" aria-label="重置"><el-icon><RefreshRight /></el-icon></el-button>
        <el-button circle @click="close" aria-label="关闭"><el-icon><Close /></el-icon></el-button>
      </div>
      <div class="viewer-stage" @click.self="close">
        <img
          :src="src"
          :style="{ transform: `scale(${scale}) translate(${lastX / scale}px, ${lastY / scale}px)` }"
          class="viewer-img"
          :class="{ grabbing: isGrabbing }"
          @mousedown="startDrag"
          @mousemove="onDrag"
          @mouseup="endDrag"
          @mouseleave="endDrag"
          @click.stop
          draggable="false"
        />
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  src: String
})

const visible = ref(false)
const scale = ref(1)
const isGrabbing = ref(false)
const dragging = ref(false)
const overlayRef = ref(null)
let startX = 0, startY = 0, lastX = 0, lastY = 0

watch(visible, async (v) => {
  if (v) {
    await nextTick()
    overlayRef.value?.focus()
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

function open() {
  scale.value = 1
  lastX = 0
  lastY = 0
  visible.value = true
}

function close() {
  visible.value = false
}

function zoomIn() {
  scale.value = Math.min(5, +(scale.value + 0.25).toFixed(2))
}

function zoomOut() {
  scale.value = Math.max(0.25, +(scale.value - 0.25).toFixed(2))
  if (scale.value <= 1) {
    lastX = 0
    lastY = 0
  }
}

function resetZoom() {
  scale.value = 1
  lastX = 0
  lastY = 0
}

function onWheel(e) {
  if (e.deltaY < 0) zoomIn()
  else zoomOut()
}

function onKeydown(e) {
  if (e.key === 'Escape') close()
  if (e.key === '+' || e.key === '=') zoomIn()
  if (e.key === '-') zoomOut()
  if (e.key === '0') resetZoom()
}

function startDrag(e) {
  if (scale.value <= 1) return
  isGrabbing.value = true
  dragging.value = true
  startX = e.clientX - lastX
  startY = e.clientY - lastY
}

function onDrag(e) {
  if (!dragging.value) return
  lastX = e.clientX - startX
  lastY = e.clientY - startY
}

function endDrag() {
  isGrabbing.value = false
  dragging.value = false
}

defineExpose({ open, close })
</script>

<style scoped>
.viewer-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: rgba(15, 23, 42, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s ease;
  outline: none;
}

.viewer-toolbar {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 3001;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.viewer-toolbar :deep(.el-button) {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: #fff;
  transition: background 0.15s ease;
}
.viewer-toolbar :deep(.el-button:hover) {
  background: rgba(255, 255, 255, 0.22);
}
.viewer-toolbar :deep(.el-button.is-disabled) {
  opacity: 0.3;
}

.zoom-level {
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  font-weight: 500;
  min-width: 42px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.viewer-stage {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.viewer-img {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  transition: transform 0.12s ease;
  user-select: none;
  cursor: grab;
}
.viewer-img.grabbing {
  cursor: grabbing;
  transition: none;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
