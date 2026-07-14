<template>
  <button
    class="base-icon-button"
    :class="[`base-icon-button--${variant}`, `base-icon-button--${size}`]"
    type="button"
    :aria-label="label"
    :aria-pressed="pressed === undefined ? undefined : String(pressed)"
    :disabled="disabled"
    @click="handleClick"
  >
    <slot />
  </button>
</template>

<script setup>
const props = defineProps({
  label: { type: String, required: true },
  variant: { type: String, default: 'quiet', validator: value => ['quiet', 'secondary', 'inverse'].includes(value) },
  size: { type: String, default: 'md', validator: value => ['sm', 'md', 'lg'].includes(value) },
  disabled: { type: Boolean, default: false },
  pressed: { type: Boolean, default: undefined },
})

const emit = defineEmits(['click'])

function handleClick(event) {
  if (!props.disabled) emit('click', event)
}
</script>

<style scoped>
.base-icon-button {
  width: 40px;
  height: 40px;
  display: inline-grid;
  place-items: center;
  flex: 0 0 auto;
  padding: 0;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-primary);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-standard), border-color var(--duration-fast) var(--ease-standard), color var(--duration-fast) var(--ease-standard);
}

.base-icon-button--sm { width: 36px; height: 36px; }
.base-icon-button--lg { width: 44px; height: 44px; }
.base-icon-button--quiet:hover:not(:disabled) { background: var(--color-surface-2); }
.base-icon-button--secondary { border-color: var(--color-border-subtle); background: var(--color-surface-1); }
.base-icon-button--secondary:hover:not(:disabled) { border-color: var(--color-border-strong); }
.base-icon-button--inverse { background: var(--color-viewer-surface); color: var(--color-text-inverse); }
.base-icon-button:active:not(:disabled) { transform: translateY(1px); }
.base-icon-button:disabled { cursor: not-allowed; opacity: 0.48; }

@media (max-width: 767px) {
  .base-icon-button { width: 44px; height: 44px; }
}
</style>
