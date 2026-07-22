<template>
  <AuthLayout form-width="wide">
    <template #aside>
      <div class="auth-aside-copy">
        <span class="auth-aside-eyebrow">开始创建、上传</span>
        <h2 class="auth-aside-title">把零散的图片，慢慢整理成自己的收藏</h2>
        <p class="auth-aside-description">建立账号后，可以保存、整理并分享愿意公开的内容。</p>
      </div>

      <ul class="auth-capabilities" data-auth-capabilities aria-label="注册后可用能力">
        <li>集中保存图片</li>
        <li>整理分类与标签</li>
        <li>设置图片可见范围</li>
      </ul>
    </template>

    <template #header>
      <span class="auth-kicker">加入 AstralSpace</span>
      <h1 class="auth-title">创建账号</h1>
      <p class="auth-description">完成邮箱验证后即可开始保存图片。</p>
    </template>

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
          <el-input v-model="form.username" placeholder="你的唯一用户名" autocomplete="username" size="large" />
        </el-form-item>
        <el-form-item label="手机号（可选）">
          <el-input v-model="form.phone" placeholder="选填" autocomplete="tel" maxlength="20" size="large" />
        </el-form-item>
      </div>

      <div class="form-columns">
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="至少 6 位密码"
            autocomplete="new-password"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="再次输入密码"
            autocomplete="new-password"
            size="large"
            show-password
          />
        </el-form-item>
      </div>

      <div class="form-columns email-verification-controls">
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="form.email"
            placeholder="name@example.com"
            autocomplete="email"
            size="large"
            @input="clearEmailCodeFeedback"
          />
        </el-form-item>
        <el-form-item label="邮箱验证码" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="输入邮箱验证码" autocomplete="one-time-code" size="large" />
            <el-button
              type="primary"
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
      </div>

      <p
        v-if="emailCodeFeedback"
        class="operation-feedback email-code-feedback"
        :class="`is-${emailCodeFeedback.type}`"
        role="status"
        aria-live="polite"
      >
        {{ emailCodeFeedback.message }}
      </p>

      <el-form-item label="图形验证码" prop="captchaCode">
        <div class="captcha-row-inline">
          <el-input v-model="form.captchaCode" placeholder="输入图形验证码" autocomplete="off" size="large" />
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

      <p
        v-if="registerFeedback"
        class="operation-feedback register-feedback"
        :class="`is-${registerFeedback.type}`"
        role="status"
        aria-live="polite"
      >
        {{ registerFeedback.message }}
      </p>

      <div class="turnstile-section" aria-label="人机验证">
        <span class="section-label">人机验证</span>
        <TurnstileWidget
          ref="turnstileRef"
          class="register-turnstile"
          @verified="turnstileToken = $event"
          @expired="turnstileToken = ''"
          @error="turnstileToken = ''"
        />
      </div>

      <el-form-item class="submit-row">
        <el-button type="primary" native-type="submit" size="large" class="login-btn" :loading="loading">
          <span>创建账号</span>
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="auth-footer-copy">
        已有账号？
        <router-link class="auth-footer-link" to="/login">返回登录</router-link>
      </div>
    </template>
  </AuthLayout>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCaptcha, register, sendCode } from '../api/user'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import AuthLayout from '../components/auth/AuthLayout.vue'
import TurnstileWidget from '../components/TurnstileWidget.vue'
import { prepareRegisterPayload } from '../utils/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const sendCodeLoading = ref(false)
const captchaLoading = ref(false)
const turnstileRef = ref(null)
const turnstileToken = ref('')
const captchaImage = ref('')
const countdown = ref(0)
const emailCodeFeedback = ref(null)
const registerFeedback = ref(null)
let countdownTimer = null

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  code: '',
  captchaId: '',
  captchaCode: '',
})

const validateConfirmPassword = (_rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度为2-50个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  captchaCode: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
}

onMounted(() => {
  loadCaptcha()
})

onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
})

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    form.captchaId = res.data?.captchaId || ''
    captchaImage.value = res.data?.captchaImage || ''
  } catch {
    // The axios interceptor already reports the API error.
  } finally {
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
  const valid = await validateFields(['email'])
  if (!valid) {
    emailCodeFeedback.value = { type: 'error', message: '请先填写有效邮箱' }
    return
  }
  const token = getTurnstileToken()
  if (!token) {
    emailCodeFeedback.value = { type: 'error', message: '请先完成人机验证，再发送邮箱验证码' }
    ElMessage.warning('请完成人机验证')
    return
  }

  sendCodeLoading.value = true
  emailCodeFeedback.value = { type: 'info', message: `正在向 ${form.email} 发送验证码…` }
  try {
    await sendCode({
      email: form.email,
      purpose: 'register',
      turnstileToken: token,
    })
    ElMessage.success('验证码发送请求已受理，请留意收件箱')
    emailCodeFeedback.value = {
      type: 'success',
      message: '发送请求已受理，验证码 5 分钟内有效。邮件送达可能需要一点时间，请勿连续重复发送。',
    }
    startCountdown()
  } catch (error) {
    emailCodeFeedback.value = {
      type: 'error',
      message: error?.message || '验证码发送失败，请稍后重试',
    }
  } finally {
    resetTurnstile()
    sendCodeLoading.value = false
  }
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    registerFeedback.value = { type: 'error', message: '请检查标出的表单项后再创建账号' }
    return
  }
  const token = getTurnstileToken()
  if (!token) {
    registerFeedback.value = { type: 'error', message: '请先完成人机验证，再创建账号' }
    ElMessage.warning('请完成人机验证')
    return
  }

  loading.value = true
  registerFeedback.value = { type: 'info', message: '正在创建账号…' }
  try {
    const payload = prepareRegisterPayload(form)
    await register({ ...payload, turnstileToken: token })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    registerFeedback.value = {
      type: 'error',
      message: error?.message || '账号创建失败，请检查后重试',
    }
    form.captchaCode = ''
    await loadCaptcha()
  } finally {
    resetTurnstile()
    loading.value = false
  }
}

function clearEmailCodeFeedback() {
  emailCodeFeedback.value = null
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
.form-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.email-verification-controls {
  align-items: start;
}

.email-verification-controls :deep(.el-form-item) {
  min-width: 0;
}

.send-code-btn {
  --el-button-bg-color: var(--color-vermilion);
  --el-button-border-color: var(--color-vermilion);
  --el-button-text-color: var(--color-text-inverse);
  --el-button-hover-bg-color: var(--color-vermilion-hover);
  --el-button-hover-border-color: var(--color-vermilion-hover);
  --el-button-active-bg-color: var(--color-vermilion-hover);
  --el-button-active-border-color: var(--color-vermilion-hover);
  font-weight: 600;
}

.operation-feedback {
  margin: -14px 0 20px;
  padding: 10px 12px;
  border-left: 2px solid var(--color-border-strong);
  background: var(--color-surface-2);
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: var(--leading-sm);
}

.operation-feedback.is-success {
  border-left-color: var(--color-success);
  color: var(--color-success);
}

.operation-feedback.is-error {
  border-left-color: var(--color-error);
  color: var(--color-error);
}

.turnstile-section {
  margin: 0 0 8px;
}

.section-label {
  display: block;
  margin-bottom: 8px;
  color: var(--color-text-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: var(--leading-sm);
}

.register-turnstile {
  margin-bottom: 0;
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

@media (max-width: 640px) {
  .form-columns,
  .email-verification-controls {
    grid-template-columns: 1fr;
  }
}
</style>
