<template>
  <div
    class="astral-environment"
    :data-astral-intensity="intensity"
    :data-astral-mode="mode"
    :data-astral-paused="String(paused)"
    :data-astral-rings="ringCount"
    aria-hidden="true"
  >
    <svg
      class="astral-score"
      data-astral-score
      viewBox="0 0 1600 900"
      preserveAspectRatio="xMidYMid slice"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="astral-score-gradient" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="var(--astral-teal)" />
          <stop offset="0.5" stop-color="var(--astral-gold)" />
          <stop offset="1" stop-color="var(--astral-rose)" />
        </linearGradient>
      </defs>
      <g fill="none" stroke="url(#astral-score-gradient)" stroke-width="1">
        <path d="M-80 168 C 240 34, 430 282, 760 126 S 1300 72, 1690 210" />
        <path d="M-90 192 C 250 60, 432 304, 770 150 S 1310 96, 1700 234" />
        <path d="M-100 216 C 260 86, 434 326, 780 174 S 1320 120, 1710 258" />
        <path d="M-120 760 C 210 640, 520 824, 820 700 S 1300 614, 1710 748" />
        <path d="M-130 786 C 220 666, 524 850, 830 726 S 1310 640, 1720 774" />
        <path d="M-140 812 C 230 692, 528 876, 840 752 S 1320 666, 1730 800" />
      </g>
      <g fill="var(--astral-starlight)">
        <circle cx="188" cy="117" r="2.5" />
        <circle cx="418" cy="229" r="1.8" />
        <circle cx="708" cy="147" r="2.2" />
        <circle cx="1238" cy="115" r="1.7" />
        <circle cx="278" cy="704" r="2" />
        <circle cx="650" cy="770" r="1.7" />
        <circle cx="1058" cy="681" r="2.4" />
        <circle cx="1424" cy="729" r="1.8" />
      </g>
    </svg>
    <canvas ref="canvasRef" data-astral-canvas />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { ASTRAL_MODES, ASTRAL_MODE_VALUES, ASTRAL_STAR_COLOR_TOKENS } from './astralSystem'

const props = defineProps({
  intensity: {
    type: String,
    required: true,
    validator: value => ['strong', 'medium', 'quiet'].includes(value),
  },
  mode: {
    type: String,
    required: true,
    validator: value => ASTRAL_MODE_VALUES.includes(value),
  },
})

const canvasRef = ref(null)
const paused = ref(false)
const ringCount = ref(0)
let starColors = []
const reducedMotionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
const finePointerQuery = window.matchMedia('(hover: hover) and (pointer: fine)')
const blockingSurfaceSelector = [
  '.el-overlay',
  '.viewer-overlay',
  '.drawer-backdrop',
  '.notification-drawer',
  '.crop-container',
].join(',')
const blankSurfaceSelector = [
  'html',
  'body',
  '#app',
  '.app-shell',
  '.app-shell__content',
  '.auth-page',
  '.public-square-page',
  '.asset-page',
  '.detail-page',
  '.profile-page',
  '.forbidden-page',
  '.not-found-page',
].join(',')
let context
let stars = []
let animationFrame = 0
let lastFrameTime = 0
let activeTime = 0
let surfaceObserver
let rings = []
const pointerTarget = { x: 0, y: 0 }
const pointerPosition = { x: 0, y: 0 }

function hasBlockingSurface() {
  return document.hidden
    || document.documentElement.classList.contains('viewer-open')
    || Boolean(document.querySelector(blockingSurfaceSelector))
}

function starLimit() {
  const areaCount = Math.round((window.innerWidth * window.innerHeight) / 15000)
  const limits = { strong: 110, medium: 82, quiet: 48 }
  return Math.max(28, Math.min(areaCount, limits[props.intensity]))
}

function createStars() {
  stars = Array.from({ length: starLimit() }, (_, index) => ({
    x: Math.random(),
    y: Math.random(),
    radius: 0.45 + Math.random() * 1.25,
    speed: 1.4 + Math.random() * 2.8,
    phase: Math.random() * Math.PI * 2,
    color: starColors[index % starColors.length],
  }))
}

function readStarColors() {
  const rootStyle = getComputedStyle(document.documentElement)
  starColors = ASTRAL_STAR_COLOR_TOKENS.map(token => rootStyle.getPropertyValue(token).trim())
}

function resizeCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return

  const dpr = Math.min(window.devicePixelRatio || 1, 1.5)
  canvas.width = Math.round(window.innerWidth * dpr)
  canvas.height = Math.round(window.innerHeight * dpr)
  context = canvas.getContext('2d')
  context.setTransform(dpr, 0, 0, dpr, 0, 0)
  readStarColors()
  createStars()
  drawFrame(activeTime, 0)
}

function drawFrame(time, deltaSeconds) {
  if (!context) return

  context.clearRect(0, 0, window.innerWidth, window.innerHeight)
  for (const star of stars) {
    if (deltaSeconds > 0) {
      star.y = (star.y + (star.speed * deltaSeconds) / window.innerHeight) % 1
    }

    const twinkle = 0.48 + Math.sin(time / 950 + star.phase) * 0.18
    context.globalAlpha = twinkle
    context.fillStyle = star.color
    context.beginPath()
    context.arc(star.x * window.innerWidth, star.y * window.innerHeight, star.radius, 0, Math.PI * 2)
    context.fill()
  }

  rings = rings.filter(ring => {
    const progress = (time - ring.startedAt) / 700
    if (progress >= 1) return false

    context.globalAlpha = (1 - progress) * 0.42
    context.strokeStyle = ring.color
    context.lineWidth = 1
    context.beginPath()
    context.arc(ring.x, ring.y, 8 + progress * 42, 0, Math.PI * 2)
    context.stroke()
    return true
  })
  if (ringCount.value !== rings.length) ringCount.value = rings.length
  context.globalAlpha = 1
}

function animate(time) {
  if (props.mode !== ASTRAL_MODES.FLOW || reducedMotionQuery.matches || hasBlockingSurface()) {
    paused.value = true
    animationFrame = 0
    lastFrameTime = 0
    return
  }

  const deltaMilliseconds = lastFrameTime ? Math.min(time - lastFrameTime, 50) : 0
  const deltaSeconds = deltaMilliseconds / 1000
  lastFrameTime = time
  activeTime += deltaMilliseconds
  pointerPosition.x += (pointerTarget.x - pointerPosition.x) * 0.08
  pointerPosition.y += (pointerTarget.y - pointerPosition.y) * 0.08
  if (canvasRef.value) {
    canvasRef.value.style.transform = `translate3d(${pointerPosition.x.toFixed(2)}px, ${pointerPosition.y.toFixed(2)}px, 0)`
  }
  drawFrame(activeTime, deltaSeconds)
  animationFrame = window.requestAnimationFrame(animate)
}

function syncMode() {
  window.cancelAnimationFrame(animationFrame)
  animationFrame = 0
  paused.value = props.mode !== ASTRAL_MODES.FLOW || reducedMotionQuery.matches || hasBlockingSurface()

  if (!context) return
  if (props.mode === ASTRAL_MODES.OFF) {
    context.clearRect(0, 0, window.innerWidth, window.innerHeight)
    rings = []
    ringCount.value = 0
    pointerTarget.x = 0
    pointerTarget.y = 0
    pointerPosition.x = 0
    pointerPosition.y = 0
    canvasRef.value.style.transform = 'translate3d(0, 0, 0)'
    lastFrameTime = 0
    return
  }
  lastFrameTime = 0
  if (paused.value) {
    drawFrame(activeTime, 0)
    return
  }
  animationFrame = window.requestAnimationFrame(animate)
}

function handleResize() {
  resizeCanvas()
  syncMode()
}

function handlePointerMove(event) {
  if (props.mode !== ASTRAL_MODES.FLOW || paused.value) return
  if (!finePointerQuery.matches) {
    pointerTarget.x = 0
    pointerTarget.y = 0
    return
  }
  pointerTarget.x = ((event.clientX / window.innerWidth) - 0.5) * 8
  pointerTarget.y = ((event.clientY / window.innerHeight) - 0.5) * 8
}

function handlePointerLeave() {
  pointerTarget.x = 0
  pointerTarget.y = 0
}

function handleBlankPointerDown(event) {
  if (props.mode !== ASTRAL_MODES.FLOW || paused.value) return
  if (!(event.target instanceof Element) || !event.target.matches(blankSurfaceSelector)) return

  rings.push({
    x: event.clientX,
    y: event.clientY,
    startedAt: activeTime,
    color: starColors[rings.length % starColors.length],
  })
  if (rings.length > 3) rings.shift()
  ringCount.value = rings.length
}

watch(() => props.mode, syncMode)
watch(() => props.intensity, handleResize)

onMounted(() => {
  resizeCanvas()
  syncMode()
  window.addEventListener('resize', handleResize, { passive: true })
  document.addEventListener('pointermove', handlePointerMove, { passive: true })
  document.addEventListener('pointerleave', handlePointerLeave, { passive: true })
  document.addEventListener('pointerdown', handleBlankPointerDown, { passive: true })
  document.addEventListener('visibilitychange', syncMode)
  reducedMotionQuery.addEventListener('change', syncMode)
  surfaceObserver = new MutationObserver(syncMode)
  surfaceObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['class'],
    childList: true,
    subtree: true,
  })
})

onUnmounted(() => {
  window.cancelAnimationFrame(animationFrame)
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('pointermove', handlePointerMove)
  document.removeEventListener('pointerleave', handlePointerLeave)
  document.removeEventListener('pointerdown', handleBlankPointerDown)
  document.removeEventListener('visibilitychange', syncMode)
  reducedMotionQuery.removeEventListener('change', syncMode)
  surfaceObserver?.disconnect()
})
</script>

<style scoped>
.astral-environment {
  --astral-backdrop: image-set(
    url('../../assets/astral/astral-atlas-medium.avif') type('image/avif'),
    url('../../assets/astral/astral-atlas-medium.webp') type('image/webp')
  );

  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-image:
    radial-gradient(circle at 12% 16%, rgba(93, 143, 139, 0.2), transparent 32%),
    radial-gradient(circle at 84% 12%, rgba(185, 163, 111, 0.14), transparent 28%),
    radial-gradient(circle at 76% 78%, rgba(167, 123, 131, 0.16), transparent 34%),
    radial-gradient(circle at 26% 88%, rgba(85, 109, 145, 0.18), transparent 36%),
    var(--astral-backdrop);
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  background-blend-mode: screen, screen, screen, screen, normal;
  opacity: 0.72;
}

.astral-environment canvas {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
}

.astral-score {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0.28;
}

.astral-score path {
  vector-effect: non-scaling-stroke;
}

.astral-environment[data-astral-intensity='strong'] .astral-score { opacity: 0.4; }
.astral-environment[data-astral-intensity='quiet'] .astral-score { opacity: 0.12; }
.astral-environment[data-astral-mode='off'] .astral-score { opacity: 0; }

.astral-environment[data-astral-intensity='strong'] {
  --astral-backdrop: image-set(
    url('../../assets/astral/astral-veil-strong.avif') type('image/avif'),
    url('../../assets/astral/astral-veil-strong.webp') type('image/webp')
  );

  opacity: 1;
}

.astral-environment[data-astral-intensity='quiet'] {
  --astral-backdrop: image-set(
    url('../../assets/astral/astral-horizon-quiet.avif') type('image/avif'),
    url('../../assets/astral/astral-horizon-quiet.webp') type('image/webp')
  );

  opacity: 0.46;
}

@media (forced-colors: active) {
  .astral-environment {
    background: Canvas;
    opacity: 1;
  }

  .astral-environment canvas,
  .astral-score {
    display: none;
  }
}
</style>
