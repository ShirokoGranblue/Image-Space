<template>
  <div v-show="enabled" class="turnstile-wrap" ref="containerRef"></div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

const props = defineProps({
  enabled: {
    type: Boolean,
    default: import.meta.env.VITE_TURNSTILE_ENABLED === 'true'
  },
  sitekey: {
    type: String,
    default: import.meta.env.VITE_TURNSTILE_SITE_KEY || '0x4AAAAAADXRE_jtv9_OBFRo'
  }
})

const emit = defineEmits(['verified', 'expired', 'error'])

const containerRef = ref(null)
const token = ref('')
let widgetId = null

function loadTurnstileScript() {
  if (window.turnstile) {
    return Promise.resolve()
  }
  if (window.__turnstileLoading) {
    return window.__turnstileLoading
  }
  window.__turnstileLoading = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'
    script.async = true
    script.defer = true
    script.onload = resolve
    script.onerror = reject
    document.head.appendChild(script)
  })
  return window.__turnstileLoading
}

async function renderWidget() {
  await loadTurnstileScript()
  if (!containerRef.value || widgetId !== null) return
  widgetId = window.turnstile.render(containerRef.value, {
    sitekey: props.sitekey,
    callback: (value) => {
      token.value = value
      emit('verified', value)
    },
    'expired-callback': () => {
      token.value = ''
      emit('expired')
    },
    'error-callback': () => {
      token.value = ''
      emit('error')
    }
  })
}

function getToken() {
  if (!props.enabled) {
    return 'turnstile-disabled'
  }
  return token.value
}

function reset() {
  if (!props.enabled) {
    return
  }
  token.value = ''
  if (window.turnstile && widgetId !== null) {
    window.turnstile.reset(widgetId)
  }
}

defineExpose({ getToken, reset })

onMounted(() => {
  if (!props.enabled) {
    return
  }
  renderWidget().catch(() => emit('error'))
})

onUnmounted(() => {
  if (props.enabled && window.turnstile && widgetId !== null) {
    window.turnstile.remove(widgetId)
  }
})
</script>

<style scoped>
.turnstile-wrap {
  min-height: 65px;
  display: flex;
  justify-content: center;
  margin: 4px 0 14px;
}
</style>
