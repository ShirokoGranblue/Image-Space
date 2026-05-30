<template>
  <ParticleBackground />
  <router-view />
  <NotificationDrawer />
  <ParticleSettings />
</template>

<script setup>
import ParticleBackground from './components/ParticleBackground.vue'
import ParticleSettings from './components/ParticleSettings.vue'
import NotificationDrawer from './components/NotificationDrawer.vue'
</script>
