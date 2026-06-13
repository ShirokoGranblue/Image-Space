<template>
  <div class="auth-page">
    <div class="login-wrap">
      <!-- Left side panel -->
      <div class="login-panel">
        <router-link to="/home" class="login-logo">IMAGESPACE</router-link>
        <div class="login-heading" v-html="authMode === 'login' ? '欢迎<br>回来' : '创建<br>账号'"></div>

        <template v-if="authMode === 'login'">
          <div class="tab-row" role="tablist" aria-label="登录方式">
            <button
              type="button"
              class="login-tab"
              :class="{ act: loginMode === 'password' }"
              @click="setLoginMode('password')"
            >
              密码登录
            </button>
            <button
              type="button"
              class="login-tab"
              :class="{ act: loginMode === 'email' }"
              @click="setLoginMode('email')"
            >
              邮箱登录
            </button>
          </div>

          <el-form
            v-if="loginMode === 'password'"
            :model="form"
            :rules="rules"
            ref="formRef"
            label-position="top"
            class="auth-form"
            @submit.prevent="handleLogin"
          >
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
              <el-button type="primary" size="large" class="login-btn" @click="handleLogin" :loading="loading">
                登录
              </el-button>
            </el-form-item>
          </el-form>

          <el-form
            v-else
            :model="emailForm"
            :rules="emailRules"
            ref="emailFormRef"
            label-position="top"
            class="auth-form"
            @submit.prevent="handleEmailLogin"
          >
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="emailForm.email" placeholder="输入邮箱" size="large" />
            </el-form-item>
            <el-form-item label="图形验证码" prop="captchaCode">
              <div class="captcha-row-inline">
                <el-input v-model="emailForm.captchaCode" placeholder="输入图形验证码" size="large" @keyup.enter="handleSendCode" />
                <button class="captcha-image" type="button" @click="loadCaptcha" :disabled="captchaLoading">
                  <img v-if="captchaImage" :src="captchaImage" alt="图形验证码" />
                  <span v-else>{{ captchaLoading ? '加载中' : '刷新' }}</span>
                </button>
              </div>
            </el-form-item>
            <el-form-item label="邮箱验证码" prop="code">
              <div class="code-row">
                <el-input v-model="emailForm.code" placeholder="输入邮箱验证码" size="large" @keyup.enter="handleEmailLogin" />
                <el-button class="send-code-btn" @click="handleSendCode" :loading="sendCodeLoading" :disabled="countdown > 0">
                  {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <TurnstileWidget
              ref="turnstileRef"
              @verified="turnstileToken = $event"
              @expired="turnstileToken = ''"
              @error="turnstileToken = ''"
            />
            <el-form-item>
              <el-button type="primary" size="large" class="login-btn" @click="handleEmailLogin" :loading="codeLoginLoading">
                邮箱登录
              </el-button>
            </el-form-item>
          </el-form>

          <div class="or-row">或</div>
          <div class="oauth-row">
            <button class="oauth-btn" @click="handleGithubLogin" :disabled="githubLoading">
              <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true" style="margin-right: 4px;"><path fill="currentColor" d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/></svg>
              GitHub
            </button>
            <button class="oauth-btn" @click="handleGoogleLogin" :disabled="googleLoading">
              <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true" style="margin-right: 4px;"><path fill="#EA4335" d="M12.24 10.285V14.4h6.887c-.648 2.41-2.519 4.114-5.136 4.114A5.56 5.56 0 0 1 8.35 13c0-3.076 2.488-5.571 5.557-5.571 1.48 0 2.81.579 3.8 1.527l3.056-3.056C18.847 2.057 16.518 1 13.907 1 7.855 1 2.923 5.932 2.923 12s4.932 11 10.984 11c6.305 0 10.485-4.429 10.485-10.667 0-.742-.067-1.428-.19-2.048H12.24Z"/></svg>
              Google
            </button>
            <button class="oauth-btn" @click="handleMicrosoftLogin" :disabled="microsoftLoading">
              <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true" style="margin-right: 4px;"><path fill="#F25022" d="M11.4 2H2v9.4h9.4V2z"/><path fill="#7FBA00" d="M22 2h-9.4v9.4H22V2z"/><path fill="#00A4EF" d="M11.4 12.6H2V22h9.4v-9.4z"/><path fill="#FFB900" d="M22 12.6h-9.4V22H22v-9.4z"/></svg>
              Microsoft
            </button>
          </div>
        </template>

        <el-form
          v-else
          :model="registerForm"
          :rules="registerRules"
          ref="registerFormRef"
          label-position="top"
          class="auth-form register-form"
          @submit.prevent="handleRegister"
        >
          <el-form-item class="full-field" label="用户名" prop="username">
            <el-input v-model="registerForm.username" placeholder="你的唯一用户名" size="large" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="registerForm.password" type="password" placeholder="至少 6 位密码" size="large" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" size="large" show-password @keyup.enter="handleRegister" />
          </el-form-item>
          <el-form-item label="邮箱（选填）">
            <el-input v-model="registerForm.email" placeholder="your@email.com" size="large" />
          </el-form-item>
          <el-form-item label="手机号（选填）">
            <el-input v-model="registerForm.phone" placeholder="选填" maxlength="20" size="large" />
          </el-form-item>
          <TurnstileWidget
            class="register-turnstile full-field"
            ref="turnstileRef"
            @verified="turnstileToken = $event"
            @expired="turnstileToken = ''"
            @error="turnstileToken = ''"
          />
          <el-form-item class="full-field compact-submit">
            <el-button type="primary" size="large" class="login-btn" @click="handleRegister" :loading="registerLoading">
              创建账号
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <template v-if="authMode === 'login'">
            还没有账号？<a @click="setAuthMode('register')">创建账号 ↗</a>
          </template>
          <template v-else>
            已有账号？<a @click="setAuthMode('login')">返回登录 ↗</a>
          </template>
        </div>
      </div>

      <!-- Right side visual -->
      <div class="login-visual">
        <div class="vis-grid">
          <div
            v-for="(cell, i) in visCells"
            :key="i"
            class="vis-cell"
            :style="{ background: cell.color, opacity: cell.opacity }"
          ></div>
        </div>
        <div style="position:relative;z-index:2;">
          <div class="vis-quote">收藏灵感，<br>展开次元空间。</div>
          <div class="vis-sub">角色图集 · 灵感收藏 · 创作者空间</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { login, register, sendCode, loginByCode, getCaptcha, getGithubAuthUrl, getGoogleAuthUrl } from '../api/user'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'
import TurnstileWidget from '../components/TurnstileWidget.vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const emailFormRef = ref(null)
const registerFormRef = ref(null)
const authMode = ref('login')
const loginMode = ref('password')
const loading = ref(false)
const registerLoading = ref(false)
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

const BLUE_TONES = ['#042C53', '#0C447C', '#185FA5', '#378ADD', '#85B7EB']
const visCells = ref([])
let visInterval = null

const form = reactive({ username: '', password: '' })
const emailForm = reactive({ email: '', code: '', captchaId: '', captchaCode: '' })
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: ''
})

const validateConfirmPassword = (_rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const emailRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  captchaCode: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度为 2-50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

function initVisCells() {
  const cells = []
  for (let i = 0; i < 12; i++) {
    cells.push({
      color: BLUE_TONES[Math.floor(Math.random() * BLUE_TONES.length)],
      opacity: Math.random() * 0.5 + 0.3
    })
  }
  visCells.value = cells
}

function startVisAnimation() {
  visInterval = setInterval(() => {
    if (visCells.value.length > 0) {
      const idx = Math.floor(Math.random() * visCells.value.length)
      visCells.value[idx].opacity = (Math.random() * 0.5 + 0.3).toFixed(2)
    }
  }, 600)
}

onMounted(async () => {
  initVisCells()
  startVisAnimation()
  const params = new URLSearchParams(window.location.search)
  const oauthCode = params.get('oauth_code')
  const oauthError = params.get('oauthError')

  // 清理 URL 参数
  if (oauthCode || oauthError) {
    window.history.replaceState({}, '', '/login')
  }

  // 处理 OAuth 错误
  if (oauthError) {
    const errorMessages = {
      'oauth_error': 'Microsoft 登录失败',
      'missing_code': '授权码缺失',
      'missing_state': '安全验证缺失',
      'state_invalid': '安全验证已过期',
      'login_failed': '登录失败，请重试'
    }
    ElMessage.error(errorMessages[oauthError] || '登录失败，请重试')
    return
  }

  // 处理 OAuth 成功（通过一次性 code 换取 token）
  if (oauthCode) {
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

onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
  if (visInterval) window.clearInterval(visInterval)
})

function setAuthMode(mode) {
  authMode.value = mode
  resetTurnstile()
}

async function setLoginMode(mode) {
  loginMode.value = mode
  resetTurnstile()
  if (mode === 'email' && !captchaImage.value) await loadCaptcha()
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

async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return
  const token = getTurnstileToken()
  if (!token) { ElMessage.warning('请完成人机验证'); return }
  registerLoading.value = true
  try {
    const { confirmPassword, ...payload } = registerForm
    await register({ ...payload, turnstileToken: token })
    ElMessage.success('注册成功，请登录')
    setAuthMode('login')
    loginMode.value = 'password'
    form.username = registerForm.username
  } catch {} finally {
    resetTurnstile()
    registerLoading.value = false
  }
}

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    emailForm.captchaId = res.data?.captchaId || ''
    captchaImage.value = res.data?.captchaImage || ''
  } catch {} finally {
    captchaLoading.value = false
  }
}

async function handleSendCode() {
  const emailOk = await emailFormRef.value.validateField('email').then(() => true).catch(() => false)
  const captchaOk = await emailFormRef.value.validateField('captchaCode').then(() => true).catch(() => false)
  if (!emailOk || !captchaOk) return
  const token = getTurnstileToken()
  if (!token) { ElMessage.warning('请完成人机验证'); return }
  sendCodeLoading.value = true
  try {
    await sendCode({
      email: emailForm.email,
      captchaId: emailForm.captchaId,
      captchaCode: emailForm.captchaCode,
      turnstileToken: token
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
  if (!token) { ElMessage.warning('请完成人机验证'); return }
  codeLoginLoading.value = true
  try {
    const res = await loginByCode({ email: emailForm.email, code: emailForm.code, turnstileToken: token })
    userStore.setToken(res.data)
    await userStore.fetchUserInfo()
    ElMessage.success('欢迎回来')
    router.push('/home')
  } catch {} finally {
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

const ALLOWED_OAUTH_DOMAINS = [
  'github.com',
  'accounts.google.com',
  'login.microsoftonline.com',
]

function isSafeOAuthUrl(url) {
  try {
    const parsed = new URL(url)
    return ALLOWED_OAUTH_DOMAINS.some(domain => parsed.hostname === domain)
  } catch {
    return false
  }
}

async function handleGithubLogin() {
  githubLoading.value = true
  try {
    const res = await getGithubAuthUrl()
    const url = res.data.authorizeUrl
    if (!isSafeOAuthUrl(url)) { ElMessage.error('OAuth 地址无效'); return }
    window.location.href = url
  } catch {} finally { githubLoading.value = false }
}

async function handleGoogleLogin() {
  googleLoading.value = true
  try {
    const res = await getGoogleAuthUrl()
    const url = res.data.authorizeUrl
    if (!isSafeOAuthUrl(url)) { ElMessage.error('OAuth 地址无效'); return }
    window.location.href = url
  } catch {} finally { googleLoading.value = false }
}

/**
 * Microsoft OAuth2 登录
 * 直接跳转到后端 /oauth/microsoft/login，后端会重定向到 Microsoft 授权页面
 * 注意：生产环境推荐后端使用 HttpOnly Cookie 传递 token，避免 token 暴露在 URL 中
 */
function handleMicrosoftLogin() {
  microsoftLoading.value = true
  try {
    // 传递当前 origin 作为 baseUrl，用于回调后重定向回来
    const baseUrl = encodeURIComponent(window.location.origin)
    window.location.href = `/api/user/oauth/microsoft/login?baseUrl=${baseUrl}`
  } finally {
    // 页面即将跳转，不需要重置 loading 状态
    setTimeout(() => { microsoftLoading.value = false }, 3000)
  }
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
  padding: 36px 24px;
  background:
    radial-gradient(circle at 18% 18%, rgba(55, 138, 221, 0.24), transparent 34vw),
    radial-gradient(circle at 82% 12%, rgba(239, 159, 39, 0.14), transparent 28vw);
}

.login-wrap {
  display: grid;
  grid-template-columns: minmax(360px, 430px) 1fr;
  width: min(100%, 1080px);
  min-height: 640px;
  background: rgba(255, 253, 248, 0.88);
  border: 1px solid rgba(255, 253, 248, 0.58);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: var(--shadow-cinematic);
  backdrop-filter: blur(22px);
}

.login-panel {
  background: rgba(255, 253, 248, 0.92);
  padding: 56px 48px;
  border-right: 1px solid rgba(4, 44, 83, 0.08);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-logo {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--ink);
  letter-spacing: .22em;
  margin-bottom: 42px;
  text-decoration: none;
  font-weight: 800;
  display: inline-block;
}

.login-logo:hover {
  opacity: 0.78;
}

.login-heading {
  font-family: var(--font-display);
  font-size: clamp(38px, 5vw, 56px);
  font-weight: 500;
  color: var(--ink);
  line-height: 0.96;
  margin-bottom: 32px;
  letter-spacing: -0.02em;
}

.tab-row {
  display: inline-flex;
  align-self: flex-start;
  gap: 4px;
  padding: 4px;
  border: 1px solid rgba(4, 44, 83, 0.1);
  border-radius: 999px;
  background: rgba(4, 44, 83, 0.04);
  margin-bottom: 24px;
}

.login-tab {
  font-size: 13px;
  padding: 8px 14px;
  background: transparent;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  font-family: var(--font-body);
  color: var(--ink3);
  font-weight: 700;
  transition: background .15s, color .15s, transform .15s;
}

.login-tab:hover {
  transform: translateY(-1px);
}

.login-tab.act {
  color: var(--paper);
  background: var(--ink);
}

.login-btn {
  width: 100%;
  height: 46px;
  background: var(--ink);
  color: var(--paper);
  border: none;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  font-family: var(--font-body);
  transition: background .15s, transform .1s, box-shadow .18s;
  letter-spacing: .06em;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 30px rgba(4, 44, 83, 0.24);
}

.login-btn:hover {
  background: var(--ink2);
  box-shadow: 0 16px 36px rgba(4, 44, 83, 0.3);
}

.login-btn:active {
  transform: scale(.98);
}

.or-row {
  text-align: center;
  margin: 18px 0;
  font-size: 12px;
  color: var(--ink3);
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.oauth-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
}

.oauth-btn {
  height: 38px;
  border: 1px solid rgba(4, 44, 83, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.58);
  font-size: 13px;
  color: var(--ink);
  cursor: pointer;
  font-family: var(--font-body);
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  transition: background .15s, border-color .15s, transform .15s;
}

.oauth-btn:hover {
  background: #fff;
  border-color: rgba(55, 138, 221, 0.42);
  transform: translateY(-1px);
}

.login-footer {
  text-align: center;
  margin-top: 22px;
  font-size: 13px;
  color: var(--ink3);
}

.login-footer a {
  color: var(--ink);
  cursor: pointer;
  text-decoration: none;
  font-weight: 800;
}

.login-visual {
  background:
    radial-gradient(circle at 22% 28%, rgba(55, 138, 221, 0.34), transparent 32%),
    linear-gradient(145deg, var(--cinema) 0%, var(--cinema2) 58%, #14110d 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 42px;
}

.login-visual::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, transparent 24%, rgba(255, 253, 248, 0.1) 48%, transparent 70%);
  animation: softGlow 9s var(--ease-in-out) infinite;
}

.vis-grid {
  position: absolute;
  inset: 18px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(4, 1fr);
  gap: 8px;
  opacity: 0.54;
}

.vis-cell {
  border-radius: 14px;
  transition: opacity .6s, transform .6s;
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.08);
}

.vis-quote {
  position: relative;
  z-index: 2;
  font-family: var(--font-display);
  font-size: clamp(26px, 3.2vw, 42px);
  color: rgba(255, 253, 248, .92);
  line-height: 1.16;
  letter-spacing: -0.01em;
}

.vis-sub {
  position: relative;
  z-index: 2;
  font-size: 13px;
  color: rgba(201, 216, 231, 0.72);
  margin-top: 14px;
  letter-spacing: 0.08em;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.auth-form :deep(.el-form-item__label) {
  font-size: 12px;
  color: var(--ink2) !important;
  letter-spacing: .12em;
  margin-bottom: 7px;
  padding-bottom: 0;
  text-transform: uppercase;
}

.auth-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.72) !important;
  border: 1px solid rgba(4, 44, 83, 0.1) !important;
  border-radius: 999px !important;
  box-shadow: none !important;
  padding: 0 14px;
  height: 44px;
  transition: border-color .15s, background .15s, box-shadow .15s;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(186, 117, 23, 0.55) !important;
  background: #fff !important;
  box-shadow: 0 0 0 4px rgba(186, 117, 23, 0.1) !important;
}

.auth-form :deep(.el-input__inner) {
  font-family: var(--font-body);
  font-size: 14px;
  color: var(--ink) !important;
}

.captcha-row-inline,
.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 118px;
  gap: 8px;
  align-items: center;
}

.captcha-image,
.send-code-btn {
  height: 44px;
  border: 1px solid rgba(4, 44, 83, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--ink);
  overflow: hidden;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  padding: 0;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.send-code-btn {
  background: rgba(4, 44, 83, 0.06) !important;
  color: var(--ink) !important;
  font-family: var(--font-body);
  width: 100%;
}

.register-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.register-form :deep(.full-field),
.register-form .register-turnstile {
  grid-column: 1 / -1;
}

@media (max-width: 820px) {
  .auth-page {
    padding: 20px 14px;
  }
  .login-wrap {
    grid-template-columns: 1fr;
    min-height: auto;
    width: 100%;
    border-radius: 20px;
  }
  .login-panel {
    border-right: none;
    border-bottom: 1px solid rgba(4, 44, 83, 0.08);
    padding: 34px 24px;
  }
  .login-visual {
    min-height: 220px;
    padding: 28px;
  }
  .register-form {
    grid-template-columns: 1fr;
    gap: 0;
  }
  .oauth-row {
    grid-template-columns: 1fr;
  }
}

/* Anime paper override */
.auth-page {
  background: #f0eee6;
}

.login-wrap {
  background: #f0eee6;
  border-color: rgba(17, 26, 53, 0.12);
  box-shadow: 0 26px 80px rgba(17, 26, 53, 0.14);
}

.login-panel {
  background: #f0eee6;
}

.login-visual {
  background:
    radial-gradient(circle at 24% 26%, rgba(255, 122, 184, 0.38), transparent 32%),
    radial-gradient(circle at 76% 18%, rgba(88, 184, 255, 0.3), transparent 30%),
    linear-gradient(145deg, #fff4de 0%, #f5ecff 52%, #dff6ff 100%);
}

.login-visual::after {
  background:
    radial-gradient(circle at 76% 22%, rgba(255,255,255,0.82), transparent 12%),
    linear-gradient(120deg, transparent 24%, rgba(255, 122, 184, 0.16) 48%, transparent 70%);
}

.vis-cell {
  box-shadow: inset 0 0 0 1px rgba(17, 26, 53, 0.08);
  animation: cardTwinkle 4.8s var(--ease-in-out) infinite;
}

.vis-cell:nth-child(2n) {
  animation-delay: -1.6s;
}

.vis-cell:nth-child(3n) {
  animation-delay: -2.8s;
}

.vis-quote {
  color: var(--ink);
  text-shadow: 0 1px 0 rgba(255,255,255,0.66);
}

.vis-sub {
  color: rgba(4, 44, 83, 0.58);
}

.auth-form :deep(.el-input__wrapper),
.captcha-image,
.send-code-btn {
  background: #f0eee6 !important;
  border-color: rgba(17, 26, 53, 0.1) !important;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(255, 122, 184, 0.52) !important;
  background: #f0eee6 !important;
  box-shadow: 0 0 0 4px rgba(255, 122, 184, 0.12) !important;
}

.login-btn {
  background: var(--cinema) !important;
  border-color: var(--cinema) !important;
}

.login-btn:hover {
  background: var(--anime-pink) !important;
  border-color: var(--anime-pink) !important;
}

@keyframes cardTwinkle {
  0%, 100% { transform: translateY(0); filter: saturate(1); }
  50% { transform: translateY(-8px); filter: saturate(1.12); }
}
</style>
