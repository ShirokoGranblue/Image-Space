<template>
  <div class="login-page">
    <div class="login-bg"></div>
    <div class="login-frame">
      <div class="login-brand">
        <h1 class="wordmark">ImageSpace</h1>
        <p class="tagline">整理、浏览和分享你的图片</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="login-form">
        <el-form-item label="Username" prop="username">
          <el-input v-model="form.username" placeholder="你的用户名" size="large" />
        </el-form-item>
        <el-form-item label="Password" prop="password">
          <el-input v-model="form.password" type="password" placeholder="••••••••" size="large"
            @keyup.enter="handleLogin" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" @click="handleLogin" :loading="loading">
            进入图库
          </el-button>
        </el-form-item>
      </el-form>
      <p class="footer-link">
        还没有账号？<router-link to="/register">创建账号</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/user'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form)
    userStore.setToken(res.data)
    await userStore.fetchUserInfo()
    ElMessage.success('欢迎回来')
    router.push('/home')
  } catch {} finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: linear-gradient(135deg, #f5f7fb 0%, #eaf2ff 100%);
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(37,99,235,0.08), transparent 44%);
}

.login-frame {
  position: relative;
  width: 420px;
  padding: 44px 40px;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.12);
  z-index: 1;
  animation: frameIn 0.6s ease-out;
}

@keyframes frameIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-brand {
  text-align: center;
  margin-bottom: var(--space-xl);
}

.wordmark {
  font-family: var(--font-display);
  font-size: 38px;
  font-weight: 750;
  color: var(--text-primary);
  letter-spacing: 0;
  margin: 0;
  line-height: 1.1;
}

.tagline {
  font-family: var(--font-display);
  font-size: 15px;
  font-style: normal;
  color: var(--text-muted);
  margin-top: var(--space-sm);
  letter-spacing: 0;
}

.login-form {
  margin-top: var(--space-sm);
}

.login-form :deep(.el-form-item__label) {
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 0;
  text-transform: none;
  color: var(--text-muted) !important;
  padding-bottom: var(--space-xs);
}

.login-btn {
  width: 100%;
  height: 48px;
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 650;
  letter-spacing: 0;
  margin-top: var(--space-sm);
  border-radius: var(--radius-md);
}

.footer-link {
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
  margin-top: var(--space-lg);
}

.footer-link a {
  color: var(--accent);
  font-weight: 500;
}

.footer-link a:hover {
  color: var(--accent-glow);
}

@media (max-width: 480px) {
  .login-frame {
    width: calc(100vw - 32px);
    padding: 32px 20px;
    border-radius: var(--radius-lg);
  }
  .wordmark { font-size: 30px; }
  .tagline { font-size: 14px; }
}
</style>
