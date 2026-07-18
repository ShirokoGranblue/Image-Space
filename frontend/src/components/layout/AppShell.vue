<template>
  <div class="app-shell" :class="`app-shell--${variant}`">
    <a class="app-shell__skip" href="#app-content">跳到主要内容</a>
    <slot v-if="withHeader" name="header" />
    <div id="app-content" class="app-shell__content" :data-width="width" tabindex="-1">
      <slot />
    </div>
    <slot name="footer" />
  </div>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    default: 'default',
    validator: value => ['default', 'auth', 'admin'].includes(value),
  },
  width: {
    type: String,
    default: 'full',
    validator: value => ['wide', 'standard', 'reading', 'full'].includes(value),
  },
  withHeader: {
    type: Boolean,
    default: false,
  },
})
</script>

<style scoped>
.app-shell {
  position: relative;
  z-index: 1;
  min-height: 100dvh;
  background: transparent;
  color: var(--color-text-primary);
}

.app-shell--auth {
  background: transparent;
}

.app-shell--admin {
  background: var(--color-surface-1);
}

.app-shell__skip {
  position: fixed;
  top: var(--space-3);
  left: var(--space-3);
  z-index: var(--layer-floating);
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  background: var(--color-surface-1);
  color: var(--color-text-primary);
  font-family: var(--font-ui);
  transform: translateY(calc(-100% - var(--space-5)));
  transition: transform var(--duration-fast) var(--ease-standard);
}

.app-shell__skip:focus {
  transform: translateY(0);
}

.app-shell__content {
  min-height: 100dvh;
  margin-inline: auto;
}

.app-shell__content:focus {
  outline: none;
  box-shadow: none;
}

.app-shell__content[data-width='wide'] {
  width: min(100%, var(--page-wide));
  padding-inline: var(--page-gutter);
}

.app-shell__content[data-width='standard'] {
  width: min(100%, var(--page-standard));
  padding-inline: var(--page-gutter);
}

.app-shell__content[data-width='reading'] {
  width: min(100%, var(--page-reading));
  padding-inline: var(--page-gutter);
}
</style>
