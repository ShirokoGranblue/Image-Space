<template>
  <Teleport to="body">
    <div class="astral-mode-control">
      <Transition name="astral-mode-menu">
        <div v-if="open" class="astral-mode-menu" role="group" aria-label="星象动效模式">
          <button
            v-for="option in options"
            :key="option.value"
            type="button"
            :aria-pressed="String(mode === option.value)"
            @click="selectMode(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
      </Transition>

      <button
        class="astral-mode-trigger"
        type="button"
        aria-label="Astral modes"
        aria-haspopup="true"
        :aria-expanded="String(open)"
        @click="open = !open"
      >
        Astral modes
      </button>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
import { ASTRAL_MODES, ASTRAL_MODE_VALUES } from './astralSystem'

defineProps({
  mode: {
    type: String,
    required: true,
    validator: value => ASTRAL_MODE_VALUES.includes(value),
  },
})

const emit = defineEmits(['update:mode'])
const open = ref(false)
const options = [
  { value: ASTRAL_MODES.FLOW, label: '流光' },
  { value: ASTRAL_MODES.QUIET, label: '静谧' },
  { value: ASTRAL_MODES.OFF, label: '熄灭' },
]

function selectMode(mode) {
  emit('update:mode', mode)
  open.value = false
}
</script>

<style scoped>
.astral-mode-control {
  position: fixed;
  right: var(--space-4);
  bottom: var(--space-4);
  z-index: var(--layer-floating);
  font-family: var(--font-ui);
}

.astral-mode-trigger,
.astral-mode-menu button {
  min-height: 44px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm);
  background: rgba(25, 28, 37, 0.9);
  color: var(--color-text-secondary);
  font-size: var(--text-xs);
  font-weight: 500;
  cursor: pointer;
  transition:
    border-color var(--duration-fast) var(--ease-standard),
    color var(--duration-fast) var(--ease-standard),
    transform var(--duration-fast) var(--ease-standard);
}

.astral-mode-trigger {
  padding: 0 var(--space-3);
  box-shadow: var(--shadow-float);
  letter-spacing: 0.04em;
}

.astral-mode-menu {
  position: absolute;
  right: 0;
  bottom: calc(100% + var(--space-2));
  display: grid;
  min-width: 112px;
  gap: var(--space-1);
  padding: var(--space-2);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  background: rgba(25, 28, 37, 0.94);
  box-shadow: var(--shadow-float);
  transform-origin: 100% 100%;
}

.astral-mode-menu button {
  width: 100%;
  padding: 0 var(--space-3);
  background: transparent;
  text-align: left;
}

.astral-mode-menu button[aria-pressed='true'] {
  border-color: var(--astral-gold);
  color: var(--astral-starlight);
}

.astral-mode-trigger:focus-visible,
.astral-mode-menu button:focus-visible {
  outline: 2px solid var(--astral-gold);
  outline-offset: 2px;
}

.astral-mode-trigger:active,
.astral-mode-menu button:active {
  transform: scale(0.97);
}

.astral-mode-menu-enter-active,
.astral-mode-menu-leave-active {
  transition:
    opacity var(--duration-standard) var(--ease-standard),
    transform var(--duration-standard) var(--ease-standard);
}

.astral-mode-menu-enter-from,
.astral-mode-menu-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@media (prefers-reduced-motion: reduce) {
  .astral-mode-trigger,
  .astral-mode-menu button,
  .astral-mode-menu-enter-active,
  .astral-mode-menu-leave-active {
    transition: none;
  }
}

@media (forced-colors: active) {
  .astral-mode-trigger,
  .astral-mode-menu,
  .astral-mode-menu button {
    border-color: ButtonText;
    background: Canvas;
    color: ButtonText;
    box-shadow: none;
  }

  .astral-mode-menu button[aria-pressed='true'] {
    border-color: Highlight;
    color: Highlight;
  }
}
</style>
