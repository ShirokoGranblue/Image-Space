<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-blob bg-blob-1"></div>
      <div class="bg-blob bg-blob-2"></div>
      <div class="bg-blob bg-blob-3"></div>
    </div>
    <div class="login-frame">
      <div class="login-brand">
        <div class="brand-icon">
          <el-icon :size="36"><PictureFilled /></el-icon>
        </div>
        <h1 class="wordmark">ImageSpace</h1>
      </div>
      <el-tabs v-model="loginMode" class="login-tabs">
        <el-tab-pane label="密码登录" name="password"></el-tab-pane>
        <el-tab-pane label="邮箱登录" name="code"></el-tab-pane>
      </el-tabs>
      <div v-show="loginMode === 'password'">
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="login-form" @submit.prevent="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="输入你的用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="••••••••" size="large"
            @keyup.enter="handleLogin" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" @click="handleLogin" :loading="loading">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      </div>
      <div v-show="loginMode === 'code'" class="login-form">
        <el-form-item label="邮箱">
          <el-input v-model="codeForm.email" placeholder="your@email.com" size="large" :prefix-icon="Message" />
        </el-form-item>
        <el-form-item label="图形验证码">
          <div style="display:flex;gap:8px;align-items:center">
            <el-input v-model="captchaCode" placeholder="4位验证码" size="large" maxlength="4" style="flex:1" />
            <img :src="captchaImage" @click="fetchCaptcha" style="height:40px;cursor:pointer;border-radius:4px;border:1px solid #ddd" title="点击刷新" />
          </div>
        </el-form-item>
        <el-form-item label="验证码">
          <div style="display:flex;gap:8px;width:100%">
            <el-input v-model="codeForm.code" placeholder="6位数字" size="large" maxlength="6" style="flex:1" />
            <el-button size="large" @click="handleSendCode" :loading="sending" :disabled="countdown > 0" style="min-width:120px">
              {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" @click="handleCodeLogin" :loading="loading">
            验证并登录
          </el-button>
        </el-form-item>
      </div>
      <div class="oauth-section">
        <div class="divider"><span>第三方登录</span></div>
        <div class="oauth-btns">
          <el-button size="large" class="github-btn" @click="handleGithubLogin" :loading="githubLoading">
            <svg class="github-icon" viewBox="0 0 16 16" fill="currentColor"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/></svg>
            GitHub 登录
          </el-button>
          <el-button size="large" class="google-btn" @click="handleGoogleLogin" :loading="googleLoading">
            <svg class="google-icon" viewBox="0 0 24 24"><path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/><path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/><path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/><path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/></svg>
            Google 登录
          </el-button>
        </div>
      </div>
      <p class="footer-link">
        还没有账号？<router-link to="/register">创建账号</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { login, sendCode, loginByCode, getCaptcha, getGithubAuthUrl, getGoogleAuthUrl } from '../api/user'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const loginMode = ref('password')
const sending = ref(false)
const countdown = ref(0)
const githubLoading = ref(false)
const googleLoading = ref(false)
let countdownTimer = null

const codeForm = reactive({
  email: '',
  code: ''
})

const captchaId = ref('')
const captchaImage = ref('')
const captchaCode = ref('')
const captchaExpiry = ref(0)
let captchaTimerId = null

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

watch(loginMode, (mode) => {
  if (mode === 'code') {
    fetchCaptcha()
  }
})

onMounted(() => {
  const token = new URLSearchParams(window.location.search).get('satoken')
  if (token) {
    userStore.setToken(token)
    window.history.replaceState({}, '', '/login')
    userStore.fetchUserInfo().then(() => {
      ElMessage.success('欢迎回来')
      router.push('/home')
    })
    return
  }
  fetchCaptcha()
})

onUnmounted(() => {
  clearCaptchaTimer()
  if (countdownTimer) clearInterval(countdownTimer)
})

function startCaptchaTimer() {
  clearCaptchaTimer()
  captchaExpiry.value = 60
  captchaTimerId = setInterval(() => {
    captchaExpiry.value--
    if (captchaExpiry.value <= 0) {
      clearCaptchaTimer()
      fetchCaptcha()
    }
  }, 1000)
}

function clearCaptchaTimer() {
  if (captchaTimerId) { clearInterval(captchaTimerId); captchaTimerId = null }
}

async function fetchCaptcha() {
  try {
    const res = await getCaptcha()
    captchaId.value = res.data.captchaId
    captchaImage.value = res.data.captchaImage
    startCaptchaTimer()
  } catch {}
}

async function handleSendCode() {
  if (!codeForm.email) { ElMessage.warning('请输入邮箱'); return }
  if (!captchaCode.value) { ElMessage.warning('请输入图形验证码'); return }
  sending.value = true
  try {
    await sendCode({ email: codeForm.email.trim(), captchaId: captchaId.value, captchaCode: captchaCode.value })
    ElMessage.success('验证码已发送')
    countdown.value = 60
    countdownTimer = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(countdownTimer) }, 1000)
  } catch {
    fetchCaptcha()
  } finally { sending.value = false }
}

async function handleCodeLogin() {
  if (!codeForm.email) { ElMessage.warning('请输入邮箱'); return }
  if (!codeForm.code) { ElMessage.warning('请输入验证码'); return }
  loading.value = true
  try {
    const res = await loginByCode({ email: codeForm.email.trim(), code: codeForm.code.trim() })
    userStore.setToken(res.data)
    await userStore.fetchUserInfo()
    ElMessage.success('欢迎回来')
    router.push('/home')
  } catch {} finally { loading.value = false }
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
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8f0fe 30%, #f5f7fb 100%);
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

/* Decorative blobs */
.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}
.bg-blob-1 {
  width: 440px; height: 440px;
  background: rgba(37, 99, 235, 0.1);
  top: -120px; right: -100px;
  animation: blobFloat 20s ease-in-out infinite;
}
.bg-blob-2 {
  width: 320px; height: 320px;
  background: rgba(56, 189, 248, 0.08);
  bottom: -80px; left: -60px;
  animation: blobFloat 17s ease-in-out 2s infinite reverse;
}
.bg-blob-3 {
  width: 200px; height: 200px;
  background: rgba(99, 102, 241, 0.07);
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  animation: blobFloat 22s ease-in-out 5s infinite;
}

@keyframes blobFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -40px) scale(1.05); }
  50% { transform: translate(-20px, 20px) scale(0.95); }
  75% { transform: translate(-15px, -30px) scale(1.02); }
}

.login-frame {
  position: relative;
  width: 420px;
  padding: 44px 40px;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-2xl);
  z-index: 1;
  animation: frameIn 0.55s var(--ease-out);
}

@keyframes frameIn {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.login-brand {
  text-align: center;
  margin-bottom: var(--space-xl);
}

.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--accent), var(--accent-glow));
  color: #fff;
  margin-bottom: var(--space-md);
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.25);
}

.wordmark {
  font-family: var(--font-display);
  font-size: 38px;
  font-weight: 750;
  color: var(--text-primary);
  letter-spacing: -0.3px;
  margin: 0;
  line-height: 1.15;
}

.tagline {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--text-muted);
  margin-top: var(--space-sm);
  letter-spacing: 0;
}

.login-form {
  margin-top: var(--space-sm);
}

.login-form :deep(.el-form-item__label) {
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 550;
  color: var(--text-secondary) !important;
  padding-bottom: 6px;
}

.login-form :deep(.el-input__prefix) {
  color: var(--text-muted);
}

.login-btn {
  width: 100%;
  height: 48px;
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 650;
  margin-top: var(--space-sm);
  border-radius: var(--radius-md);
  transition: transform 0.15s var(--ease-out), box-shadow 0.15s var(--ease-out);
}
.login-btn:not(.is-loading):hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.3);
}
.login-btn:not(.is-loading):active {
  transform: translateY(0);
}

.oauth-section {
  margin-top: var(--space-lg);
}

.divider {
  display: flex;
  align-items: center;
  color: var(--text-muted);
  font-size: 12px;
  margin-bottom: var(--space-md);
}
.divider::before, .divider::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid var(--border-subtle);
}
.divider span {
  padding: 0 12px;
}

.oauth-btns {
  display: flex;
  gap: 10px;
}
.github-btn {
  flex: 1;
  height: 44px;
  font-size: 14px;
  font-weight: 550;
  border-radius: var(--radius-md);
  background: #24292f;
  color: #fff;
  border: none;
}
.github-btn:hover {
  background: #1b1f23;
  color: #fff;
}
.github-btn:active {
  background: #0d1117;
}
.github-icon {
  width: 18px;
  height: 18px;
  margin-right: 6px;
}

.google-btn {
  flex: 1;
  height: 44px;
  font-size: 14px;
  font-weight: 550;
  border-radius: var(--radius-md);
  background: #fff;
  color: #444;
  border: 1px solid #dadce0;
}
.google-btn:hover {
  background: #f8f9fa;
  color: #222;
  border-color: #c0c4c8;
}
.google-icon {
  width: 18px;
  height: 18px;
  margin-right: 8px;
}

.footer-link {
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
  margin-top: var(--space-lg);
}
.footer-link a {
  color: var(--accent);
  font-weight: 550;
}
.footer-link a:hover { color: var(--accent-glow); }

.login-tabs { margin-bottom: var(--space-md); }
.login-tabs :deep(.el-tabs__header) { margin-bottom: 0; }
.login-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.login-tabs :deep(.el-tabs__item) {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 550;
}

@media (max-width: 480px) {
  .login-frame {
    width: calc(100vw - 32px);
    padding: 32px 24px;
    border-radius: var(--radius-lg);
  }
  .wordmark { font-size: 30px; }
  .tagline { font-size: 14px; }
  .brand-icon { width: 56px; height: 56px; }
}
</style>
