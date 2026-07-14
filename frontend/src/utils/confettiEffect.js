import confetti from 'canvas-confetti'

const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)'
const SIDE_ORIGINS = {
  left: { x: 0, y: 1 },
  right: { x: 1, y: 1 }
}
const DEFAULT_Z_INDEX = 2200

function prefersReducedMotion() {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return false
  return window.matchMedia(REDUCED_MOTION_QUERY).matches
}

function fireSidePair(options) {
  if (prefersReducedMotion()) return

  const shared = {
    spread: 56,
    startVelocity: 38,
    ticks: 120,
    gravity: 0.9,
    decay: 0.92,
    scalar: 0.9,
    zIndex: DEFAULT_Z_INDEX,
    disableForReducedMotion: true,
    ...options,
  }

  confetti({
    ...shared,
    angle: 60,
    origin: SIDE_ORIGINS.left,
  })
  confetti({
    ...shared,
    angle: 120,
    origin: SIDE_ORIGINS.right,
  })
}

export function fireSmallSideCannons() {
  fireSidePair({
    particleCount: 28,
    spread: 48,
    startVelocity: 32,
    ticks: 90,
    scalar: 0.75,
  })
}

export function fireMediumSideCannons() {
  fireSidePair({
    particleCount: 56,
    spread: 62,
    startVelocity: 40,
    ticks: 120,
    scalar: 0.9,
  })
}

export function fireBigSideCannons() {
  fireSidePair({
    particleCount: 130,
    spread: 78,
    startVelocity: 46,
    ticks: 150,
    scalar: 1.05,
  })
}