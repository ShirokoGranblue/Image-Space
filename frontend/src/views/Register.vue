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
  font-family: 'Playfair Display', serif;
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
  font-family: 'Playfair Display', serif;
  font-size: 28px;
  font-weight: 400;
  color: var(--ink);
  line-height: 1.2;
  margin-bottom: 28px;
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
  font-family: 'DM Sans', sans-serif;
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
  font-family: 'Playfair Display', serif;
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
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
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
