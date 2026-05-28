<template>
  <div class="auth-page">
    <!-- Left: Form -->
    <div class="auth-form-side">
      <div class="auth-form-wrap">
        <router-link to="/home" class="auth-logo">ImageSpace</router-link>
        <h1 class="auth-title">欢迎回来</h1>

        <!-- Login mode tabs -->
        <div class="auth-tabs">
          <button
            class="auth-tab"
            :class="{ active: loginMode === 'password' }"
            @click="loginMode = 'password'"
          >密码登录</button>
          <button
            class="auth-tab"
            :class="{ active: loginMode === 'email' }"
            @click="loginMode = 'email'"
          >邮箱登录</button>
        </div>

        <!-- Password login -->
        <el-form v-if="loginMode === 'password'" :model="form" :rules="rules" ref="formRef" label-position="top" class="auth-form" @submit.prevent="handleLogin">
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

        <!-- Email code login -->
        <el-form v-if="loginMode === 'email'" :model="codeForm" label-position="top" class="auth-form" @submit.prevent="handleCodeLogin">
          <el-form-item label="邮箱">
            <el-input v-model="codeForm.email" placeholder="your@email.com" size="large" />
          </el-form-item>
          <el-form-item label="验证码">
            <div class="captcha-row">
              <el-input v-model="captchaCode" placeholder="图形验证码" size="large" />
              <img v-if="captchaImage" :src="captchaImage" class="captcha-img" @click="fetchCaptcha" alt="验证码" />
            </div>
          </el-form-item>
          <el-form-item label="邮箱验证码">
            <div class="captcha-row">
              <el-input v-model="codeForm.code" placeholder="6位验证码" size="large" />
              <el-button size="large" class="code-btn" :disabled="countdown > 0" :loading="sending" @click="handleSendCode">
                {{ countdown > 0 ? `${countdown}s` : '发送' }}
              </el-button>
            </div>
          </el-form-item>
          <TurnstileWidget
            ref="codeTurnstileRef"
            @verified="codeTurnstileToken = $event"
            @expired="codeTurnstileToken = ''"
            @error="codeTurnstileToken = ''"
          />
          <el-form-item>
            <el-button type="primary" size="large" class="auth-submit" @click="handleCodeLogin" :loading="loading">
              登录
            </el-button>
          </el-form-item>
        </el-form>

        <!-- OAuth -->
        <div class="auth-oauth">
          <span class="oauth-divider">或</span>
          <div class="oauth-btns">
            <button class="oauth-btn github" @click="handleGithubLogin" :disabled="githubLoading">
              <svg viewBox="0 0 24 24" width="18" height="18"><path fill="currentColor" d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/></svg>
              GitHub
            </button>
            <button class="oauth-btn google" @click="handleGoogleLogin" :disabled="googleLoading">
              <svg viewBox="0 0 24 24" width="18" height="18"><path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/><path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/><path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/><path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/></svg>
              Google
            </button>
          </div>
        </div>

        <p class="auth-footer">
          还没有账号？<router-link to="/register">创建账号</router-link>
        </p>
      </div>
    </div>

    <!-- Right: Diagonal gallery -->
    <div class="auth-gallery-side">
      <div class="gallery-outer">
        <div class="gallery-inner">
          <div class="gallery-track" :style="trackStyle">
            <div class="gallery-grid" v-for="copy in 2" :key="copy">
              <img
                v-for="img in galleryImages"
                :key="`${copy}-${img.id}`"
                :src="img.imageUrl"
                :alt="img.imageName"
                class="gallery-img"
                loading="lazy"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { login, sendCode, loginByCode, getCaptcha, getGithubAuthUrl, getGoogleAuthUrl } from '../api/user'
import { getImageList } from '../api/image'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'
import TurnstileWidget from '../components/TurnstileWidget.vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const loginMode = ref('password')
const sending = ref(false)
const countdown = ref(0)
const githubLoading = ref(false)
const googleLoading = ref(false)
const turnstileRef = ref(null)
const turnstileToken = ref('')
const codeTurnstileRef = ref(null)
const codeTurnstileToken = ref('')
const captchaImage = ref('')
const captchaId = ref('')
const captchaCode = ref('')
const galleryImages = ref([])
let countdownTimer = null

const form = reactive({ username: '', password: '' })
const codeForm = reactive({ email: '', code: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const trackStyle = computed(() => ({
  animationDuration: `${Math.max(60, galleryImages.value.length * 3)}s`
}))

onMounted(async () => {
  const token = new URLSearchParams(window.location.search).get('satoken')
  if (token) {
    userStore.setToken(token)
    window.history.replaceState({}, '', '/login')
    try {
      await userStore.fetchUserInfo()
      ElMessage.success('欢迎回来')
      router.push('/home')
    } catch { ElMessage.error('登录失败，请重试') }
    return
  }
  fetchCaptcha()
  try {
    const res = await getImageList({ page: 1, limit: 30, visibility: 'PUBLIC' })
    galleryImages.value = (res.data?.records || []).filter(img => img.imageUrl)
  } catch {}
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

async function fetchCaptcha() {
  try {
    const res = await getCaptcha()
    captchaImage.value = res.data.captchaImage
    captchaId.value = res.data.captchaId
  } catch {}
}

async function handleSendCode() {
  if (!codeForm.email) { ElMessage.warning('请输入邮箱'); return }
  if (!captchaCode.value) { ElMessage.warning('请输入图形验证码'); return }
  const token = getCodeTurnstileToken()
  if (!token) { ElMessage.warning('请完成人机验证'); return }
  sending.value = true
  try {
    await sendCode({ email: codeForm.email.trim(), captchaId: captchaId.value, captchaCode: captchaCode.value, turnstileToken: token })
    ElMessage.success('验证码已发送')
    countdown.value = 60
    countdownTimer = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(countdownTimer) }, 1000)
  } catch { fetchCaptcha() }
  finally { resetCodeTurnstile(); sending.value = false }
}

async function handleCodeLogin() {
  if (!codeForm.email) { ElMessage.warning('请输入邮箱'); return }
  if (!codeForm.code) { ElMessage.warning('请输入验证码'); return }
  const token = getCodeTurnstileToken()
  if (!token) { ElMessage.warning('请完成人机验证'); return }
  loading.value = true
  try {
    const res = await loginByCode({ email: codeForm.email.trim(), code: codeForm.code.trim(), turnstileToken: token })
    userStore.setToken(res.data)
    await userStore.fetchUserInfo()
    ElMessage.success('欢迎回来')
    router.push('/home')
  } catch {} finally { resetCodeTurnstile(); loading.value = false }
}

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
function getCodeTurnstileToken() { return codeTurnstileRef.value?.getToken?.() || codeTurnstileToken.value }
function resetTurnstile() { turnstileToken.value = ''; turnstileRef.value?.reset?.() }
function resetCodeTurnstile() { codeTurnstileToken.value = ''; codeTurnstileRef.value?.reset?.() }
</script>

<style scoped>
.auth-page {
  display: flex;
  min-height: 100vh;
  min-height: 100dvh;
}

/* ── Left: Form ── */
.auth-form-side {
  width: 42%;
  max-width: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  background: var(--bg-base);
  overflow-y: auto;
}

.auth-form-wrap {
  width: 100%;
  max-width: 380px;
  animation: fadeUp 0.5s var(--ease-out);
}

.auth-logo {
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.auth-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 8px 0 24px;
  letter-spacing: -0.01em;
}

/* ── Tabs ── */
.auth-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid var(--border-subtle);
  margin-bottom: 24px;
}

.auth-tab {
  flex: 1;
  padding: 10px 0;
  border: none;
  background: none;
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
  letter-spacing: 0;
}
.auth-tab::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0; right: 0;
  height: 2px;
  background: var(--text-primary);
  transform: scaleX(0);
  transition: transform 0.2s var(--ease-out);
}
.auth-tab:hover { color: var(--text-primary); }
.auth-tab.active { color: var(--text-primary); font-weight: 600; }
.auth-tab.active::after { transform: scaleX(1); }

/* ── Form ── */
.auth-form {
  margin-top: 0;
}

.auth-form :deep(.el-form-item__label) {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted) !important;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding-bottom: 4px;
}

.auth-submit {
  width: 100%;
  height: 46px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: -0.01em;
  margin-top: 4px;
}

/* ── Captcha ── */
.captcha-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.captcha-row .el-input { flex: 1; }
.captcha-img {
  height: 40px;
  cursor: pointer;
  border: 1px solid var(--border-subtle);
  flex-shrink: 0;
}
.code-btn {
  flex-shrink: 0;
  min-width: 72px;
  height: 40px;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid var(--border-visible);
  background: transparent;
  color: var(--text-primary);
}
.code-btn:hover { background: var(--bg-hover); }

/* ── OAuth ── */
.auth-oauth {
  margin-top: 28px;
  text-align: center;
}

.oauth-divider {
  font-size: 12px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  display: block;
  margin-bottom: 12px;
}

.oauth-btns {
  display: flex;
  gap: 10px;
}

.oauth-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  border: 1px solid var(--border-visible);
  background: transparent;
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}
.oauth-btn:hover { background: var(--bg-hover); border-color: var(--border-strong); }
.oauth-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.oauth-btn.github { color: #24292f; }
.oauth-btn.google { color: #444; }

/* ── Footer ── */
.auth-footer {
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 500;
  margin-top: 24px;
}

.auth-footer a {
  color: var(--text-primary);
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 3px;
}

/* ── Right: Diagonal gallery ── */
.auth-gallery-side {
  width: 58%;
  position: relative;
  overflow: hidden;
  background: var(--bg-surface);
  border-left: 1px solid var(--border-subtle);
}

.gallery-outer {
  position: absolute;
  inset: -10%;
  overflow: hidden;
}

.gallery-inner {
  position: absolute;
  inset: -20%;
  transform: rotate(15deg);
}

.gallery-track {
  animation: scrollDiagonal 80s linear infinite;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 8px;
}

.gallery-img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  filter: grayscale(30%);
  transition: filter 0.4s;
}
.gallery-img:hover { filter: grayscale(0%); }

@keyframes scrollDiagonal {
  0% { transform: translateY(0); }
  100% { transform: translateY(-50%); }
}

/* ── Mobile ── */
@media (max-width: 768px) {
  .auth-page { flex-direction: column; }

  .auth-gallery-side {
    width: 100%;
    height: 160px;
    order: 1;
    border-left: none;
    border-bottom: 1px solid var(--border-subtle);
  }
  .gallery-outer { inset: -30%; }
  .gallery-inner { inset: -40%; transform: rotate(12deg); }
  .gallery-grid { grid-template-columns: repeat(6, 1fr); gap: 4px; }

  .auth-form-side {
    width: 100%;
    max-width: none;
    order: 2;
    padding: 28px 24px;
  }
}
</style>
