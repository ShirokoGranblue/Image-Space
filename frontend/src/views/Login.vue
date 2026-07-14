<template>
  <AuthLayout form-width="narrow">
    <template #aside>
      <div class="auth-aside-copy">
        <span class="auth-aside-eyebrow">私人图库</span>
        <h2 class="auth-aside-title">整理灵感，保留图像的来路。</h2>
        <p class="auth-aside-description">上传、分类、分享与回看，都在一个清爽的空间里完成。</p>
      </div>

      <ul class="auth-capabilities" data-auth-capabilities aria-label="图像空间能力">
        <li>把图片集中保存</li>
        <li>按分类和标签归档</li>
        <li>控制每张图的可见范围</li>
      </ul>
    </template>

    <template #header>
      <span class="auth-kicker">账户登录</span>
      <h1 class="auth-title">欢迎回来</h1>
      <p class="auth-description">继续整理图片、分类和分享范围。</p>
    </template>

    <div class="login-method-switch" role="tablist" aria-label="登录方式">
      <button
        id="password-login-tab"
        type="button"
        role="tab"
        :aria-selected="loginMode === 'password'"
        :tabindex="loginMode === 'password' ? 0 : -1"
        :class="{ 'is-active': loginMode === 'password' }"
        aria-controls="password-login-panel"
        @click="setLoginMode('password')"
      >
        <el-icon><Lock /></el-icon>
        <span>密码登录</span>
      </button>
      <button
        id="email-login-tab"
        type="button"
        role="tab"
        :aria-selected="loginMode === 'email'"
        :tabindex="loginMode === 'email' ? 0 : -1"
        :class="{ 'is-active': loginMode === 'email' }"
        aria-controls="email-login-panel"
        @click="setLoginMode('email')"
      >
        <el-icon><Message /></el-icon>
        <span>邮箱登录</span>
      </button>
    </div>

    <el-form
      v-if="loginMode === 'password'"
      id="password-login-panel"
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="auth-form login-form login-form--password"
      role="tabpanel"
      aria-labelledby="password-login-tab"
      @submit.prevent="handleLogin"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="form.username"
          placeholder="输入用户名"
          autocomplete="username"
          size="large"
        />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="输入密码"
          autocomplete="current-password"
          size="large"
          show-password
        />
      </el-form-item>
      <TurnstileWidget
        ref="turnstileRef"
        @verified="turnstileToken = $event"
        @expired="turnstileToken = ''"
        @error="turnstileToken = ''"
      />
      <el-form-item class="submit-row">
        <el-button type="primary" native-type="submit" size="large" class="login-btn" :loading="loading">
          <span>登录</span>
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </el-form-item>
    </el-form>

    <el-form
      v-else
      id="email-login-panel"
      ref="emailFormRef"
      :model="emailForm"
      :rules="emailRules"
      label-position="top"
      class="auth-form login-form login-form--email"
      role="tabpanel"
      aria-labelledby="email-login-tab"
      @submit.prevent="handleEmailLogin"
    >
      <el-form-item label="邮箱" prop="email">
        <el-input
          v-model="emailForm.email"
          placeholder="name@example.com"
          autocomplete="email"
          size="large"
        />
      </el-form-item>
      <el-form-item label="图形验证码" prop="captchaCode">
        <div class="captcha-row-inline">
          <el-input
            v-model="emailForm.captchaCode"
            placeholder="输入图形验证码"
            autocomplete="off"
            size="large"
          />
          <button
            class="captcha-image"
            type="button"
            aria-label="刷新图形验证码"
            :disabled="captchaLoading"
            @click="loadCaptcha"
          >
            <img v-if="captchaImage" :src="captchaImage" alt="图形验证码" />
            <span v-else>{{ captchaLoading ? '加载中' : '刷新' }}</span>
          </button>
        </div>
      </el-form-item>
      <el-form-item label="邮箱验证码" prop="code">
        <div class="code-row">
          <el-input
            v-model="emailForm.code"
            placeholder="输入邮箱验证码"
            autocomplete="one-time-code"
            size="large"
          />
          <el-button
            class="send-code-btn"
            native-type="button"
            :loading="sendCodeLoading"
            :disabled="countdown > 0"
            @click="handleSendCode"
          >
            {{ countdown > 0 ? countdown + 's' : '发送验证码' }}
          </el-button>
        </div>
      </el-form-item>
      <TurnstileWidget
        ref="turnstileRef"
        @verified="turnstileToken = $event"
        @expired="turnstileToken = ''"
        @error="turnstileToken = ''"
      />
      <el-form-item class="submit-row">
        <el-button type="primary" native-type="submit" size="large" class="login-btn" :loading="codeLoginLoading">
          <span>邮箱登录</span>
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </el-form-item>
    </el-form>

    <div class="auth-supplemental">
      <div class="or-row"><span>或使用第三方账户</span></div>
      <div class="oauth-row" aria-label="第三方登录">
        <button class="oauth-btn" type="button" :disabled="githubLoading" @click="handleGithubLogin">
          <svg class="oauth-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
          </svg>
          <span>GitHub</span>
        </button>
        <button class="oauth-btn" type="button" :disabled="googleLoading" @click="handleGoogleLogin">
          <svg class="oauth-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="#EA4335" d="M12.24 10.285V14.4h6.887c-.648 2.41-2.519 4.114-5.136 4.114A5.56 5.56 0 0 1 8.35 13c0-3.076 2.488-5.571 5.557-5.571 1.48 0 2.81.579 3.8 1.527l3.056-3.056C18.847 2.057 16.518 1 13.907 1 7.855 1 2.923 5.932 2.923 12s4.932 11 10.984 11c6.305 0 10.485-4.429 10.485-10.667 0-.742-.067-1.428-.19-2.048H12.24Z" />
          </svg>
          <span>Google</span>
        </button>
        <button class="oauth-btn" type="button" :disabled="microsoftLoading" @click="handleMicrosoftLogin">
          <svg class="oauth-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="#F25022" d="M11.4 2H2v9.4h9.4V2z" />
            <path fill="#7FBA00" d="M22 2h-9.4v9.4H22V2z" />
            <path fill="#00A4EF" d="M11.4 12.6H2V22h9.4v-9.4z" />
            <path fill="#FFB900" d="M22 12.6h-9.4V22H22v-9.4z" />
          </svg>
          <span>Microsoft</span>
        </button>
      </div>
    </div>

    <template #footer>
      <div class="auth-footer-copy">
        还没有账号？
        <router-link class="auth-footer-link" to="/register">创建账号</router-link>
      </div>
    </template>
  </AuthLayout>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { getCaptcha, getGithubAuthUrl, getGoogleAuthUrl, login, loginByCode, sendCode } from '../api/user'
import { ElMessage } from 'element-plus'
import { ArrowRight, Lock, Message } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'
import AuthLayout from '../components/auth/AuthLayout.vue'
import TurnstileWidget from '../components/TurnstileWidget.vue'
import { isSafeOAuthUrl } from '../utils/oauth'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const emailFormRef = ref(null)
const loginMode = ref('password')
const loading = ref(false)
const codeLoginLoading = ref(false)
const sendCodeLoading = ref(false)
const captchaLoading = ref(false)
const githubLoading = ref(false)
const googleLoading = ref(false)
const microsoftLoading = ref(false)
const turnstileRef = ref(null)
const turnstileToken = ref('')
const captchaImage = ref('')
const countdown = ref(0)
let countdownTimer = null

const form = reactive({ username: '', password: '' })
const emailForm = reactive({ email: '', code: '', captchaId: '', captchaCode: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const emailRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  captchaCode: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
}

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  const oauthCode = params.get('oauth_code')
  const oauthError = params.get('oauthError')

  if (oauthCode || oauthError) {
    window.history.replaceState({}, '', '/login')
  }

  if (oauthError) {
    const errorMessages = {
      oauth_error: 'Microsoft 登录失败',
      missing_code: '授权码缺失',
      missing_state: '安全验证缺失',
      state_invalid: '安全验证已过期',
      login_failed: '登录失败，请重试',
    }
    ElMessage.error(errorMessages[oauthError] || '登录失败，请重试')
    return
  }

  if (oauthCode) {
    try {
      const res = await api.post('/user/oauth/exchange', { code: oauthCode })
      userStore.setToken(res.data.satoken)
      await userStore.fetchUserInfo()
      ElMessage.success('欢迎回来')
      router.push('/home')
    } catch {
      ElMessage.error('登录失败，请重试')
    }
  }
})

onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
})

async function setLoginMode(mode) {
  loginMode.value = mode
  resetTurnstile()
  if (mode === 'email' && !captchaImage.value) await loadCaptcha()
  await nextTick()
  document.querySelector(`.login-form--${mode} input`)?.focus()
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const token = getTurnstileToken()
  if (!token) {
    ElMessage.warning('请完成人机验证')
    return
  }
  loading.value = true
  try {
    const res = await login({ ...form, turnstileToken: token })
    userStore.setToken(res.data)
    await userStore.fetchUserInfo()
    ElMessage.success('欢迎回来')
    router.push('/home')
  } catch {
    // The axios interceptor already reports the API error.
  } finally {
    resetTurnstile()
    loading.value = false
  }
}

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    emailForm.captchaId = res.data?.captchaId || ''
    captchaImage.value = res.data?.captchaImage || ''
  } catch {
    // The axios interceptor already reports the API error.
  } finally {
    captchaLoading.value = false
  }
}

async function handleSendCode() {
  const emailOk = await emailFormRef.value.validateField('email').then(() => true).catch(() => false)
  const captchaOk = await emailFormRef.value.validateField('captchaCode').then(() => true).catch(() => false)
  if (!emailOk || !captchaOk) return
  const token = getTurnstileToken()
  if (!token) {
    ElMessage.warning('请完成人机验证')
    return
  }
  sendCodeLoading.value = true
  try {
    await sendCode({
      email: emailForm.email,
      captchaId: emailForm.captchaId,
      captchaCode: emailForm.captchaCode,
      turnstileToken: token,
    })
    ElMessage.success('验证码已发送')
    startCountdown()
    resetTurnstile()
  } catch {
    await loadCaptcha()
  } finally {
    sendCodeLoading.value = false
  }
}

async function handleEmailLogin() {
  const valid = await emailFormRef.value.validate().catch(() => false)
  if (!valid) return
  const token = getTurnstileToken()
  if (!token) {
    ElMessage.warning('请完成人机验证')
    return
  }
  codeLoginLoading.value = true
  try {
    const res = await loginByCode({ email: emailForm.email, code: emailForm.code, turnstileToken: token })
    userStore.setToken(res.data)
    await userStore.fetchUserInfo()
    ElMessage.success('欢迎回来')
    router.push('/home')
  } catch {
    // The axios interceptor already reports the API error.
  } finally {
    resetTurnstile()
    codeLoginLoading.value = false
  }
}

function startCountdown() {
  countdown.value = 60
  if (countdownTimer) window.clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function handleGithubLogin() {
  githubLoading.value = true
  try {
    const res = await getGithubAuthUrl()
    const url = res.data.authorizeUrl
    if (!isSafeOAuthUrl(url)) {
      ElMessage.error('第三方登录地址无效')
      return
    }
    window.location.href = url
  } catch {
    // The axios interceptor already reports the API error.
  } finally {
    githubLoading.value = false
  }
}

async function handleGoogleLogin() {
  googleLoading.value = true
  try {
    const res = await getGoogleAuthUrl()
    const url = res.data.authorizeUrl
    if (!isSafeOAuthUrl(url)) {
      ElMessage.error('第三方登录地址无效')
      return
    }
    window.location.href = url
  } catch {
    // The axios interceptor already reports the API error.
  } finally {
    googleLoading.value = false
  }
}

function handleMicrosoftLogin() {
  microsoftLoading.value = true
  try {
    const baseUrl = encodeURIComponent(window.location.origin)
    window.location.href = `/api/user/oauth/microsoft/login?baseUrl=${baseUrl}`
  } finally {
    setTimeout(() => { microsoftLoading.value = false }, 3000)
  }
}

function getTurnstileToken() {
  return turnstileRef.value?.getToken?.() || turnstileToken.value
}

function resetTurnstile() {
  turnstileToken.value = ''
  turnstileRef.value?.reset?.()
}
</script>

<style scoped>
.login-method-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  width: 100%;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--color-border-subtle);
}

.login-method-switch button {
  min-width: 0;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 12px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: var(--font-ui);
  font-size: 14px;
}

.login-method-switch button.is-active {
  border-bottom-color: var(--color-vermilion);
  background: var(--color-surface-2);
  color: var(--color-text-primary);
}

.auth-supplemental {
  margin-top: 8px;
}

.or-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0 14px;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.or-row::before,
.or-row::after {
  content: '';
  height: 1px;
  flex: 1;
  background: var(--color-border-subtle);
}

.oauth-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.oauth-btn {
  min-width: 0;
  min-height: var(--control-height-lg);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background var(--duration-fast) var(--ease-standard), border-color var(--duration-fast) var(--ease-standard);
}

.oauth-btn:hover:not(:disabled),
.captcha-image:hover:not(:disabled) {
  border-color: var(--color-border-strong);
  background: var(--color-surface-1);
}

.oauth-icon {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
}

.auth-footer-copy {
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: var(--leading-sm);
}

.auth-footer-link {
  margin-left: 6px;
  color: var(--color-night);
  font-weight: 600;
  text-decoration: underline;
  text-decoration-thickness: 1px;
  text-underline-offset: 3px;
}

@media (max-width: 479px) {
  .captcha-row-inline,
  .code-row,
  .oauth-row {
    grid-template-columns: 1fr;
  }

  .captcha-image,
  .send-code-btn {
    width: 100%;
  }
}
</style>
