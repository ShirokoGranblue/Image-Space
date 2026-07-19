<template>
  <AstralEnvironment
    v-if="astralIntensity"
    :intensity="astralIntensity"
    :mode="astralMode"
  />
  <AppShell :variant="shellVariant" width="full">
    <router-view />
  </AppShell>
  <NotificationDrawer />
  <AstralModeControl
    v-if="astralIntensity"
    :mode="astralMode"
    @update:mode="astralMode = $event"
  />
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AstralEnvironment from './components/astral/AstralEnvironment.vue'
import AstralModeControl from './components/astral/AstralModeControl.vue'
import { ASTRAL_MODE_VALUES, DEFAULT_ASTRAL_MODE } from './components/astral/astralSystem'
import AppShell from './components/layout/AppShell.vue'
import NotificationDrawer from './components/NotificationDrawer.vue'

const route = useRoute()
const allowedAstralModes = new Set(ASTRAL_MODE_VALUES)
const storedAstralMode = localStorage.getItem('astral-mode')
const astralMode = ref(allowedAstralModes.has(storedAstralMode) ? storedAstralMode : DEFAULT_ASTRAL_MODE)

watch(astralMode, mode => localStorage.setItem('astral-mode', mode))

const astralIntensity = computed(() => {
  if (route.path.startsWith('/admin/')) return null
  if (route.path === '/home') return 'quiet'
  if (
    route.path === '/square'
    || route.path.startsWith('/image/')
    || route.path.startsWith('/profile/')
  ) return 'medium'
  return 'strong'
})

const shellVariant = computed(() => {
  if (route.path === '/login' || route.path === '/register') return 'auth'
  if (route.path.startsWith('/admin/')) return 'admin'
  return 'default'
})
</script>
