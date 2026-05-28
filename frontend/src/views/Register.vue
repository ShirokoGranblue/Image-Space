<template>
  <div class="auth-page">
    <!-- Left: Form -->
    <div class="auth-form-side">
      <div class="auth-form-wrap">
        <router-link to="/home" class="auth-logo">ImageSpace</router-link>
        <h1 class="auth-title">创建账号</h1>

        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="auth-form" @submit.prevent="handleRegister">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="你的唯一用户名" size="large" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="至少 6 位密码" size="large" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" size="large"
              @keyup.enter="handleRegister" show-password />
          </el-form-item>
          <el-form-item label="邮箱（选填）">
            <el-input v-model="form.email" placeholder="your@email.com" size="large" />
          </el-form-item>
          <el-form-item label="手机号（选填）">
            <el-input v-model="form.phone" placeholder="选填" maxlength="20" size="large" />
          </el-form-item>
          <TurnstileWidget
            ref="turnstileRef"
            @verified="turnstileToken = $event"
            @expired="turnstileToken = ''"
            @error="turnstileToken = ''"
          />
          <el-form-item>
            <el-button type="primary" size="large" class="auth-submit" @click="handleRegister" :loading="loading">
              创建账号
            </el-button>
          </el-form-item>
        </el-form>

        <p class="auth-footer">
          已有账号？<router-link to="/login">返回登录</router-link>
        </p>
      </div>
    </div>

    <!-- Right: Diagonal gallery -->
    <div class="auth-gallery-side">
      <div class="gallery-outer">
        <div class="gallery-inner">
          <div class="gallery-track" :style="trackStyle">
            <div class="gallery-grid" v-for="copy in 2" :key="copy">
              <img
                v-for="img in galleryImages"
                :key="`${copy}-${img.id}`"
                :src="img.imageUrl"
                :alt="img.imageName"
                class="gallery-img"
                loading="lazy"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/user'
import { getImageList } from '../api/image'
import { ElMessage } from 'element-plus'
import TurnstileWidget from '../components/TurnstileWidget.vue'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const turnstileRef = ref(null)
const turnstileToken = ref('')
const galleryImages = ref([])

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: ''
})

const trackStyle = computed(() => ({
  animationDuration: `${Math.max(60, galleryImages.value.length * 3)}s`
}))

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

onMounted(async () => {
  try {
    const res = await getImageList({ page: 1, limit: 30, visibility: 'PUBLIC' })
    galleryImages.value = (res.data?.records || []).filter(img => img.imageUrl)
  } catch {}
})

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const token = getTurnstileToken()
  if (!token) { ElMessage.warning('请完成人机验证'); return }

  loading.value = true
  try {
    await register({ ...form, turnstileToken: token })
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
  display: flex;
  min-height: 100vh;
  min-height: 100dvh;
}

/* ── Left: Form ── */
.auth-form-side {
  width: 42%;
  max-width: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  background: var(--bg-base);
}

.auth-form-wrap {
  width: 100%;
  max-width: 380px;
  animation: fadeUp 0.5s var(--ease-out);
}

.auth-logo {
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.auth-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 8px 0 32px;
  letter-spacing: -0.01em;
}

.auth-form {
  margin-top: 0;
}

.auth-form :deep(.el-form-item__label) {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted) !important;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding-bottom: 4px;
}

.auth-submit {
  width: 100%;
  height: 46px;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: -0.01em;
  margin-top: 4px;
}

.auth-footer {
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
  margin-top: 24px;
}

.auth-footer a {
  color: var(--text-primary);
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
}

/* ── Right: Diagonal gallery ── */
.auth-gallery-side {
  width: 58%;
  position: relative;
  overflow: hidden;
  background: var(--bg-surface);
  border-left: 1px solid var(--border-subtle);
}

.gallery-outer {
  position: absolute;
  inset: -10%;
  overflow: hidden;
}

.gallery-inner {
  position: absolute;
  inset: -20%;
  transform: rotate(15deg);
}

.gallery-track {
  animation: scrollDiagonal 80s linear infinite;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 8px;
}

.gallery-img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  filter: grayscale(30%);
  transition: filter 0.4s;
}

.gallery-img:hover {
  filter: grayscale(0%);
}

@keyframes scrollDiagonal {
  0% { transform: translateY(0); }
  100% { transform: translateY(-50%); }
}

/* ── Mobile ── */
@media (max-width: 768px) {
  .auth-page {
    flex-direction: column;
  }

  .auth-gallery-side {
    width: 100%;
    height: 180px;
    order: 1;
    border-left: none;
    border-bottom: 1px solid var(--border-subtle);
  }

  .gallery-outer {
    inset: -30%;
  }

  .gallery-inner {
    inset: -40%;
    transform: rotate(12deg);
  }

  .gallery-grid {
    grid-template-columns: repeat(6, 1fr);
    gap: 4px;
  }

  .auth-form-side {
    width: 100%;
    max-width: none;
    order: 2;
    padding: 32px 24px;
  }
}
</style>
