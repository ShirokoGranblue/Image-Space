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
        <el-tab-pane label="验证码登录" name="code"></el-tab-pane>
        <el-tab-pane label="短信登录" name="sms"></el-tab-pane>
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
            Log in
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
            <span v-if="captchaExpiry > 0" style="font-size:12px;color:#999;white-space:nowrap">{{ captchaExpiry }}s</span>
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
      <div v-show="loginMode === 'sms'" class="login-form">
        <el-form-item label="手机号">
          <el-input v-model="smsForm.phone" placeholder="输入已绑定的手机号" size="large" :prefix-icon="Phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="图形验证码">
          <div style="display:flex;gap:8px;align-items:center">
            <el-input v-model="smsCaptchaCode" placeholder="4位验证码" size="large" maxlength="4" style="flex:1" />
            <img :src="captchaImage" @click="fetchCaptcha" style="height:40px;cursor:pointer;border-radius:4px;border:1px solid #ddd" title="点击刷新" />
            <span v-if="captchaExpiry > 0" style="font-size:12px;color:#999;white-space:nowrap">{{ captchaExpiry }}s</span>
          </div>
        </el-form-item>
        <el-form-item label="短信验证码">
          <div style="display:flex;gap:8px;width:100%">
            <el-input v-model="smsForm.code" placeholder="6位数字" size="large" maxlength="6" style="flex:1" />
            <el-button size="large" @click="handleSendSmsCode" :loading="smsSending" :disabled="smsCountdown > 0" style="min-width:120px">
              {{ smsCountdown > 0 ? smsCountdown + 's' : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" @click="handleSmsLogin" :loading="loading">
            验证并登录
          </el-button>
        </el-form-item>
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
import { User, Lock, Message, Phone } from '@element-plus/icons-vue'
import { login, sendCode, loginByCode, getCaptcha, sendSmsCode, loginBySmsCode } from '../api/user'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const loginMode = ref('password')
const sending = ref(false)
const countdown = ref(0)
let countdownTimer = null

const codeForm = reactive({
  email: '',
  code: ''
})

const captchaId = ref('')
const captchaImage = ref('')
const captchaCode = ref('')
const smsCaptchaCode = ref('')
const captchaExpiry = ref(0)
let captchaTimerId = null

const smsForm = reactive({
  phone: '',
  code: ''
})
const smsSending = ref(false)
const smsCountdown = ref(0)
let smsCountdownTimer = null

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

watch(loginMode, (mode) => {
  if (mode === 'code' || mode === 'sms') {
    fetchCaptcha()
  }
})

onMounted(() => {
  if (loginMode.value === 'code' || loginMode.value === 'sms') {
    fetchCaptcha()
  }
})

onUnmounted(() => {
  clearCaptchaTimer()
  if (countdownTimer) clearInterval(countdownTimer)
  if (smsCountdownTimer) clearInterval(smsCountdownTimer)
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
    fetchCaptcha()
    countdown.value = 60
    countdownTimer = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(countdownTimer) }, 1000)
  } catch {} finally { sending.value = false }
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

async function handleSendSmsCode() {
  if (!smsForm.phone) { ElMessage.warning('请输入手机号'); return }
  if (!smsCaptchaCode.value) { ElMessage.warning('请输入图形验证码'); return }
  smsSending.value = true
  try {
    await sendSmsCode({ phone: smsForm.phone.trim(), captchaId: captchaId.value, captchaCode: smsCaptchaCode.value })
    ElMessage.success('验证码已发送')
    fetchCaptcha()
    smsCountdown.value = 60
    smsCountdownTimer = setInterval(() => { smsCountdown.value--; if (smsCountdown.value <= 0) clearInterval(smsCountdownTimer) }, 1000)
  } catch {} finally { smsSending.value = false }
}

async function handleSmsLogin() {
  if (!smsForm.phone) { ElMessage.warning('请输入手机号'); return }
  if (!smsForm.code) { ElMessage.warning('请输入验证码'); return }
  loading.value = true
  try {
    const res = await loginBySmsCode({ phone: smsForm.phone.trim(), code: smsForm.code.trim() })
    userStore.setToken(res.data)
    await userStore.fetchUserInfo()
    ElMessage.success('欢迎回来')
    router.push('/home')
  } catch {} finally { loading.value = false }
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
