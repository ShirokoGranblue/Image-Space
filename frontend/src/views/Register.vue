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
          <el-icon :size="36"><UserFilled /></el-icon>
        </div>
        <h1 class="wordmark">创建账号</h1>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="login-form" @submit.prevent="handleRegister">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="你的唯一用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="至少 6 位密码" size="large" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" size="large"
            @keyup.enter="handleRegister" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item label="邮箱（选填）">
          <el-input v-model="form.email" placeholder="your@email.com" size="large" :prefix-icon="Message" />
        </el-form-item>
        <el-form-item label="手机号（选填）">
          <el-input v-model="form.phone" placeholder="选填" maxlength="20" size="large" :prefix-icon="Phone" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" @click="handleRegister" :loading="loading">
            创建账号
          </el-button>
        </el-form-item>
      </el-form>
      <p class="footer-link">
        已有账号？<router-link to="/login">返回登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Message, Phone } from '@element-plus/icons-vue'
import { register } from '../api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

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

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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

.login-bg { position: absolute; inset: 0; overflow: hidden; }

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
  font-size: 32px;
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

.login-form { margin-top: var(--space-sm); }

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
.login-btn:not(.is-loading):active { transform: translateY(0); }

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

@media (max-width: 480px) {
  .login-frame {
    width: calc(100vw - 32px);
    padding: 32px 24px;
    border-radius: var(--radius-lg);
  }
  .wordmark { font-size: 26px; }
  .tagline { font-size: 14px; }
  .brand-icon { width: 56px; height: 56px; }
}
</style>
