<template>
  <header class="navbar" ref="navbarEl">
    <div class="navbar-inner">
      <router-link to="/home" class="logo" aria-label="Image Space 首页">
        <span class="logo-mark">IS</span>
        <span>IMAGE SPACE</span>
      </router-link>

      <nav class="nav-links" role="navigation" aria-label="主导航">
        <router-link to="/square" class="nav-link" :class="{ active: $route.path === '/square' }">
          Square
        </router-link>
        <router-link to="/home" class="nav-link" :class="{ active: $route.path === '/home' }">
          Images
        </router-link>
        <router-link v-if="token" :to="profilePath" class="nav-link" :class="{ active: $route.path.startsWith('/profile') }">
          Profile
        </router-link>
      </nav>

      <div class="navbar-right">
        <button v-if="token" class="upload-nav-btn" type="button" @click="goUpload">
          <el-icon><Plus /></el-icon>
          <span>Upload</span>
        </button>

        <div class="user-section" v-if="token">
          <NotificationBell :active="!!token" />
          <button class="avatar-button" type="button" @click="goProfile" :title="userInfo?.displayName || userInfo?.username">
            <el-avatar :size="32" :src="userInfo?.avatarUrl || userInfo?.avatar" class="nav-avatar" />
          </button>
          <button class="logout-btn" type="button" @click="handleLogout">Exit</button>
        </div>

        <button class="mobile-toggle" @click="mobileOpen = !mobileOpen" :aria-expanded="mobileOpen" aria-label="菜单">
          <span class="hamburger-line" :class="{ open: mobileOpen }"></span>
        </button>
      </div>
    </div>

    <transition name="slide-down">
      <div class="mobile-drawer" v-if="mobileOpen">
        <nav class="mobile-nav">
          <router-link to="/square" class="mobile-nav-item" :class="{ active: $route.path === '/square' }" @click="mobileOpen = false">
            Square
          </router-link>
          <router-link to="/home" class="mobile-nav-item" :class="{ active: $route.path === '/home' }" @click="mobileOpen = false">
            Images
          </router-link>
          <router-link v-if="token" :to="profilePath" class="mobile-nav-item" :class="{ active: $route.path.startsWith('/profile') }" @click="mobileOpen = false">
            Profile
          </router-link>
        </nav>
        <div class="mobile-actions" v-if="token">
          <button class="upload-nav-btn" type="button" @click="goUpload">
            <el-icon><Plus /></el-icon>
            <span>Upload</span>
          </button>
          <NotificationBell :active="!!token" />
          <button class="logout-btn" type="button" @click="handleLogout">Exit</button>
        </div>
      </div>
    </transition>
  </header>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { logout } from '../api/user'
import { ElMessage } from 'element-plus'
import NotificationBell from './NotificationBell.vue'

const router = useRouter()
const userStore = useUserStore()

const token = computed(() => userStore.token)
const userInfo = computed(() => userStore.userInfo)
const profilePath = computed(() => `/profile/${userInfo.value?.uuid || 0}`)
const mobileOpen = ref(false)
const navbarEl = ref(null)

onMounted(() => {
  if (userStore.token && !userStore.userInfo) {
    userStore.fetchUserInfo()
  }
  window.addEventListener('scroll', onScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})

function onScroll() {
  if (navbarEl.value) {
    navbarEl.value.classList.toggle('scrolled', window.scrollY > 40)
  }
}

function goProfile() {
  mobileOpen.value = false
  router.push(profilePath.value)
}

function goUpload() {
  mobileOpen.value = false
  router.push({ path: '/home', query: { upload: '1' } })
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
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  padding: 10px 24px;
  background: var(--nav-blue);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 12px 30px rgba(3, 25, 47, 0.12);
  transition: box-shadow 0.2s ease, background 0.2s ease;
}

.navbar.scrolled {
  background: rgba(3, 25, 47, 0.98);
  box-shadow: 0 14px 34px rgba(3, 25, 47, 0.16);
}

.navbar-inner {
  width: min(100%, 1280px);
  height: 36px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  color: #fff;
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.08em;
  text-decoration: none;
}

.logo:hover {
  opacity: 0.9;
}

.logo-mark {
  width: 30px;
  height: 30px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: #fff;
  font-size: 11px;
  letter-spacing: 0.04em;
}

.nav-links {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.nav-link {
  min-width: 76px;
  padding: 7px 14px;
  border-radius: 999px;
  color: #cfe1ed;
  font-size: 13px;
  font-weight: 300;
  text-align: center;
  text-decoration: none;
  transition: color 0.18s ease, background 0.18s ease;
}

.nav-link:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  opacity: 1;
}

.nav-link.active {
  color: var(--nav-blue);
  background: #fffdf8;
}

.navbar-right,
.user-section {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.upload-nav-btn,
.logout-btn,
.avatar-button {
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border-radius: 14px;
  height: 34px;
  padding: 0 13px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 300;
  cursor: pointer;
  transition: transform 0.16s ease, background 0.18s ease, border-color 0.18s ease;
}

.upload-nav-btn:hover,
.logout-btn:hover,
.avatar-button:hover {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.28);
}

.upload-nav-btn:active,
.logout-btn:active,
.avatar-button:active {
  transform: scale(0.98);
}

.avatar-button {
  width: 36px;
  padding: 0;
  border-radius: 50%;
  overflow: hidden;
}

.nav-avatar {
  border: 1px solid rgba(255, 255, 255, 0.24);
}

.navbar :deep(.notification-bell) {
  color: #cfe1ed;
  border-radius: 12px;
}

.navbar :deep(.notification-bell:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.logout-btn {
  color: #d9e7f2;
}

.mobile-toggle {
  display: none;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  cursor: pointer;
  padding: 0;
}

.hamburger-line,
.hamburger-line::before,
.hamburger-line::after {
  display: block;
  width: 18px;
  height: 2px;
  background: #fff;
  border-radius: 999px;
  transition: all 0.22s var(--ease-out);
}

.hamburger-line {
  position: relative;
  margin: auto;
}

.hamburger-line::before,
.hamburger-line::after {
  content: '';
  position: absolute;
  left: 0;
}

.hamburger-line::before { top: -6px; }
.hamburger-line::after { top: 6px; }
.hamburger-line.open { background: transparent; }
.hamburger-line.open::before { top: 0; transform: rotate(45deg); }
.hamburger-line.open::after { top: 0; transform: rotate(-45deg); }

.mobile-drawer {
  width: min(100%, 1280px);
  margin: 10px auto 0;
  padding: 12px;
  background: var(--nav-blue-soft);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
}

.mobile-nav,
.mobile-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.mobile-actions {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.mobile-nav-item {
  flex: 1 1 110px;
  padding: 10px 12px;
  color: #cfe1ed;
  border-radius: 12px;
  text-align: center;
  font-size: 13px;
  font-weight: 300;
}

.mobile-nav-item.active,
.mobile-nav-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  opacity: 1;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 860px) {
  .navbar {
    padding: 10px 14px;
  }
  .navbar-inner {
    gap: 10px;
  }
  .nav-links,
  .user-section,
  .navbar-right > .upload-nav-btn {
    display: none;
  }
  .mobile-toggle {
    display: grid;
    place-items: center;
  }
}

@media (max-width: 480px) {
  .logo span:last-child {
    font-size: 12px;
  }
}
</style>
