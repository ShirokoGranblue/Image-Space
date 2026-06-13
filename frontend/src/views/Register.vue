<template>
  <div class="auth-page">
    <div class="login-wrap">
      <!-- Left side panel -->
      <div class="login-panel">
        <router-link to="/home" class="login-logo">IMAGESPACE</router-link>
        <div class="login-heading">创建<br>账号</div>

        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="auth-form register-form" @submit.prevent="handleRegister">
          <el-form-item class="full-field" label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="你的唯一用户名" size="large" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="至少 6 位密码" size="large" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" size="large"
              @keyup.enter="handleRegister" show-password />
          </el-form-item>
          <el-form-item label="邮箱（可选）">
            <el-input v-model="form.email" placeholder="your@email.com" size="large" />
          </el-form-item>
          <el-form-item label="手机号（可选）">
            <el-input v-model="form.phone" placeholder="选填" maxlength="20" size="large" />
          </el-form-item>
          <TurnstileWidget
            class="register-turnstile full-field"
            ref="turnstileRef"
            @verified="turnstileToken = $event"
            @expired="turnstileToken = ''"
            @error="turnstileToken = ''"
          />
          <el-form-item class="full-field compact-submit">
            <el-button type="primary" size="large" class="login-btn" @click="handleRegister" :loading="loading">
              创建账号
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          已有账号？<router-link to="/login">返回登录 ↗</router-link>
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
import { register } from '../api/user'
import { ElMessage } from 'element-plus'
import TurnstileWidget from '../components/TurnstileWidget.vue'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const turnstileRef = ref(null)
const turnstileToken = ref('')

const BLUE_TONES = ['#042C53', '#0C447C', '#185FA5', '#378ADD', '#85B7EB']
const visCells = ref([])
let visInterval = null

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: ''
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

onMounted(() => {
  initVisCells()
  startVisAnimation()
})

onUnmounted(() => {
  if (visInterval) window.clearInterval(visInterval)
})

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const token = getTurnstileToken()
  if (!token) { ElMessage.warning('请完成人机验证'); return }

  loading.value = true
  try {
    const { confirmPassword, ...payload } = form
    await register({ ...payload, turnstileToken: token })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {} finally {
    resetTurnstile()
    loading.value = false
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
}
</style>
