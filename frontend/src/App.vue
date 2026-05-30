<template>
  <teleport to="body">
    <div class="cursor-dot" ref="cursorDot"></div>
    <div class="cursor-ring" ref="cursorRing"></div>
    <button class="back-to-top" ref="backToTop" @click="scrollToTop" title="返回顶部">&uarr;</button>
  </teleport>
  <router-view />
  <NotificationDrawer />
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import NotificationDrawer from './components/NotificationDrawer.vue'

const router = useRouter()
const cursorDot = ref(null)
const cursorRing = ref(null)
const backToTop = ref(null)

/* ── Custom cursor ── */
let mx = 0, my = 0, rx = 0, ry = 0

function onMouseMove(e) {
  mx = e.clientX; my = e.clientY
}

function updateDot() {
  if (cursorDot.value) {
    cursorDot.value.style.left = mx + 'px'
    cursorDot.value.style.top = my + 'px'
  }
}

function animRing() {
  rx += (mx - rx) * 0.12
  ry += (my - ry) * 0.12
  if (cursorRing.value) {
    cursorRing.value.style.left = Math.round(rx) + 'px'
    cursorRing.value.style.top = Math.round(ry) + 'px'
  }
  updateDot()
  requestAnimationFrame(animRing)
}

/* ── Back to top ── */
function onScroll() {
  if (backToTop.value) {
    backToTop.value.classList.toggle('visible', window.scrollY > 300)
  }
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/* ── Scroll reveal ── */
let revealObserver = null

function setupReveal() {
  revealObserver = new IntersectionObserver((entries) => {
    entries.forEach(e => {
      if (e.isIntersecting) e.target.classList.add('visible')
    })
  }, { threshold: 0.15 })
}

function observeReveal() {
  if (!revealObserver) return
  document.querySelectorAll('.reveal').forEach(el => revealObserver.observe(el))
}

onMounted(() => {
  window.addEventListener('mousemove', onMouseMove, { capture: true, passive: true })
  animRing()
  window.addEventListener('scroll', onScroll)
  setupReveal()
  observeReveal()
  router.afterEach(() => {
    setTimeout(observeReveal, 100)
  })
})

onUnmounted(() => {
  window.removeEventListener('mousemove', onMouseMove, { capture: true })
  window.removeEventListener('scroll', onScroll)
  if (revealObserver) revealObserver.disconnect()
})
</script>
