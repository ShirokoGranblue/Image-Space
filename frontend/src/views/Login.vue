<template>
  <div class="auth-page">
    <div class="auth-shell">
      <aside class="auth-visual-panel">
        <router-link to="/home" class="auth-brand" aria-label="图像空间首页">
          <span class="brand-mark">图</span>
          <span>图像空间</span>
        </router-link>

        <div class="auth-visual-frame">
          <div class="vis-grid" aria-hidden="true">
            <div
              v-for="(cell, i) in visCells"
              :key="i"
              class="vis-cell"
              :style="{ background: cell.color, opacity: cell.opacity }"
            ></div>
          </div>
          <div class="visual-copy">
            <span class="eyebrow">私人图库</span>
            <h2>整理灵感，保留图像的来路。</h2>
            <p>上传、分类、分享与回看，都在一个清爽的空间里完成。</p>
          </div>
          <div class="visual-rail" aria-hidden="true">
            <span>01</span>
            <span>收集</span>
            <span>整理</span>
          </div>
        </div>

        <div class="auth-stats" aria-label="图像空间能力">
          <div>
            <strong>收藏</strong>
            <span>把图片集中保存</span>
          </div>
          <div>
            <strong>整理</strong>
            <span>按分类和标签归档</span>
          </div>
          <div>
            <strong>分享</strong>
            <span>控制每张图可见范围</span>
          </div>
        </div>
      </aside>

      <main class="auth-card">
        <div class="auth-card-head">
          <span class="auth-kicker">{{ authMode === 'login' ? '账户登录' : '创建账户' }}</span>
          <h1>{{ authMode === 'login' ? '欢迎回来' : '创建账号' }}</h1>
          <p>{{ authMode === 'login' ? '继续整理图片、分类和分享范围。' : '从第一张收藏开始，建立自己的图片空间。' }}</p>
        </div>

        <div class="auth-mode-switch" aria-label="账户操作">
          <button type="button" :class="{ active: authMode === 'login' }" @click="setAuthMode('login')">
            登录
          </button>
          <button type="button" :class="{ active: authMode === 'register' }" @click="setAuthMode('register')">
            注册
          </button>
        </div>

        <template v-if="authMode === 'login'">
          <div class="tab-row" role="tablist" aria-label="登录方式">
            <button
              type="button"
              class="login-tab"
              :class="{ act: loginMode === 'password' }"
              @click="setLoginMode('password')"
            >
              <el-icon><Lock /></el-icon>
              密码登录
            </button>
            <button
              type="button"
              class="login-tab"
              :class="{ act: loginMode === 'email' }"
              @click="setLoginMode('email')"
            >
              <el-icon><Message /></el-icon>
              邮箱登录
            </button>
          </div>

          <el-form
            v-if="loginMode === 'password'"
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            class="auth-form"
            @submit.prevent="handleLogin"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="输入用户名" size="large" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="输入密码"
                size="large"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <TurnstileWidget
              ref="turnstileRef"
              @verified="turnstileToken = $event"
              @expired="turnstileToken = ''"
              @error="turnstileToken = ''"
            />
            <el-form-item class="submit-row">
              <el-button type="primary" size="large" class="login-btn" @click="handleLogin" :loading="loading">
                <span>登录</span>
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </el-form-item>
          </el-form>

          <el-form
            v-else
            ref="emailFormRef"
            :model="emailForm"
            :rules="emailRules"
            label-position="top"
            class="auth-form"
            @submit.prevent="handleEmailLogin"
          >
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="emailForm.email" placeholder="name@example.com" size="large" />
            </el-form-item>
            <el-form-item label="图形验证码" prop="captchaCode">
              <div class="captcha-row-inline">
                <el-input
                  v-model="emailForm.captchaCode"
                  placeholder="输入图形验证码"
                  size="large"
                  @keyup.enter="handleSendCode"
                />
                <button class="captcha-image" type="button" @click="loadCaptcha" :disabled="captchaLoading">
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
                  size="large"
                  @keyup.enter="handleEmailLogin"
                />
                <el-button class="send-code-btn" @click="handleSendCode" :loading="sendCodeLoading" :disabled="countdown > 0">
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
              <el-button type="primary" size="large" class="login-btn" @click="handleEmailLogin" :loading="codeLoginLoading">
                <span>邮箱登录</span>
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </el-form-item>
          </el-form>

          <div class="or-row"><span>或使用第三方账户</span></div>
          <div class="oauth-row">
            <button class="oauth-btn" type="button" @click="handleGithubLogin" :disabled="githubLoading">
              <svg class="oauth-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path fill="currentColor" d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
              </svg>
              <span>GitHub</span>
            </button>
            <button class="oauth-btn" type="button" @click="handleGoogleLogin" :disabled="googleLoading">
              <svg class="oauth-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path fill="#EA4335" d="M12.24 10.285V14.4h6.887c-.648 2.41-2.519 4.114-5.136 4.114A5.56 5.56 0 0 1 8.35 13c0-3.076 2.488-5.571 5.557-5.571 1.48 0 2.81.579 3.8 1.527l3.056-3.056C18.847 2.057 16.518 1 13.907 1 7.855 1 2.923 5.932 2.923 12s4.932 11 10.984 11c6.305 0 10.485-4.429 10.485-10.667 0-.742-.067-1.428-.19-2.048H12.24Z" />
              </svg>
              <span>Google</span>
            </button>
            <button class="oauth-btn" type="button" @click="handleMicrosoftLogin" :disabled="microsoftLoading">
              <svg class="oauth-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path fill="#F25022" d="M11.4 2H2v9.4h9.4V2z" />
                <path fill="#7FBA00" d="M22 2h-9.4v9.4H22V2z" />
                <path fill="#00A4EF" d="M11.4 12.6H2V22h9.4v-9.4z" />
                <path fill="#FFB900" d="M22 12.6h-9.4V22H22v-9.4z" />
              </svg>
              <span>Microsoft</span>
            </button>
          </div>
        </template>

        <el-form
          v-else
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          label-position="top"
          class="auth-form register-form"
          @submit.prevent="handleRegister"
        >
          <div class="form-columns">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="你的唯一用户名" size="large" />
            </el-form-item>
            <el-form-item label="手机号（选填）">
              <el-input v-model="registerForm.phone" placeholder="选填" maxlength="20" size="large" />
            </el-form-item>
          </div>
          <div class="form-columns">
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="至少 6 位密码" size="large" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="再次输入密码"
                size="large"
                show-password
                @keyup.enter="handleRegister"
              />
            </el-form-item>
          </div>
          <el-form-item label="邮箱" required class="email-verification-item">
            <div class="email-verification-controls">
              <el-form-item prop="email" class="inline-form-item">
                <el-input v-model="registerForm.email" placeholder="name@example.com" size="large" />
              </el-form-item>
              <el-form-item prop="code" class="inline-form-item email-code-inline">
                <div class="code-row">
                  <el-input
                    v-model="registerForm.code"
                    placeholder="输入邮箱验证码"
                    size="large"
                    @keyup.enter="handleRegister"
                  />
                  <el-button
                    class="send-code-btn"
                    @click="handleRegisterSendCode"
                    :loading="registerSendCodeLoading"
                    :disabled="registerCountdown > 0"
                  >
                    {{ registerCountdown > 0 ? registerCountdown + 's' : '发送验证码' }}
                  </el-button>
                </div>
              </el-form-item>
            </div>
          </el-form-item>
          <el-form-item class="centered-captcha-item" label="图形验证码" prop="captchaCode">
            <div class="captcha-row-inline">
              <el-input
                v-model="registerForm.captchaCode"
                placeholder="输入图形验证码"
                size="large"
                @keyup.enter="handleRegisterSendCode"
              />
              <button class="captcha-image" type="button" @click="loadRegisterCaptcha" :disabled="registerCaptchaLoading">
                <img v-if="registerCaptchaImage" :src="registerCaptchaImage" alt="图形验证码" />
                <span v-else>{{ registerCaptchaLoading ? '加载中' : '刷新' }}</span>
              </button>
            </div>
          </el-form-item>
          <TurnstileWidget
            class="register-turnstile"
            ref="turnstileRef"
            @verified="turnstileToken = $event"
            @expired="turnstileToken = ''"
            @error="turnstileToken = ''"
          />
          <el-form-item class="submit-row">
            <el-button type="primary" size="large" class="login-btn" @click="handleRegister" :loading="registerLoading">
              <span>创建账号</span>
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <template v-if="authMode === 'login'">
            还没有账号？<button type="button" @click="setAuthMode('register')">创建账号</button>
          </template>
          <template v-else>
            已有账号？<button type="button" @click="setAuthMode('login')">返回登录</button>
          </template>
        </div>
      </main>
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
import { fireBigSideCannons } from '../utils/confettiEffect'

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
const registerSendCodeLoading = ref(false)
const registerCaptchaLoading = ref(false)
const githubLoading = ref(false)
const googleLoading = ref(false)
const microsoftLoading = ref(false)
const turnstileRef = ref(null)
const turnstileToken = ref('')
const captchaImage = ref('')
const registerCaptchaImage = ref('')
const countdown = ref(0)
const registerCountdown = ref(0)
let countdownTimer = null
let registerCountdownTimer = null

const BLUE_TONES = ['#151922', '#202633', '#2a3140', '#b7ff3c', '#38d5ff', '#ff6b57']
const visCells = ref([])
let visInterval = null

const form = reactive({ username: '', password: '' })
const emailForm = reactive({ email: '', code: '', captchaId: '', captchaCode: '' })
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  code: '',
  captchaId: '',
  captchaCode: ''
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
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  captchaCode: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }]
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
  if (registerCountdownTimer) window.clearInterval(registerCountdownTimer)
  if (visInterval) window.clearInterval(visInterval)
})

function setAuthMode(mode) {
  authMode.value = mode
  resetTurnstile()
  if (mode === 'register' && !registerCaptchaImage.value) loadRegisterCaptcha()
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
    const { confirmPassword, captchaId, captchaCode, ...payload } = registerForm
    await register({ ...payload, turnstileToken: token })
    ElMessage.success('注册成功，请登录')
    fireBigSideCannons()
    setAuthMode('login')
    loginMode.value = 'password'
    form.username = registerForm.username
  } catch {} finally {
    resetTurnstile()
    registerLoading.value = false
  }
}

async function loadRegisterCaptcha() {
  registerCaptchaLoading.value = true
  try {
    const res = await getCaptcha()
    registerForm.captchaId = res.data?.captchaId || ''
    registerCaptchaImage.value = res.data?.captchaImage || ''
  } catch {} finally {
    registerCaptchaLoading.value = false
  }
}

async function validateRegisterFields(fields) {
  for (const field of fields) {
    const ok = await registerFormRef.value.validateField(field).then(() => true).catch(() => false)
    if (!ok) return false
  }
  return true
}

async function handleRegisterSendCode() {
  const valid = await validateRegisterFields(['username', 'password', 'confirmPassword', 'email', 'captchaCode'])
  if (!valid) return
  const token = getTurnstileToken()
  if (!token) { ElMessage.warning('请完成人机验证'); return }

  registerSendCodeLoading.value = true
  try {
    await sendCode({
      email: registerForm.email,
      captchaId: registerForm.captchaId,
      captchaCode: registerForm.captchaCode,
      purpose: 'register',
      turnstileToken: token
    })
    ElMessage.success('邮箱验证码已发送')
    registerForm.captchaCode = ''
    startRegisterCountdown()
    resetTurnstile()
    await loadRegisterCaptcha()
  } catch {
    registerForm.captchaCode = ''
    await loadRegisterCaptcha()
  } finally {
    registerSendCodeLoading.value = false
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

function startRegisterCountdown() {
  registerCountdown.value = 60
  if (registerCountdownTimer) window.clearInterval(registerCountdownTimer)
  registerCountdownTimer = window.setInterval(() => {
    registerCountdown.value -= 1
    if (registerCountdown.value <= 0) {
      window.clearInterval(registerCountdownTimer)
      registerCountdownTimer = null
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
    if (!isSafeOAuthUrl(url)) { ElMessage.error('第三方登录地址无效'); return }
    window.location.href = url
  } catch {} finally { githubLoading.value = false }
}

async function handleGoogleLogin() {
  googleLoading.value = true
  try {
    const res = await getGoogleAuthUrl()
    const url = res.data.authorizeUrl
    if (!isSafeOAuthUrl(url)) { ElMessage.error('第三方登录地址无效'); return }
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
  display: grid;
  place-items: center;
  padding: 32px;
  color: var(--ad-text);
  background:
    linear-gradient(135deg, rgba(183, 255, 60, 0.08), transparent 32%),
    linear-gradient(45deg, rgba(56, 213, 255, 0.07), transparent 38%),
    var(--ad-bg);
}

.auth-shell {
  width: min(1180px, 100%);
  min-height: min(760px, calc(100vh - 64px));
  display: grid;
  grid-template-columns: minmax(360px, 0.92fr) minmax(420px, 1fr);
  border: 1px solid var(--ad-line);
  background: rgba(13, 16, 22, 0.78);
  box-shadow: var(--ad-shadow);
}

.auth-visual-panel,
.auth-card {
  min-width: 0;
}

.auth-visual-panel {
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 24px;
  padding: 28px;
  border-right: 1px solid var(--ad-line);
  background:
    linear-gradient(180deg, rgba(255,255,255,0.035), transparent),
    rgba(17, 23, 34, 0.92);
}

.auth-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: max-content;
  color: var(--ad-text);
  font-size: 12px;
  letter-spacing: 0;
}

.brand-mark {
  width: 34px;
  height: 34px;
  display: inline-grid;
  place-items: center;
  border: 1px solid var(--ad-line-strong);
  color: var(--ad-green);
  background: rgba(244, 241, 232, 0.04);
}

.auth-visual-frame {
  position: relative;
  display: grid;
  align-content: end;
  min-height: 470px;
  padding: 28px;
  overflow: hidden;
  border: 1px solid var(--ad-line);
  background:
    linear-gradient(90deg, rgba(244,241,232,0.04) 1px, transparent 1px),
    linear-gradient(rgba(244,241,232,0.04) 1px, transparent 1px),
    #0f141d;
  background-size: 42px 42px;
}

.vis-grid {
  position: absolute;
  inset: 18px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(3, 1fr);
  gap: 10px;
  opacity: 0.86;
}

.vis-cell {
  min-height: 82px;
  border: 1px solid rgba(244, 241, 232, 0.16);
  filter: saturate(0.9);
  transition: opacity 0.42s var(--ad-ease), transform 0.42s var(--ad-ease);
}

.vis-cell:nth-child(2n) {
  transform: translateY(18px);
}

.visual-copy {
  position: relative;
  z-index: 2;
  max-width: 360px;
}

.eyebrow,
.auth-kicker,
.visual-rail,
.auth-stats span {
  color: var(--ad-muted);
  font-size: 11px;
  letter-spacing: 0;
}

.visual-copy h2 {
  margin: 14px 0 14px;
  color: var(--ad-text);
  font-size: clamp(34px, 4.2vw, 58px);
  line-height: 0.98;
  font-weight: 340;
}

.visual-copy p {
  color: var(--ad-text-soft);
  font-size: 16px;
  line-height: 1.8;
}

.visual-rail {
  position: absolute;
  top: 24px;
  right: 24px;
  display: grid;
  gap: 10px;
  justify-items: end;
}

.auth-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border: 1px solid var(--ad-line);
}

.auth-stats div {
  padding: 16px;
}

.auth-stats div + div {
  border-left: 1px solid var(--ad-line);
}

.auth-stats strong {
  display: block;
  color: var(--ad-green);
  font-size: 18px;
  font-weight: 520;
}

.auth-stats span {
  display: block;
  margin-top: 8px;
  line-height: 1.4;
}

.auth-card {
  align-self: stretch;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: clamp(34px, 5vw, 64px);
  background:
    linear-gradient(180deg, rgba(244, 241, 232, 0.035), transparent 36%),
    rgba(13, 16, 22, 0.94);
}

.auth-card-head {
  margin-bottom: 24px;
}

.auth-card-head h1 {
  margin: 10px 0 12px;
  color: var(--ad-text);
  font-size: clamp(46px, 5.8vw, 78px);
  line-height: 0.95;
  font-weight: 340;
}

.auth-card-head p {
  max-width: 520px;
  color: var(--ad-text-soft);
  font-size: 16px;
  line-height: 1.8;
}

.auth-mode-switch,
.tab-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  width: 100%;
  padding: 4px;
  gap: 4px;
  border: 1px solid var(--ad-line);
  background: rgba(244, 241, 232, 0.04);
}

.auth-mode-switch {
  margin-bottom: 18px;
}

.auth-mode-switch button,
.login-tab {
  border: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 36px;
  padding: 0 14px;
  color: var(--ad-muted);
  background: transparent;
  font-family: var(--ad-font);
  font-size: 13px;
  cursor: pointer;
}

.auth-mode-switch button.active,
.login-tab.act {
  color: #071014;
  background: var(--ad-green);
}

.tab-row {
  margin-bottom: 18px;
}

.auth-form {
  display: grid;
  gap: 2px;
}

.form-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.auth-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  color: var(--ad-text-soft) !important;
  font-weight: 420;
}

.email-verification-item {
  width: 100%;
}

.email-verification-item :deep(.el-form-item__content) {
  display: block;
}

.email-verification-controls {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.92fr);
  gap: 10px;
  align-items: start;
  width: 100%;
}

.email-verification-controls :deep(.el-form-item) {
  margin-bottom: 0;
}

.inline-form-item {
  min-width: 0;
}

.captcha-row-inline,
.code-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  width: 100%;
}

.captcha-image,
.oauth-btn,
.login-footer button {
  border: 1px solid var(--ad-line);
  background: rgba(244, 241, 232, 0.05);
  color: var(--ad-text);
  font-family: var(--ad-font);
  cursor: pointer;
}

.captcha-image {
  width: 132px;
  min-height: 40px;
  overflow: hidden;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.send-code-btn {
  min-width: 116px;
}

.centered-captcha-item {
  width: min(100%, 420px);
  justify-self: center;
}

.register-turnstile {
  margin-bottom: 16px;
}

.submit-row {
  margin-top: 4px;
}

.login-btn {
  width: 100%;
  min-height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.or-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 10px 0 14px;
  color: var(--ad-muted);
  font-size: 12px;
}

.or-row::before,
.or-row::after {
  content: '';
  height: 1px;
  flex: 1;
  background: var(--ad-line);
}

.oauth-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.oauth-btn {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: background .16s ease, border-color .16s ease, transform .16s var(--ad-ease);
}

.oauth-icon {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
}

.oauth-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  border-color: var(--ad-line-strong);
  background: rgba(244, 241, 232, 0.08);
}

.oauth-btn:disabled,
.captcha-image:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.login-footer {
  margin-top: 22px;
  color: var(--ad-muted);
  font-size: 14px;
}

.login-footer button,
.login-footer a {
  margin-left: 6px;
  padding: 0;
  border: 0;
  color: var(--ad-green);
  background: transparent;
}

@media (max-width: 980px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-visual-panel {
    display: none;
  }

  .auth-card {
    min-height: calc(100vh - 64px);
  }
}

@media (max-width: 640px) {
  .auth-page {
    padding: 16px;
  }

  .auth-card {
    padding: 26px 18px;
  }

  .auth-mode-switch,
  .tab-row,
  .oauth-row,
  .form-columns,
  .email-verification-controls,
  .captcha-row-inline,
  .code-row {
    width: 100%;
    grid-template-columns: 1fr;
  }

  .auth-mode-switch,
  .tab-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .oauth-row {
    grid-template-columns: 1fr;
  }

  .captcha-image,
  .send-code-btn {
    width: 100%;
  }
}
</style>
