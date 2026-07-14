import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { provideGlobalConfig } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

import App from './App.vue'
import router from './router'
import './style.css'
import './styles/tokens.css'

let scrollbarTimer
document.addEventListener('scroll', () => {
  document.documentElement.classList.add('is-scrolling')
  window.clearTimeout(scrollbarTimer)
  scrollbarTimer = window.setTimeout(() => {
    document.documentElement.classList.remove('is-scrolling')
  }, 700)
}, { capture: true, passive: true })

const app = createApp(App)

app.use(createPinia())
app.use(router)
provideGlobalConfig({ locale: zhCn }, app, true)

app.mount('#app')
