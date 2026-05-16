<template>
  <teleport to="body">
    <div class="viewer-overlay" v-if="visible" @click.self="close" @wheel.prevent="onWheel">
      <div class="viewer-toolbar">
        <span class="zoom-level">{{ Math.round(scale * 100) }}%</span>
        <el-button circle @click="zoomIn"><el-icon><ZoomIn /></el-icon></el-button>
        <el-button circle @click="zoomOut"><el-icon><ZoomOut /></el-icon></el-button>
        <el-button circle @click="resetZoom"><el-icon><RefreshRight /></el-icon></el-button>
        <el-button circle @click="close"><el-icon><Close /></el-icon></el-button>
      </div>
      <div class="viewer-stage" @click.self="close">
        <img
          :src="src"
          :style="{ transform: `scale(${scale})` }"
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
import { ref } from 'vue'

const props = defineProps({
  src: String
})

const emit = defineEmits(['close'])
const visible = ref(false)
const scale = ref(1)
const isGrabbing = ref(false)
const dragging = ref(false)
let startX = 0, startY = 0, lastX = 0, lastY = 0

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
  scale.value = Math.min(5, scale.value + 0.25)
}

function zoomOut() {
  scale.value = Math.max(0.25, scale.value - 0.25)
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
  e.target.style.transform = `scale(${scale.value}) translate(${lastX}px, ${lastY}px)`
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
  background: rgba(15, 23, 42, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
}

.viewer-toolbar {
  position: absolute;
  top: 16px;
  right: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 3001;
  padding: 8px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
}

.zoom-level {
  color: #fff;
  font-size: 13px;
  min-width: 48px;
  text-align: center;
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
  transition: transform 0.1s ease;
  user-select: none;
  cursor: grab;
}

.viewer-img.grabbing {
  cursor: grabbing;
  transition: none;
}
</style>
