<template>
  <span class="user-identity" :title="identityTitle">
    <strong class="user-identity__name">{{ displayName }}</strong>
    <span v-if="handle" class="user-identity__handle">{{ handle }}</span>
    <span v-if="time" class="user-identity__separator" aria-hidden="true">·</span>
    <span v-if="time" class="user-identity__time">{{ time }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatUserHandle, getUserDisplayName } from '../../utils/userIdentity'

const props = defineProps({
  displayName: { type: String, default: '' },
  username: { type: String, default: '' },
  time: { type: String, default: '' },
  fallback: { type: String, default: '用户' },
})

const displayName = computed(() => getUserDisplayName(props, props.fallback))
const handle = computed(() => formatUserHandle(props.username))
const identityTitle = computed(() => [displayName.value, handle.value, props.time].filter(Boolean).join(' · '))
</script>

<style scoped>
.user-identity {
  display: inline-flex;
  min-width: 0;
  max-width: 100%;
  align-items: baseline;
  gap: .32em;
  line-height: inherit;
  white-space: nowrap;
}

.user-identity__name {
  min-width: 0;
  overflow: hidden;
  color: var(--color-text-primary);
  font: inherit;
  font-weight: 700;
  text-overflow: ellipsis;
}

.user-identity__handle,
.user-identity__separator,
.user-identity__time {
  flex: 0 1 auto;
  overflow: hidden;
  color: var(--color-text-muted);
  font-weight: 400;
  text-overflow: ellipsis;
}

.user-identity__separator { flex: 0 0 auto; }
</style>
