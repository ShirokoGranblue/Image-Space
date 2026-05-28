<script setup>
import { ref, shallowRef, reactive, onMounted, onUnmounted } from 'vue'
import { presets } from '../particle/presets'
import { createExplosion, createTrail, updateParticles } from '../particle/engine'

const mouse = { x: -1000, y: -1000, onScreen: false }
let rafId = null

const particles = shallowRef([])
const svgRef = ref(null)
const presetName = ref('fireworks')
const expanded = ref(true)

const explosionParams = reactive({ ...presets.fireworks.explosion })
const trailParams = reactive({ ...presets.fireworks.trail })

const explosionSliders = [
  { key: 'particleCount', label: '粒子数量', min: 10, max: 200, step: 5 },
  { key: 'speed', label: '爆发速度', min: 50, max: 600, step: 10 },
  { key: 'gravity', label: '重力', min: -100, max: 400, step: 10 },
  { key: 'size', label: '粒子大小', min: 1, max: 12, step: 0.5 },
  { key: 'rings', label: '环数', min: 1, max: 4, step: 1 },
]

const trailSliders = [
  { key: 'density', label: '拖尾密度', min: 0, max: 2, step: 0.1 },
  { key: 'size', label: '拖尾大小', min: 0.5, max: 8, step: 0.5 },
  { key: 'decay', label: '衰减速度', min: 0.1, max: 1.5, step: 0.05 },
]

function getExplosionConfig() {
  return { ...explosionParams, colors: [...explosionParams.colors] }
}

function getTrailConfig() {
  return { ...trailParams }
}

onMounted(() => {
  let lastTime = performance.now()
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

function changePreset(name) {
  presetName.value = name
  Object.assign(explosionParams, presets[name].explosion)
  Object.assign(trailParams, presets[name].trail)
}
</script>

<template>
  <div class="particle-page">
    <NavBar />

    <div class="particle-canvas-wrapper">
      <svg
        ref="svgRef"
        @click="handleClick"
        @mousemove="handleMouseMove"
        @mouseleave="handleMouseLeave"
      >
        <defs>
          <filter id="glow">
            <feGaussianBlur stdDeviation="1.5" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
          <filter id="glow-strong">
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
          :filter="p.type === 'explosion' ? 'url(#glow)' : undefined"
        />
      </svg>
    </div>

    <aside v-if="expanded" class="control-panel">
      <button class="panel-toggle" @click="expanded = !expanded">
        控制面板 <span>收起 &#9650;</span>
      </button>

      <div class="preset-row">
        <el-button
          v-for="(preset, name) in presets"
          :key="name"
          :type="presetName === name ? 'primary' : 'default'"
          size="small"
          @click="changePreset(name)"
        >
          {{ preset.icon }} {{ preset.label }}
        </el-button>
      </div>

      <div class="params-section">
        <h4>爆炸参数</h4>
        <div v-for="s in explosionSliders" :key="s.key" class="slider-row">
          <span class="slider-label">{{ s.label }}</span>
          <el-slider
            v-model="explosionParams[s.key]"
            :min="s.min"
            :max="s.max"
            :step="s.step"
            size="small"
            :show-tooltip="false"
          />
          <span class="slider-value">{{ explosionParams[s.key] }}</span>
        </div>
      </div>

      <div class="params-section">
        <h4>拖尾参数</h4>
        <div v-for="s in trailSliders" :key="s.key" class="slider-row">
          <span class="slider-label">{{ s.label }}</span>
          <el-slider
            v-model="trailParams[s.key]"
            :min="s.min"
            :max="s.max"
            :step="s.step"
            size="small"
            :show-tooltip="false"
          />
          <span class="slider-value">{{ trailParams[s.key] }}</span>
        </div>
      </div>
    </aside>

    <el-button
      v-else
      class="panel-toggle-collapsed"
      size="small"
      @click="expanded = true"
    >
      控制面板 &#9660;
    </el-button>

    <p class="hint">点击画面产生爆炸 · 移动鼠标产生拖尾</p>
  </div>
</template>

<style scoped>
.particle-page {
  min-height: 100vh;
  background: var(--bg-base);
  position: relative;
}

.particle-canvas-wrapper {
  position: fixed;
  inset: 0;
  z-index: 0;
}

.particle-canvas-wrapper svg {
  width: 100%;
  height: 100%;
  display: block;
  cursor: crosshair;
  user-select: none;
  -webkit-user-select: none;
}

.control-panel {
  position: fixed;
  left: 16px;
  top: 76px;
  z-index: 10;
  width: 240px;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 16px;
  font-family: var(--font-display);
  max-height: calc(100vh - 100px);
  overflow-y: auto;
  animation: fadeUp 0.35s var(--ease-out);
}

.panel-toggle {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: none;
  border: none;
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 0 0 12px 0;
  margin-bottom: 12px;
  border-bottom: 1px solid var(--border-subtle);
}
.panel-toggle:hover {
  color: var(--text-primary);
}

.panel-toggle-collapsed {
  position: fixed !important;
  left: 16px;
  top: 76px;
  z-index: 10;
}

.preset-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 16px;
}

.params-section h4 {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--text-muted);
  margin: 0 0 10px 0;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.slider-label {
  font-size: 11px;
  color: var(--text-secondary);
  min-width: 52px;
  text-align: right;
  flex-shrink: 0;
}

.slider-value {
  font-size: 11px;
  color: var(--text-muted);
  min-width: 32px;
  text-align: right;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.control-panel :deep(.el-slider) {
  flex: 1;
}
.control-panel :deep(.el-slider__runway) {
  height: 3px;
  margin: 0;
}
.control-panel :deep(.el-slider__bar) {
  height: 3px;
}
.control-panel :deep(.el-slider__button) {
  width: 12px;
  height: 12px;
}
.control-panel :deep(.el-button--small) {
  font-size: 11px;
  padding: 4px 8px;
  height: auto;
}

.hint {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 5;
  font-family: var(--font-display);
  font-size: 12px;
  color: var(--text-muted);
  pointer-events: none;
  user-select: none;
}
</style>
