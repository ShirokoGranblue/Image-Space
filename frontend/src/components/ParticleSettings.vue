<script setup>
import { useParticles } from '../composables/useParticles'
import { presets } from '../particle/presets'

const { presetName, explosionParams, trailParams, settingsVisible, changePreset } = useParticles()
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
</script>

<template>
  <el-drawer
    v-model="settingsVisible"
    title="粒子设置"
    direction="rtl"
    size="300px"
    :z-index="200"
    :with-header="true"
  >
    <div class="settings-body">
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
    </div>
  </el-drawer>
</template>

<style scoped>
.settings-body {
  padding: 0 4px;
  font-family: var(--font-title);
}

.preset-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 20px;
}

.params-section {
  margin-bottom: 20px;
}

.params-section h4 {
  font-size: 16px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
  margin: 0 0 10px 0;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.slider-label {
  font-size: 16px;
  color: var(--color-text-secondary);
  min-width: 52px;
  text-align: right;
  flex-shrink: 0;
}

.slider-value {
  font-size: 16px;
  color: var(--color-text-muted);
  min-width: 32px;
  text-align: right;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.preset-row :deep(.el-button--small) {
  font-size: 16px;
  padding: 4px 8px;
  height: auto;
}
</style>
