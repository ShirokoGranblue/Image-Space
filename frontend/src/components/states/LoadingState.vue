<template>
  <div class="state-view state-view--loading" :class="{ 'state-view--compact': compact }" role="status" aria-live="polite">
    <span class="state-view__loader" aria-hidden="true" />
    <span>{{ label }}</span>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, default: '正在加载' },
  compact: { type: Boolean, default: false },
})
</script>

<style scoped>
.state-view {
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  padding: var(--space-7) var(--space-4);
  color: var(--color-text-secondary);
  font-family: var(--font-ui);
  font-size: var(--text-sm);
}
.state-view--compact { min-height: 96px; padding-block: var(--space-4); }
.state-view__loader {
  width: 18px;
  height: 18px;
  border: 1px solid var(--color-border-strong);
  border-top-color: var(--color-vermilion);
  border-radius: var(--radius-round);
  animation: state-spin 800ms linear infinite;
}
@keyframes state-spin { to { transform: rotate(1turn); } }

@media (prefers-reduced-motion: reduce) {
  .state-view__loader { animation: none; }
}
</style>
