<template>
  <AppShell :variant="shellVariant" width="full">
    <router-view />
  </AppShell>
  <NotificationDrawer />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from './components/layout/AppShell.vue'
import NotificationDrawer from './components/NotificationDrawer.vue'

const route = useRoute()
const shellVariant = computed(() => {
  if (route.path === '/login' || route.path === '/register') return 'auth'
  if (route.path.startsWith('/admin/')) return 'admin'
  return 'default'
})
</script>
