<template>
  <div class="auth-page">
    <section class="auth-shell">
      <div class="auth-hero">
        <div class="auth-kicker">Blue gallery / Light interface</div>
        <h1 class="auth-title">
          IMAGE SPACE
        </h1>
        <p class="auth-desc">轻量图片空间，专注上传、管理和浏览你的作品。</p>
      </div>

      <section class="auth-card">
        <router-link to="/home" class="auth-logo">IMAGE SPACE</router-link>
        <p class="auth-card-subtitle">进入你的图库与图片广场。</p>

        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="auth-form" @submit.prevent="handleLogin">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="输入用户名" size="large" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="输入密码" size="large" show-password @keyup.enter="handleLogin" />
          </el-form-item>
          <TurnstileWidget
            ref="turnstileRef"
            @verified="turnstileToken = $event"
            @expired="turnstileToken = ''"
            @error="turnstileToken = ''"
          />
          <el-form-item>
            <el-button type="primary" size="large" class="auth-submit" @click="handleLogin" :loading="loading">
              登录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="auth-oauth">
          <button class="oauth-btn github" @click="handleGithubLogin" :disabled="githubLoading">
            <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path fill="currentColor" d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/></svg>
            GitHub
          </button>
          <button class="oauth-btn google" @click="handleGoogleLogin" :disabled="googleLoading">
            <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/><path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/><path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/><path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/></svg>
            Google
          </button>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { login, getGithubAuthUrl, getGoogleAuthUrl } from '../api/user'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'
import TurnstileWidget from '../components/TurnstileWidget.vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const githubLoading = ref(false)
const googleLoading = ref(false)
const turnstileRef = ref(null)
const turnstileToken = ref('')

const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(async () => {
  const oauthCode = new URLSearchParams(window.location.search).get('oauth_code')
  if (oauthCode) {
    window.history.replaceState({}, '', '/login')
    try {
      const res = await api.post('/user/oauth/exchange', { code: oauthCode })
      userStore.setToken(res.data.satoken)
      await userStore.fetchUserInfo()
      ElMessage.success('欢迎回来')
      router.push('/home')
    } catch { ElMessage.error('登录失败，请重试') }
    return
  }
})

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const token = getTurnstileToken()
  if (!token) { ElMessage.warning('请完成人机验证'); return }
  loading.value = true
  try {
    const res = await login({ ...form, turnstileToken: token })
    userStore.setToken(res.data)
    await userStore.fetchUserInfo()
    ElMessage.success('欢迎回来')
    router.push('/home')
  } catch {} finally { resetTurnstile(); loading.value = false }
}

async function handleGithubLogin() {
  githubLoading.value = true
  try {
    const res = await getGithubAuthUrl()
    window.location.href = res.data.authorizeUrl
  } catch {} finally { githubLoading.value = false }
}

async function handleGoogleLogin() {
  googleLoading.value = true
  try {
    const res = await getGoogleAuthUrl()
    window.location.href = res.data.authorizeUrl
  } catch {} finally { googleLoading.value = false }
}

function getTurnstileToken() { return turnstileRef.value?.getToken?.() || turnstileToken.value }
function resetTurnstile() { turnstileToken.value = ''; turnstileRef.value?.reset?.() }
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 34px 24px 64px;
  background: var(--gray1);
}

.auth-shell {
  width: min(100%, 1040px);
  display: grid;
  grid-template-columns: minmax(300px, 0.9fr) minmax(340px, 420px);
  align-items: center;
  gap: 44px;
  animation: fadeUp 0.42s var(--ease-out);
}

.auth-hero {
  padding: 10px 0;
}

.auth-kicker {
  margin-bottom: 18px;
  font-family: var(--font-display);
  color: var(--accent);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.auth-title {
  max-width: 520px;
  font-size: clamp(40px, 7vw, 58px);
  line-height: 1.05;
  font-weight: 400;
  letter-spacing: 0.04em;
  color: var(--nav-blue);
}

.auth-desc {
  max-width: 430px;
  margin-top: 18px;
  color: var(--gray3);
  line-height: 1.9;
  font-size: 15px;
  font-weight: 300;
}

.auth-card {
  padding: 34px 32px 30px;
  background: rgba(255, 253, 248, 0.92);
  border: 1px solid rgba(229, 224, 212, 0.95);
  border-radius: 30px;
  box-shadow: 0 18px 45px rgba(30, 41, 59, 0.08);
  backdrop-filter: blur(12px);
}

.auth-logo {
  display: inline-block;
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 500;
  letter-spacing: 0.08em;
  color: var(--nav-blue);
  text-decoration: none;
}

.auth-logo:hover {
  opacity: 0.8;
}

.auth-card-subtitle {
  margin: 8px 0 24px;
  color: var(--gray3);
  font-size: 13px;
  font-weight: 300;
}

.auth-form {
  margin-top: 0;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.auth-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 300;
  color: var(--gray3) !important;
  padding-bottom: 6px;
}

.auth-form :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 16px !important;
  background: #fff !important;
}

.auth-submit {
  width: 100%;
  height: 44px;
  margin-top: 2px;
  font-size: 14px;
  font-weight: 400;
}

.auth-oauth {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.oauth-btn {
  height: 40px;
  border: 1px solid var(--gray2);
  background: #fff;
  color: var(--gray4);
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 300;
  cursor: pointer;
  transition: transform 0.16s ease, border-color 0.18s ease, background 0.18s ease, color 0.18s ease;
}

.oauth-btn:hover {
  color: var(--accent);
  border-color: #b8cfe0;
  background: #f8fbfe;
}

.oauth-btn:active {
  transform: scale(0.98);
}

.oauth-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.oauth-btn.github {
  color: #24292f;
}

.oauth-btn.google {
  color: #444;
}

@media (max-width: 520px) {
  .auth-page {
    align-items: start;
    padding: 42px 16px 48px;
  }

  .auth-shell {
    gap: 22px;
  }

  .auth-title {
    font-size: 34px;
  }

  .auth-card {
    padding: 28px 22px;
    border-radius: 20px;
  }

  .auth-oauth {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }
}
</style>
