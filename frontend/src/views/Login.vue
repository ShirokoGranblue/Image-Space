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
          <div class="vis-quote">记录光影，<br>分享瞬间。</div>
          <div class="vis-sub">图片社区 · 私人图床 · 创作者空间</div>
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
  padding: 34px 24px;
  background: var(--paper);
}

.login-wrap {
  display: grid;
  grid-template-columns: 360px 1fr;
  width: min(100%, 1000px);
  min-height: 580px;
  background: var(--paper);
  border: 0.5px solid var(--paper3);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 18px 45px rgba(4, 44, 83, 0.08);
}

.login-panel {
  background: var(--paper);
  padding: 48px 40px;
  border-right: 0.5px solid var(--paper3);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-logo {
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--ink);
  letter-spacing: .1em;
  margin-bottom: 32px;
  text-decoration: none;
  font-weight: 500;
  display: inline-block;
}

.login-logo:hover {
  opacity: 0.8;
}

.login-heading {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 400;
  color: var(--ink);
  line-height: 1.2;
  margin-bottom: 28px;
}

.tab-row {
  display: flex;
  border-bottom: 1.5px solid var(--paper3);
  margin-bottom: 24px;
}

.login-tab {
  font-size: 12px;
  padding: 8px 0;
  margin-right: 24px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-family: var(--font-body);
  color: var(--ink3);
  border-bottom: 2px solid transparent;
  margin-bottom: -1.5px;
  transition: border-color .15s, color .15s;
}

.login-tab.act {
  color: var(--ink);
  border-bottom-color: var(--ink);
}

.login-btn {
  width: 100%;
  height: 42px;
  background: var(--ink);
  color: var(--paper);
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  font-family: var(--font-body);
  transition: background .15s, transform .1s;
  letter-spacing: .03em;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-btn:hover {
  background: var(--ink2);
}

.login-btn:active {
  transform: scale(.98);
}

.or-row {
  text-align: center;
  margin: 16px 0;
  font-size: 11px;
  color: var(--ink3);
}

.oauth-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.oauth-btn {
  height: 36px;
  border: 0.5px solid var(--paper3);
  border-radius: 8px;
  background: var(--paper2);
  font-size: 11px;
  color: var(--ink);
  cursor: pointer;
  font-family: var(--font-body);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: background .15s, border-color .15s;
}

.oauth-btn:hover {
  background: #fff;
  border-color: var(--ink5);
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 11px;
  color: var(--ink3);
}

.login-footer a {
  color: var(--ink);
  cursor: pointer;
  text-decoration: underline;
  font-weight: 500;
}

.login-visual {
  background: var(--ink);
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 32px;
}

.vis-grid {
  position: absolute;
  inset: 0;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(4, 1fr);
  gap: 3px;
  padding: 3px;
}

.vis-cell {
  border-radius: 6px;
  transition: opacity .6s;
}

.vis-quote {
  position: relative;
  z-index: 2;
  font-family: var(--font-display);
  font-size: 18px;
  color: rgba(255, 255, 255, .85);
  line-height: 1.5;
}

.vis-sub {
  font-size: 11px;
  color: var(--ink5);
  margin-top: 8px;
}

/* Form inputs & element-plus override styles */
.auth-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.auth-form :deep(.el-form-item__label) {
  font-size: 11px;
  color: var(--ink2) !important;
  letter-spacing: .05em;
  margin-bottom: 6px;
  padding-bottom: 0;
}

.auth-form :deep(.el-input__wrapper) {
  background: var(--paper2) !important;
  border: 0.5px solid var(--paper3) !important;
  border-radius: 8px !important;
  box-shadow: none !important;
  padding: 0 12px;
  height: 40px;
  transition: border-color .15s, background .15s;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--ink4) !important;
  background: #fff !important;
}

.auth-form :deep(.el-input__inner) {
  font-family: var(--font-body);
  font-size: 13px;
  color: var(--ink) !important;
}

.captcha-row-inline,
.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 110px;
  gap: 8px;
  align-items: center;
}

.captcha-image {
  height: 40px;
  border: 0.5px solid var(--paper3);
  border-radius: 8px;
  background: var(--paper2);
  color: var(--ink);
  overflow: hidden;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  padding: 0;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.send-code-btn {
  height: 40px;
  border-radius: 8px;
  background: var(--paper2) !important;
  border: 0.5px solid var(--paper3) !important;
  color: var(--ink) !important;
  font-family: var(--font-body);
  font-size: 11px;
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

@media (max-width: 768px) {
  .login-wrap {
    grid-template-columns: 1fr;
    min-height: auto;
    width: 100%;
  }
  .login-panel {
    border-right: none;
    border-bottom: 0.5px solid var(--paper3);
    padding: 32px 24px;
  }
  .login-visual {
    height: 180px;
    padding: 24px;
  }
  .register-form {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
