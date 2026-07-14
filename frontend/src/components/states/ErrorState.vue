<template>
  <section class="state-view" :class="{ 'state-view--compact': compact }" role="alert">
    <span class="state-view__label">错误</span>
    <h2>{{ title }}</h2>
    <p v-if="description">{{ description }}</p>
    <BaseButton v-if="retryLabel" variant="secondary" size="sm" @click="$emit('retry')">{{ retryLabel }}</BaseButton>
  </section>
</template>

<script setup>
import BaseButton from '../ui/BaseButton.vue'

defineProps({
  title: { type: String, default: '内容暂时无法显示' },
  description: { type: String, default: '' },
  retryLabel: { type: String, default: '重新加载' },
  compact: { type: Boolean, default: false },
})

defineEmits(['retry'])
</script>

<style scoped>
.state-view {
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding: var(--space-7) var(--space-5);
  border-block: 1px solid var(--color-border-subtle);
}
.state-view--compact { min-height: 120px; padding-block: var(--space-4); }
.state-view__label { margin-bottom: var(--space-3); color: var(--color-error); font-size: var(--text-xs); font-weight: 700; letter-spacing: 0.12em; }
h2 { margin: 0; font-size: var(--text-lg); line-height: var(--leading-lg); }
p { max-width: 42rem; margin: var(--space-2) 0 var(--space-5); color: var(--color-text-secondary); font-size: var(--text-sm); line-height: var(--leading-sm); }
</style>
