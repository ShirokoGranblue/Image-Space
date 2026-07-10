<template>
  <main class="forbidden-page">
    <section class="forbidden-panel">
      <p class="code">403</p>
      <h1>当前域名无权访问后台管理系统</h1>
      <p>请使用管理员后台域名访问。</p>
      <el-button type="primary" @click="$router.replace('/home')">返回图片页</el-button>
    </section>
  </main>
</template>

<style scoped>
.forbidden-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  color: var(--ad-text);
}

.forbidden-panel {
  width: min(100%, 520px);
  padding: 34px;
  border: 1px solid var(--ad-line);
  border-radius: 8px;
  background: rgba(21, 25, 34, 0.92);
  box-shadow: var(--ad-shadow-soft);
  text-align: center;
}

.code {
  margin: 0 0 10px;
  color: var(--ad-coral);
  font-size: 54px;
  font-weight: 820;
  line-height: 1;
}

h1 {
  margin: 0 0 12px;
  color: var(--ad-text);
  font-size: 24px;
}

p {
  margin: 0 0 22px;
  color: var(--ad-text-soft);
}
</style>
