<template>
  <div class="auth-page">
    <div class="auth-shell registering">
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
            <span class="eyebrow">开始收藏</span>
            <h2>先建立账户，再认真保存每一张图。</h2>
            <p>验证邮箱后，就可以上传、分类和分享图片。</p>
          </div>
          <div class="visual-rail" aria-hidden="true">
            <span>00</span>
            <span>填写</span>
            <span>验证</span>
          </div>
        </div>
      </aside>

      <main class="auth-card">
        <div class="auth-card-head">
          <span class="auth-kicker">创建账户</span>
          <h1>创建账号</h1>
          <p>注册后可以上传图片、整理分类，并按需要设置可见范围。</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="auth-form register-form"
          @submit.prevent="handleRegister"
        >
          <div class="form-columns">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="你的唯一用户名" size="large" />
            </el-form-item>
            <el-form-item label="手机号（可选）">
              <el-input v-model="form.phone" placeholder="选填" maxlength="20" size="large" />
            </el-form-item>
          </div>
          <div class="form-columns">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="至少 6 位密码" size="large" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
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
                <el-input v-model="form.email" placeholder="name@example.com" size="large" />
              </el-form-item>
              <el-form-item prop="code" class="inline-form-item email-code-inline">
                <div class="code-row">
                  <el-input
                    v-model="form.code"
                    placeholder="输入邮箱验证码"
                    size="large"
                    @keyup.enter="handleRegister"
                  />
                  <el-button class="send-code-btn" @click="handleSendCode" :loading="sendCodeLoading" :disabled="countdown > 0">
                    {{ countdown > 0 ? countdown + 's' : '发送验证码' }}
                  </el-button>
                </div>
              </el-form-item>
            </div>
          </el-form-item>
          <el-form-item class="centered-captcha-item" label="图形验证码" prop="captchaCode">
            <div class="captcha-row-inline">
              <el-input
                v-model="form.captchaCode"
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
          <TurnstileWidget
            class="register-turnstile"
            ref="turnstileRef"
            @verified="turnstileToken = $event"
            @expired="turnstileToken = ''"
            @error="turnstileToken = ''"
          />
          <el-form-item class="submit-row">
            <el-button type="primary" size="large" class="login-btn" @click="handleRegister" :loading="loading">
              <span>创建账号</span>
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          已有账号？<router-link to="/login">返回登录</router-link>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCaptcha, register, sendCode } from '../api/user'
import { ElMessage } from 'element-plus'
import TurnstileWidget from '../components/TurnstileWidget.vue'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const sendCodeLoading = ref(false)
const captchaLoading = ref(false)
const turnstileRef = ref(null)
const turnstileToken = ref('')
const captchaImage = ref('')
const countdown = ref(0)
let countdownTimer = null

const BLUE_TONES = ['#151922', '#202633', '#2a3140', '#b7ff3c', '#38d5ff', '#ff6b57']
const visCells = ref([])
let visInterval = null

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  code: '',
  captchaId: '',
  captchaCode: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度为2-50个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
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

onMounted(() => {
  initVisCells()
  startVisAnimation()
  loadCaptcha()
})

onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
  if (visInterval) window.clearInterval(visInterval)
})

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    form.captchaId = res.data?.captchaId || ''
    captchaImage.value = res.data?.captchaImage || ''
  } catch {} finally {
    captchaLoading.value = false
  }
}

async function validateFields(fields) {
  for (const field of fields) {
    const ok = await formRef.value.validateField(field).then(() => true).catch(() => false)
    if (!ok) return false
  }
  return true
}

async function handleSendCode() {
  const valid = await validateFields(['username', 'password', 'confirmPassword', 'email', 'captchaCode'])
  if (!valid) return
  const token = getTurnstileToken()
  if (!token) { ElMessage.warning('请完成人机验证'); return }

  sendCodeLoading.value = true
  try {
    await sendCode({
      email: form.email,
      captchaId: form.captchaId,
      captchaCode: form.captchaCode,
      purpose: 'register',
      turnstileToken: token
    })
    ElMessage.success('邮箱验证码已发送')
    form.captchaCode = ''
    startCountdown()
    resetTurnstile()
    await loadCaptcha()
  } catch {
    form.captchaCode = ''
    await loadCaptcha()
  } finally {
    sendCodeLoading.value = false
  }
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const token = getTurnstileToken()
  if (!token) { ElMessage.warning('请完成人机验证'); return }

  loading.value = true
  try {
    const { confirmPassword, captchaId, captchaCode, ...payload } = form
    await register({ ...payload, turnstileToken: token })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {} finally {
    resetTurnstile()
    loading.value = false
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

function getTurnstileToken() {
  return turnstileRef.value?.getToken?.() || turnstileToken.value
}

function resetTurnstile() {
  turnstileToken.value = ''
  turnstileRef.value?.reset?.()
}
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
  display: inline-flex;
  width: max-content;
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
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.oauth-btn {
  min-height: 42px;
  transition: background .16s ease, border-color .16s ease, transform .16s var(--ad-ease);
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
