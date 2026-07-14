<template>
  <section class="state-view" :class="{ 'state-view--compact': compact }" aria-labelledby="empty-state-title">
    <span class="state-view__mark" aria-hidden="true" />
    <h2 id="empty-state-title">{{ title }}</h2>
    <p v-if="description">{{ description }}</p>
    <div v-if="$slots.action" class="state-view__action"><slot name="action" /></div>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: '暂无内容' },
  description: { type: String, default: '' },
  compact: { type: Boolean, default: false },
})
</script>

<style scoped>
.state-view {
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-7) var(--space-4);
  text-align: center;
}
.state-view--compact { min-height: 120px; padding-block: var(--space-4); }
.state-view__mark { width: 48px; height: 36px; margin-bottom: var(--space-4); border: 1px solid var(--color-border-strong); border-top-color: var(--color-vermilion); }
h2 { margin: 0; font-size: var(--text-lg); line-height: var(--leading-lg); }
p { max-width: 34rem; margin: var(--space-2) 0 0; color: var(--color-text-secondary); font-size: var(--text-sm); line-height: var(--leading-sm); }
.state-view__action { margin-top: var(--space-5); }
</style>
