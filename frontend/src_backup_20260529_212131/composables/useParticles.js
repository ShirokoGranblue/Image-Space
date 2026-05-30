import { ref, shallowRef, reactive } from 'vue'
import { presets } from '../particle/presets'

const particles = shallowRef([])
const presetName = ref('fireworks')
const explosionParams = reactive({ ...presets.fireworks.explosion })
const trailParams = reactive({ ...presets.fireworks.trail })
const settingsVisible = ref(false)

function changePreset(name) {
  presetName.value = name
  Object.assign(explosionParams, presets[name].explosion)
  Object.assign(trailParams, presets[name].trail)
}

function toggleSettings() {
  settingsVisible.value = !settingsVisible.value
}

export function useParticles() {
  return {
    particles,
    presetName,
    explosionParams,
    trailParams,
    settingsVisible,
    changePreset,
    toggleSettings,
  }
}
