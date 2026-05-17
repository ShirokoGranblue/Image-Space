<template>
  <el-header class="navbar">
    <div class="navbar-inner">
      <div class="navbar-left">
        <router-link to="/home" class="logo" aria-label="ImageSpace 首页">ImageSpace</router-link>
      </div>

      <!-- Desktop nav -->
      <div class="navbar-right">
        <nav class="nav-links" role="navigation" aria-label="主导航">
          <router-link to="/home" class="nav-link" :class="{ active: $route.path === '/home' }">
            <el-icon class="nav-icon"><PictureFilled /></el-icon>
            <span class="nav-label">我的图片</span>
          </router-link>
          <router-link to="/square" class="nav-link" :class="{ active: $route.path === '/square' }">
            <el-icon class="nav-icon"><Grid /></el-icon>
            <span class="nav-label">图片广场</span>
          </router-link>
        </nav>

        <div class="user-section" v-if="token">
          <el-avatar :size="32" :src="userInfo?.avatar" class="nav-avatar" @click="goProfile" />
          <span class="username" @click="goProfile" :title="userInfo?.displayName || userInfo?.username">
            {{ userInfo?.displayName || userInfo?.username || '' }}
          </span>
          <button class="logout-btn" @click="handleLogout" aria-label="退出登录">退出</button>
        </div>

        <!-- Mobile hamburger -->
        <button class="mobile-toggle" @click="mobileOpen = !mobileOpen" :aria-expanded="mobileOpen" aria-label="菜单"
          @keydown.escape="mobileOpen = false">
          <span class="hamburger-line" :class="{ open: mobileOpen }"></span>
        </button>

      </div>
    </div>

    <!-- Mobile drawer -->
    <transition name="slide-down">
      <div class="mobile-drawer" v-if="mobileOpen" @keydown.escape="mobileOpen = false">
        <nav class="mobile-nav">
          <router-link to="/home" class="mobile-nav-item" :class="{ active: $route.path === '/home' }" @click="mobileOpen = false">
            <el-icon><PictureFilled /></el-icon> 我的图片
          </router-link>
          <router-link to="/square" class="mobile-nav-item" :class="{ active: $route.path === '/square' }" @click="mobileOpen = false">
            <el-icon><Grid /></el-icon> 图片广场
          </router-link>
        </nav>
        <div class="mobile-user" v-if="token">
          <el-avatar :size="28" :src="userInfo?.avatar" />
          <span>{{ userInfo?.displayName || userInfo?.username }}</span>
          <button class="logout-btn" @click="handleLogout">退出</button>
        </div>
      </div>
    </transition>
  </el-header>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { logout } from '../api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const token = computed(() => userStore.token)
const userInfo = computed(() => userStore.userInfo)
const mobileOpen = ref(false)

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
  mobileOpen.value = false
  router.push('/login')
}
</script>

<style scoped>
.navbar {
  background: rgba(255, 255, 255, 0.88);
  border-bottom: 1px solid var(--border-subtle);
  padding: 0;
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: saturate(180%) blur(16px);
  -webkit-backdrop-filter: saturate(180%) blur(16px);
  box-shadow: var(--shadow-sm);
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

.navbar-left { display: flex; align-items: center; }

.logo {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 750;
  color: var(--accent);
  text-decoration: none;
  cursor: pointer;
  letter-spacing: 0;
  transition: color 0.2s ease;
  padding: 4px 0;
}
.logo:hover { color: var(--accent-glow); }

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
  align-items: center;
  gap: 6px;
  padding: var(--space-sm) var(--space-md);
  text-decoration: none;
  border-radius: var(--radius-md);
  transition: background 0.2s ease, color 0.2s ease;
  min-height: 40px;
}

.nav-link:hover { background: var(--bg-hover); }

.nav-icon {
  font-size: 18px;
  color: var(--text-muted);
  transition: color 0.2s ease;
}

.nav-label {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
  color: var(--text-secondary);
  letter-spacing: 0;
  transition: color 0.2s ease;
}

.nav-link.active .nav-label,
.nav-link.active .nav-icon {
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
  width: 20px;
  height: 2.5px;
  border-radius: 2px;
  background: var(--accent);
  animation: fadeIn 0.25s ease;
}

.user-section {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding-left: var(--space-lg);
  border-left: 1px solid var(--border-subtle);
}

.nav-avatar {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  flex-shrink: 0;
}
.nav-avatar:hover {
  transform: scale(1.08);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.username {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 550;
  color: var(--text-primary);
  cursor: pointer;
  transition: color 0.2s ease;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.username:hover { color: var(--accent); }

.logout-btn {
  background: none;
  border: 1px solid var(--border-subtle);
  color: var(--text-muted);
  font-size: 12px;
  padding: 5px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: var(--font-body);
  white-space: nowrap;
  min-height: 32px;
}
.logout-btn:hover {
  color: var(--danger);
  border-color: var(--danger);
  background: rgba(220, 38, 38, 0.04);
}

/* ── Mobile toggle ── */
.mobile-toggle {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: var(--radius-sm);
}
.mobile-toggle:hover { background: var(--bg-hover); }

.hamburger-line,
.hamburger-line::before,
.hamburger-line::after {
  display: block;
  width: 22px;
  height: 2px;
  background: var(--text-primary);
  border-radius: 2px;
  transition: all 0.25s var(--ease-out);
}
.hamburger-line { position: relative; }
.hamburger-line::before,
.hamburger-line::after {
  content: '';
  position: absolute;
  left: 0;
}
.hamburger-line::before { top: -7px; }
.hamburger-line::after { top: 7px; }
.hamburger-line.open { background: transparent; }
.hamburger-line.open::before { top: 0; transform: rotate(45deg); }
.hamburger-line.open::after { top: 0; transform: rotate(-45deg); }

/* ── Mobile drawer ── */
.mobile-drawer {
  position: absolute;
  top: 64px;
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: saturate(180%) blur(16px);
  -webkit-backdrop-filter: saturate(180%) blur(16px);
  border-bottom: 1px solid var(--border-subtle);
  padding: var(--space-md);
  box-shadow: var(--shadow-lg);
  z-index: 99;
}

.mobile-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  text-decoration: none;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 550;
  transition: background 0.15s ease, color 0.15s ease;
}
.mobile-nav-item:hover { background: var(--bg-hover); }
.mobile-nav-item.active {
  color: var(--accent);
  background: rgba(37, 99, 235, 0.08);
}

.mobile-user {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 12px 16px;
  margin-top: var(--space-sm);
  border-top: 1px solid var(--border-subtle);
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.25s var(--ease-out);
}
.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ── Responsive ── */
@media (max-width: 768px) {
  .navbar-inner { padding: 0 var(--space-md); }
  .logo { font-size: 21px; }
  .nav-links { display: none; }
  .user-section { display: none; }
  .mobile-toggle { display: flex; }
}

@media (max-width: 480px) {
  .navbar { height: 56px; }
  .mobile-drawer { top: 56px; }
  .logo { font-size: 19px; }
}
</style>
