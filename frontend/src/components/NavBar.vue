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
          <NotificationBell :active="!!token" />
          <el-avatar :size="32" :src="userInfo?.avatarUrl || userInfo?.avatar" class="nav-avatar" @click="goProfile" />
          <span class="username" @click="goProfile" :title="userInfo?.displayName || userInfo?.username">
            {{ userInfo?.displayName || userInfo?.username || '' }}
          </span>
        </div>
        <button v-if="token" class="logout-btn" @click="handleLogout" aria-label="退出登录">Exit</button>

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
          <NotificationBell :active="!!token" />
          <el-avatar :size="28" :src="userInfo?.avatarUrl || userInfo?.avatar" />
          <span>{{ userInfo?.displayName || userInfo?.username }}</span>
          <button class="logout-btn" @click="handleLogout">Exit</button>
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
import NotificationBell from './NotificationBell.vue'

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
  background: var(--bg-base);
  border-bottom: 1px solid var(--border-subtle);
  padding: 0;
  height: 60px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 28px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 600;
  font-style: italic;
  color: var(--text-primary);
  text-decoration: none;
  letter-spacing: -0.01em;
  transition: opacity 0.2s;
}
.logo:hover { opacity: 0.6; }

.navbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.nav-links {
  display: flex;
  gap: 0;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  text-decoration: none;
  position: relative;
}
.nav-link::after {
  content: '';
  position: absolute;
  bottom: 0; left: 14px; right: 14px;
  height: 2px;
  background: var(--text-primary);
  transform: scaleX(0);
  transition: transform 0.2s var(--ease-out);
}
.nav-link:hover::after,
.nav-link.active::after { transform: scaleX(1); }

.nav-icon { font-size: 16px; color: var(--text-muted); transition: color 0.2s; }
.nav-label { font-size: 13px; font-weight: 500; color: var(--text-muted); transition: color 0.2s; }
.nav-link.active .nav-label,
.nav-link.active .nav-icon { color: var(--text-primary); }
.nav-link:hover .nav-label,
.nav-link:hover .nav-icon { color: var(--text-primary); }

.user-section {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.nav-avatar { flex-shrink: 0; cursor: pointer; }
.username {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-btn {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
  background: none;
  border: 1px solid var(--border-visible);
  padding: 5px 14px;
  cursor: pointer;
  transition: all 0.2s;
  letter-spacing: 0;
}
.logout-btn:hover { color: var(--danger); border-color: var(--danger); }

/* Mobile */
.mobile-toggle {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
}

.hamburger-line,
.hamburger-line::before,
.hamburger-line::after {
  display: block;
  width: 20px; height: 2px;
  background: var(--text-primary);
  transition: all 0.25s var(--ease-out);
}
.hamburger-line { position: relative; }
.hamburger-line::before,
.hamburger-line::after {
  content: '';
  position: absolute; left: 0;
}
.hamburger-line::before { top: -6px; }
.hamburger-line::after { top: 6px; }
.hamburger-line.open { background: transparent; }
.hamburger-line.open::before { top: 0; transform: rotate(45deg); }
.hamburger-line.open::after { top: 0; transform: rotate(-45deg); }

.mobile-drawer {
  position: absolute;
  top: 60px; left: 0; right: 0;
  background: var(--bg-base);
  border-bottom: 1px solid var(--border-subtle);
  padding: 12px 24px 20px;
  z-index: 99;
}

.mobile-nav {
  display: flex; flex-direction: column; gap: 2px;
}
.mobile-nav-item {
  display: flex; align-items: center; gap: 8px;
  padding: 12px;
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 14px; font-weight: 500;
  transition: color 0.15s;
}
.mobile-nav-item:hover,
.mobile-nav-item.active { color: var(--text-primary); }

.mobile-user {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 0 0; margin-top: 8px;
  border-top: 1px solid var(--border-subtle);
}
.mobile-user span { flex: 1; font-size: 13px; font-weight: 500; color: var(--text-primary); }

.slide-down-enter-active,
.slide-down-leave-active { transition: all 0.2s var(--ease-out); }
.slide-down-enter-from,
.slide-down-leave-to { opacity: 0; transform: translateY(-8px); }

@media (max-width: 768px) {
  .navbar-inner { padding: 0 20px; }
  .nav-links { display: none; }
  .user-section { display: none; }
  .logout-btn { display: none; }
  .mobile-toggle { display: flex; align-items: center; justify-content: center; width: 32px; height: 28px; }
}

@media (max-width: 480px) {
  .navbar { height: 52px; }
  .mobile-drawer { top: 52px; }
  .logo { font-size: 20px; }
}
</style>
