<template>
  <button
    class="base-button"
    :class="[`base-button--${variant}`, `base-button--${size}`, { 'base-button--block': block }]"
    :type="type"
    :disabled="disabled || loading"
    :aria-busy="loading ? 'true' : undefined"
    @click="handleClick"
  >
    <span v-if="loading" class="base-button__spinner" aria-hidden="true" />
    <span v-else-if="$slots.icon" class="base-button__icon" aria-hidden="true"><slot name="icon" /></span>
    <span class="base-button__label"><slot /></span>
  </button>
</template>

<script setup>
const props = defineProps({
  variant: { type: String, default: 'primary', validator: value => ['primary', 'secondary', 'quiet', 'danger'].includes(value) },
  size: { type: String, default: 'md', validator: value => ['sm', 'md', 'lg'].includes(value) },
  type: { type: String, default: 'button', validator: value => ['button', 'submit', 'reset'].includes(value) },
  disabled: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  block: { type: Boolean, default: false },
})

const emit = defineEmits(['click'])

function handleClick(event) {
  if (!props.disabled && !props.loading) emit('click', event)
}
</script>

<style scoped>
.base-button {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: 0 var(--space-4);
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  font-family: var(--font-ui);
  font-size: var(--text-sm);
  font-weight: 650;
  line-height: 1;
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-standard), border-color var(--duration-fast) var(--ease-standard), color var(--duration-fast) var(--ease-standard);
}

.base-button--sm { min-height: 36px; padding-inline: var(--space-3); }
.base-button--lg { min-height: 44px; padding-inline: var(--space-5); font-size: var(--text-md); }
.base-button--block { width: 100%; }
.base-button--primary { background: var(--color-vermilion); color: var(--color-text-inverse); }
.base-button--primary:hover:not(:disabled) { background: var(--color-vermilion-hover); }
.base-button--secondary { border-color: var(--color-border-strong); background: var(--color-surface-1); color: var(--color-text-primary); }
.base-button--secondary:hover:not(:disabled),
.base-button--quiet:hover:not(:disabled) { background: var(--color-surface-2); }
.base-button--quiet { background: transparent; color: var(--color-night); }
.base-button--danger { border-color: var(--color-error); background: transparent; color: var(--color-error); }
.base-button--danger:hover:not(:disabled) { background: var(--color-error); color: var(--color-text-inverse); }
.base-button:active:not(:disabled) { transform: translateY(1px); }
.base-button:disabled { cursor: not-allowed; opacity: 0.48; }

.base-button__spinner {
  width: 1em;
  height: 1em;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: var(--radius-round);
  animation: base-button-spin 700ms linear infinite;
}

@keyframes base-button-spin { to { transform: rotate(1turn); } }

@media (max-width: 767px) {
  .base-button { min-height: 44px; }
}
</style>
