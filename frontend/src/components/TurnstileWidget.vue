<template>
  <div v-show="enabled" class="turnstile-shell">
    <div
      ref="containerRef"
      class="turnstile-wrap"
    ></div>
    <div v-if="loading" class="turnstile-status">人机验证加载中</div>
    <div v-else-if="loadFailed" class="turnstile-status is-error">
      人机验证加载失败，请刷新页面重试
    </div>
  </div>
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
    default: import.meta.env.VITE_TURNSTILE_SITE_KEY || ''
  }
})

const emit = defineEmits(['verified', 'expired', 'error'])

const containerRef = ref(null)
const token = ref('')
const loading = ref(false)
const loadFailed = ref(false)
const rendered = ref(false)
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
  if (!props.sitekey) {
    loadFailed.value = true
    return
  }
  loading.value = true
  loadFailed.value = false
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
  rendered.value = true
  loading.value = false
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
  renderWidget().catch(() => {
    loading.value = false
    loadFailed.value = true
    emit('error')
  })
})

onUnmounted(() => {
  if (props.enabled && window.turnstile && widgetId !== null) {
    window.turnstile.remove(widgetId)
  }
})
</script>

<style scoped>
.turnstile-shell {
  min-height: 65px;
  display: grid;
  place-items: center;
  margin: 4px 0 14px;
}

.turnstile-wrap {
  width: 100%;
  min-height: 65px;
  display: flex;
  justify-content: center;
  grid-area: 1 / 1;
}

.turnstile-status {
  width: 100%;
  min-height: 65px;
  display: flex;
  align-items: center;
  justify-content: center;
  grid-area: 1 / 1;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  font-size: 18px;
}

.turnstile-status.is-error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}
</style>
