<template>
  <div class="auth-page">
    <div class="auth-layout" :class="`auth-layout--${formWidth}`">
      <aside class="auth-aside">
        <slot name="aside" />
      </aside>

      <main class="auth-main">
        <div class="auth-main__inner">
          <header class="auth-header">
            <slot name="header" />
          </header>
          <section class="auth-body">
            <slot />
          </section>
          <footer class="auth-footer">
            <slot name="footer" />
          </footer>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
defineProps({
  formWidth: {
    type: String,
    default: 'narrow',
    validator: value => ['narrow', 'wide'].includes(value),
  },
})
</script>

<style scoped>
.auth-page {
  height: 100dvh;
  display: grid;
  place-items: center;
  padding: 32px;
  overflow: hidden;
  background: var(--color-canvas-muted);
  color: var(--color-text-primary);
}

.auth-layout {
  width: min(100%, 1120px);
  height: min(880px, calc(100dvh - 64px));
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(280px, 0.85fr) minmax(420px, 1.15fr);
  grid-template-rows: minmax(0, 1fr);
  background: var(--color-surface-1);
}

.auth-aside,
.auth-main {
  min-width: 0;
  min-height: 0;
}

.auth-aside {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 48px;
  padding: clamp(32px, 5vw, 64px);
  overflow-y: auto;
  scrollbar-gutter: stable;
  border-right: 1px solid var(--color-border-subtle);
  background: var(--color-canvas-muted);
}

.auth-aside :deep(.auth-aside-copy) {
  margin-block: auto;
}

.auth-aside :deep(.auth-aside-copy--plain .auth-aside-title) {
  margin-top: 0;
}

.auth-aside :deep(.auth-aside-eyebrow) {
  display: inline-block;
  padding-left: var(--space-3);
  border-left: 2px solid var(--color-vermilion);
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  font-weight: 700;
  letter-spacing: .04em;
}

.auth-aside :deep(.auth-aside-title) {
  max-width: 28rem;
  margin: 14px 0 18px;
  color: var(--color-text-primary);
  font-family: var(--font-title);
  font-size: clamp(48px, 4.6vw, 64px);
  font-weight: 600;
  line-height: 1.08;
  text-wrap: balance;
}

.auth-aside :deep(.auth-aside-description) {
  max-width: 24rem;
  color: var(--color-text-primary);
  font-size: 18px;
  line-height: var(--leading-lg);
}

.auth-aside :deep(.auth-capabilities) {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  color: var(--color-text-primary);
  font-size: 16px;
  list-style: none;
}

.auth-aside :deep(.auth-capabilities li) {
  padding-top: 10px;
  border-top: 1px solid var(--color-border-subtle);
}

.auth-main {
  display: flex;
  min-height: 0;
  overflow-y: auto;
  scrollbar-gutter: stable;
  scrollbar-color: var(--color-text-secondary) var(--color-canvas-muted);
  scrollbar-width: auto;
  background: var(--color-surface-1);
}

.auth-main::-webkit-scrollbar {
  width: 12px;
}

.auth-main::-webkit-scrollbar-track {
  border-left: 1px solid var(--color-border-strong);
  background: var(--color-canvas-muted);
}

.auth-main::-webkit-scrollbar-thumb {
  border: 2px solid var(--color-canvas-muted);
  border-radius: 999px;
  background: var(--color-text-secondary);
}

.auth-main::-webkit-scrollbar-thumb:hover {
  background: var(--color-text-secondary);
}

.auth-main__inner {
  width: 100%;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  padding: clamp(32px, 5vw, 64px);
}

.auth-main__inner::after {
  content: '';
  flex: 0 0 32px;
}

.auth-layout--narrow .auth-header,
.auth-layout--narrow .auth-body,
.auth-layout--narrow .auth-footer {
  width: min(100%, var(--auth-form-narrow));
  margin-inline: auto;
}

.auth-layout--wide .auth-header,
.auth-layout--wide .auth-body,
.auth-layout--wide .auth-footer {
  width: min(100%, var(--auth-form-wide));
  margin-inline: auto;
}

.auth-footer {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border-subtle);
}

.auth-header :deep(.auth-kicker) {
  display: block;
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: var(--leading-sm);
}

.auth-header :deep(.auth-title) {
  margin: 10px 0 12px;
  color: var(--color-text-primary);
  font-family: var(--font-title);
  font-size: clamp(40px, 4vw, 56px);
  font-weight: 600;
  line-height: 1.04;
  text-wrap: balance;
}

.auth-header :deep(.auth-description) {
  max-width: 42rem;
  color: var(--color-text-secondary);
  font-size: 16px;
  line-height: var(--leading-lg);
}

.auth-footer:empty {
  display: none;
}

.auth-body :deep(.auth-form) {
  display: grid;
  gap: 0;
}

.auth-body :deep(.auth-form .el-form-item) {
  margin-bottom: 28px;
}

.auth-body :deep(.auth-form .el-form-item__label) {
  padding-bottom: 8px;
  color: var(--color-text-primary) !important;
  font-size: 14px;
  font-weight: 600;
  line-height: var(--leading-sm);
}

.auth-body :deep(.auth-form .el-form-item__error) {
  position: absolute;
  top: 100%;
  left: 0;
  padding-top: 4px;
  color: var(--color-error);
  font-size: 13px;
  line-height: var(--leading-sm);
}

.auth-body :deep(.auth-form .el-input__wrapper) {
  min-height: var(--control-height-lg);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm);
  background: var(--color-surface-1);
  box-shadow: none;
}

.auth-body :deep(.auth-form .el-input__wrapper.is-focus) {
  border-color: var(--color-urban);
  box-shadow: none;
}

.auth-body :deep(.auth-form .el-input__inner) {
  color: var(--color-text-primary);
}

.auth-body :deep(.captcha-row-inline),
.auth-body :deep(.code-row) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  width: 100%;
}

.auth-body :deep(.captcha-image),
.auth-body :deep(.oauth-btn) {
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm);
  background: var(--color-surface-2);
  color: var(--color-text-primary);
  cursor: pointer;
  font-family: var(--font-ui);
}

.auth-body :deep(.captcha-image) {
  width: 132px;
  min-height: var(--control-height-lg);
  overflow: hidden;
}

.auth-body :deep(.captcha-image img) {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.auth-body :deep(.send-code-btn) {
  min-width: 116px;
  min-height: var(--control-height-lg);
}

.auth-body :deep(.captcha-image:disabled),
.auth-body :deep(.oauth-btn:disabled) {
  cursor: not-allowed;
  opacity: 0.58;
}

.auth-body :deep(.captcha-image:hover:not(:disabled)),
.auth-body :deep(.oauth-btn:hover:not(:disabled)) {
  border-color: var(--color-border-strong);
  background: var(--color-surface-1);
}

.auth-body :deep(.submit-row) {
  margin-top: 4px !important;
  margin-bottom: 0 !important;
}

.auth-body :deep(.login-btn) {
  width: 100%;
  min-height: var(--control-height-lg);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

@media (max-width: 900px) {
  .auth-page {
    height: auto;
    min-height: 100dvh;
    place-items: start center;
    padding: 24px;
    overflow: visible;
  }

  .auth-layout {
    width: 100%;
    height: auto;
    max-width: 720px;
    min-width: 0;
    min-height: 0;
    grid-template-columns: 1fr;
    grid-template-rows: auto;
  }

  .auth-main {
    overflow: visible;
    scrollbar-gutter: auto;
  }

  .auth-aside {
    gap: 24px;
    padding: 28px 32px;
    overflow: visible;
    scrollbar-gutter: auto;
    border-right: 0;
    border-bottom: 1px solid var(--color-border-subtle);
  }

  .auth-aside :deep([data-auth-capabilities]) {
    display: none;
  }

  .auth-aside :deep(.auth-aside-title) {
    max-width: 32rem;
    font-size: clamp(38px, 5.5vw, 50px);
  }

  .auth-aside :deep(.auth-aside-description) {
    max-width: 36rem;
  }

  .auth-main__inner {
    min-height: 0;
    padding: 40px 32px;
  }

  .auth-body :deep(.captcha-row-inline),
  .auth-body :deep(.code-row) {
    grid-template-columns: 1fr;
  }

  .auth-body :deep(.captcha-image),
  .auth-body :deep(.send-code-btn) {
    width: 100%;
  }
}

@media (max-width: 479px) {
  .auth-page {
    padding: 16px;
  }

  .auth-aside {
    gap: 20px;
    padding: 24px 20px;
  }

  .auth-aside :deep(.auth-aside-title) {
    margin-top: 8px;
    font-size: 40px;
    line-height: 1.08;
  }

  .auth-main__inner {
    padding: 28px 20px 32px;
  }

  .auth-header :deep(.auth-title) {
    font-size: 36px;
  }
}
</style>
