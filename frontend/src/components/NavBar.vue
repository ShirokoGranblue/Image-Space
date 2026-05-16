<template>
  <el-header class="navbar">
    <div class="navbar-inner">
      <div class="navbar-left">
        <h2 @click="$router.push('/home')" class="logo">ImageSpace</h2>
      </div>
      <div class="navbar-right">
        <nav class="nav-links">
          <router-link to="/home" class="nav-link" :class="{ active: $route.path === '/home' }">
            <span class="nav-label">我的图片</span>
          </router-link>
          <router-link to="/square" class="nav-link" :class="{ active: $route.path === '/square' }">
            <span class="nav-label">图片广场</span>
          </router-link>
        </nav>
        <div class="user-section" v-if="token">
          <span class="username" @click="goProfile">{{ userInfo?.displayName || userInfo?.username || '' }}</span>
          <button class="logout-btn" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </div>
  </el-header>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { logout } from '../api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const token = computed(() => userStore.token)
const userInfo = computed(() => userStore.userInfo)

onMounted(() => {
  if (userStore.token && !userStore.userInfo) {
    userStore.fetchUserInfo()
  }
})

function goProfile() {
  router.push(`/profile/${userStore.userInfo?.id || 0}`)
}

async function handleLogout() {
  try {
    await logout()
    ElMessage.success('已退出登录')
  } catch {}
  userStore.clearToken()
  router.push('/login')
}
</script>

<style scoped>
.navbar {
  background: rgba(255, 255, 255, 0.86);
  border-bottom: 1px solid var(--border-subtle);
  padding: 0;
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(12px);
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.04);
}

.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--space-lg);
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 750;
  color: var(--accent);
  cursor: pointer;
  margin: 0;
  letter-spacing: 0;
  transition: color 0.2s ease;
}

.logo:hover {
  color: var(--accent-glow);
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
}

.nav-links {
  display: flex;
  gap: var(--space-xs);
}

.nav-link {
  display: flex;
  flex-direction: column;
  padding: var(--space-sm) var(--space-md);
  text-decoration: none;
  border-radius: var(--radius-md);
  transition: background 0.2s ease;
  line-height: 1.2;
}

.nav-link:hover {
  background: var(--bg-hover);
}

.nav-label {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 650;
  color: var(--text-secondary);
  letter-spacing: 0;
  transition: color 0.2s ease;
}

.nav-sub {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 0;
}

.nav-link.active .nav-label {
  color: var(--accent);
}

.nav-link.active {
  background: rgba(37, 99, 235, 0.08);
  position: relative;
}
.nav-link.active::after {
  content: '';
  position: absolute;
  bottom: 4px;
  left: 50%;
  transform: translateX(-50%);
  width: 18px;
  height: 2.5px;
  border-radius: 2px;
  background: var(--accent);
  animation: fadeIn 0.25s ease;
}

.user-section {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding-left: var(--space-md);
  border-left: 1px solid var(--border-subtle);
}

.username {
  font-family: var(--font-display);
  font-size: 16px;
  color: var(--text-primary);
  cursor: pointer;
  transition: color 0.2s ease;
}

.username:hover {
  color: var(--accent);
}

.logout-btn {
  background: none;
  border: 1px solid var(--border-subtle);
  color: var(--text-muted);
  font-size: 11px;
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  letter-spacing: 0;
  text-transform: none;
  transition: all 0.2s ease;
  font-family: var(--font-body);
}

.logout-btn:hover {
  color: var(--danger);
  border-color: var(--danger-dim);
}

@media (max-width: 640px) {
  .navbar-inner { padding: 0 var(--space-md); }
  .logo { font-size: 20px; }
  .nav-link { padding: var(--space-xs) var(--space-sm); }
  .nav-label { font-size: 14px; }
  .nav-sub { display: none; }
  .user-section { gap: var(--space-sm); padding-left: var(--space-sm); }
  .username { font-size: 14px; }
}
</style>
