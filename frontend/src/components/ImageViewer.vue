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
  background: rgba(10, 10, 10, 0.94);
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
  padding: 8px 16px;
  border-radius: 8px;
  background: var(--ink);
  border: 0.5px solid var(--ink2);
  box-shadow: 0 8px 24px rgba(4, 44, 83, 0.3), 0 4px 8px rgba(4, 44, 83, 0.2);
}

.viewer-toolbar :deep(.el-button) {
  background: transparent;
  border: none;
  color: var(--ink6);
  transition: background 0.15s ease, color 0.15s ease;
  border-radius: 4px;
}
.viewer-toolbar :deep(.el-button:hover) {
  background: var(--ink2);
  color: #fff;
}
.viewer-toolbar :deep(.el-button.is-disabled) {
  opacity: 0.3;
}

.zoom-level {
  color: var(--ink5);
  font-size: 18px;
  font-weight: 500;
  min-width: 48px;
  text-align: center;
  font-variant-numeric: tabular-nums;
  font-family: var(--font-body);
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
