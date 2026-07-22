<template>
  <AuthLayout form-width="narrow">
    <template #aside>
      <div class="auth-aside-copy">
        <span class="auth-aside-eyebrow">继续整理、分享</span>
        <h2 class="auth-aside-title">留住喜欢的画面，也留住再次回看的理由</h2>
        <p class="auth-aside-description">日常发现和长期收藏，都可以放在同一个空间里。</p>
      </div>

      <ul class="auth-capabilities" data-auth-capabilities aria-label="AstralSpace 图片管理能力">
        <li>保留值得回看的图片</li>
        <li>整理分类与标签</li>
        <li>分享愿意公开的内容</li>
      </ul>
    </template>

    <template #header>
      <span class="auth-kicker">登录 AstralSpace</span>
      <h1 class="auth-title">回到已经留下的内容</h1>
      <p class="auth-description">登录后继续浏览和整理收藏。</p>
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

    <div class="login-form-stage">
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
        <el-form-item label="图形验证码" prop="captchaCode">
          <div class="captcha-row-inline">
            <el-input
              v-model="form.captchaCode"
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
    </div>

    <div class="auth-supplemental">
      <div class="or-row"><span>或使用第三方账号登录</span></div>
      <div class="oauth-row" aria-label="第三方登录">
        <button class="oauth-btn" type="button" :disabled="googleLoading" @click="handleGoogleLogin">
          <img class="oauth-icon" :src="googleLogo" alt="" aria-hidden="true" />
          <span>使用 Google 继续</span>
        </button>
        <button class="oauth-btn" type="button" :disabled="githubLoading" @click="handleGithubLogin">
          <img class="oauth-icon" :src="githubLogo" alt="" aria-hidden="true" />
          <span>使用 GitHub 继续</span>
        </button>
        <button class="oauth-btn" type="button" :disabled="microsoftLoading" @click="handleMicrosoftLogin">
          <img class="oauth-icon" :src="microsoftLogo" alt="" aria-hidden="true" />
          <span>使用 Microsoft 继续</span>
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
import googleLogo from '../assets/brands/google.svg'
import githubLogo from '../assets/brands/github.svg'
import microsoftLogo from '../assets/brands/microsoft.svg'

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

const form = reactive({ username: '', password: '', captchaId: '', captchaCode: '' })
const emailForm = reactive({ email: '', code: '', captchaId: '', captchaCode: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }],
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
  }

  if (oauthCode) {
    try {
      const res = await api.post('/user/oauth/exchange', { code: oauthCode })
      userStore.setToken(res.data.satoken)
      await userStore.fetchUserInfo()
      ElMessage.success('登录成功')
      router.push('/home')
      return
    } catch {
      ElMessage.error('登录失败，请重试')
    }
  }

  await loadCaptcha()
})

onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
})

async function setLoginMode(mode) {
  loginMode.value = mode
  resetTurnstile()
  if (!captchaImage.value) await loadCaptcha()
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
    ElMessage.success('登录成功')
    router.push('/home')
  } catch {
    form.captchaCode = ''
    await loadCaptcha()
  } finally {
    resetTurnstile()
    loading.value = false
  }
}

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    const captchaId = res.data?.captchaId || ''
    form.captchaId = captchaId
    emailForm.captchaId = captchaId
    form.captchaCode = ''
    emailForm.captchaCode = ''
    captchaImage.value = res.data?.captchaImage || ''
  } catch {
    // The axios interceptor already reports the API error.
  } finally {
    captchaLoading.value = false
  }
}

async function handleSendCode() {
  const emailOk = await emailFormRef.value.validateField('email').then(() => true).catch(() => false)
  if (!emailOk) return
  const token = getTurnstileToken()
  if (!token) {
    ElMessage.warning('请完成人机验证')
    return
  }
  sendCodeLoading.value = true
  try {
    await sendCode({
      email: emailForm.email,
      purpose: 'login',
      turnstileToken: token,
    })
    ElMessage.success('发送请求已受理；验证码 5 分钟内有效，送达可能需要一点时间')
    startCountdown()
  } catch {
    // The axios interceptor already reports the API error.
  } finally {
    resetTurnstile()
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
    const res = await loginByCode({ ...emailForm, turnstileToken: token })
    userStore.setToken(res.data)
    await userStore.fetchUserInfo()
    ElMessage.success('登录成功')
    router.push('/home')
  } catch {
    emailForm.captchaCode = ''
    await loadCaptcha()
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

.login-form-stage {
  min-height: 0;
}

.auth-supplemental {
  padding-top: 24px;
}

.or-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 14px;
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
  grid-template-columns: 1fr;
  gap: 12px;
}

.oauth-btn {
  width: 100%;
  min-width: 0;
  min-height: 62px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 0 24px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm);
  background: var(--color-surface-1);
  color: var(--color-text-primary);
  box-shadow: var(--shadow-float);
  cursor: pointer;
  font-family: var(--font-ui);
  font-size: 18px;
  font-weight: 500;
  line-height: 1;
  transition: background var(--duration-fast) var(--ease-standard), border-color var(--duration-fast) var(--ease-standard), box-shadow var(--duration-fast) var(--ease-standard);
}

.oauth-btn:hover:not(:disabled),
.captcha-image:hover:not(:disabled) {
  border-color: var(--color-border-strong);
  background: var(--color-surface-1);
}

.oauth-btn:focus-visible {
  outline: none;
  border-color: var(--color-urban);
  box-shadow: var(--shadow-focus);
}

.oauth-btn:disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

.oauth-icon {
  width: 24px;
  height: 24px;
  flex: 0 0 24px;
  object-fit: contain;
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
