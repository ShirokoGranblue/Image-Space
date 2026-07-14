<template>
  <main class="not-found-page">
    <section class="not-found-panel">
      <p class="code">404</p>
      <h1>页面不存在</h1>
      <p>该地址没有对应页面，分类管理已整合到图片页。</p>
      <el-button type="primary" @click="$router.replace('/home')">返回图片页</el-button>
    </section>
  </main>
</template>

<style scoped>
.not-found-page { min-height: 100vh; display: grid; place-items: center; padding: var(--space-5); color: var(--color-text-primary); }
.not-found-panel { width: min(100%, 520px); padding: var(--space-6); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-sm); background: var(--color-surface-1); text-align: center; }
.code { margin: 0 0 var(--space-2); color: var(--color-vermilion); font-family: var(--font-title); font-size: 54px; line-height: 1; }
h1 { margin: 0 0 var(--space-3); font-family: var(--font-title); font-size: var(--text-xl); font-weight: 500; }
p { margin: 0 0 var(--space-5); color: var(--color-text-secondary); }
</style>
