<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useParticles } from '../composables/useParticles'
import { createExplosion, createTrail, updateParticles } from '../particle/engine'

const { particles, explosionParams, trailParams } = useParticles()

const mouse = { x: -1000, y: -1000, onScreen: false }
const svgRef = ref(null)
let rafId = null
let lastTime = 0

function getExplosionConfig() {
  return { ...explosionParams, colors: [...explosionParams.colors] }
}

function getTrailConfig() {
  return { ...trailParams }
}

onMounted(() => {
  lastTime = performance.now()
  const loop = (time) => {
    const dt = Math.min((time - lastTime) / 1000, 0.05)
    lastTime = time
    let current = particles.value
    if (mouse.onScreen) {
      const trails = createTrail(mouse.x, mouse.y, getTrailConfig())
      current = updateParticles([...current, ...trails], dt)
    } else {
      current = updateParticles(current, dt)
    }
    particles.value = current
    rafId = requestAnimationFrame(loop)
  }
  rafId = requestAnimationFrame(loop)
})

onUnmounted(() => {
  if (rafId) cancelAnimationFrame(rafId)
})

function handleClick(e) {
  const rect = svgRef.value?.getBoundingClientRect()
  if (!rect) return
  // Skip clicks on interactive elements (buttons, links, inputs)
  if (e.target.closest('button, a, input, textarea, select, .el-button, .el-slider, [role="button"]')) return
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  const expl = createExplosion(x, y, getExplosionConfig())
  particles.value = [...particles.value, ...expl]
}

function handleMouseMove(e) {
  const rect = svgRef.value?.getBoundingClientRect()
  if (!rect) return
  mouse.x = e.clientX - rect.left
  mouse.y = e.clientY - rect.top
  mouse.onScreen = true
}

function handleMouseLeave() {
  mouse.onScreen = false
}
</script>

<template>
  <svg
    ref="svgRef"
    class="particle-canvas"
    @click="handleClick"
    @mousemove="handleMouseMove"
    @mouseleave="handleMouseLeave"
  >
    <defs>
      <filter id="pb-glow">
        <feGaussianBlur stdDeviation="1.5" result="blur" />
        <feMerge>
          <feMergeNode in="blur" />
          <feMergeNode in="SourceGraphic" />
        </feMerge>
      </filter>
      <filter id="pb-glow-strong">
        <feGaussianBlur stdDeviation="3" result="blur" />
        <feMerge>
          <feMergeNode in="blur" />
          <feMergeNode in="SourceGraphic" />
        </feMerge>
      </filter>
    </defs>
    <circle
      v-for="p in particles"
      :key="p.id"
      :cx="p.x"
      :cy="p.y"
      :r="Math.max(0.3, p.size * p.life)"
      :fill="p.color"
      :opacity="Math.min(1, p.life * 1.2)"
      :filter="p.type === 'explosion' ? 'url(#pb-glow)' : undefined"
    />
  </svg>
</template>

<style scoped>
.particle-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  display: block;
  cursor: crosshair;
  user-select: none;
  -webkit-user-select: none;
  pointer-events: auto;
}
</style>
