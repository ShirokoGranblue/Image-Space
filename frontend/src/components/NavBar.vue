<template>
  <header class="navbar" ref="navbarEl">
    <div class="navbar-inner">
      <router-link to="/home" class="logo" aria-label="Image Space 首页">
        <img class="logo-mark" :src="logoIcon" alt="" aria-hidden="true" />
        <span>IMAGE SPACE</span>
      </router-link>

      <nav class="nav-links" role="navigation" aria-label="主导航">
        <router-link to="/home" class="nav-link" :class="{ active: $route.path === '/home' }">
          Images
        </router-link>
        <router-link to="/square" class="nav-link" :class="{ active: $route.path === '/square' }">
          Space
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
          <router-link to="/home" class="mobile-nav-item" :class="{ active: $route.path === '/home' }" @click="mobileOpen = false">
            Images
          </router-link>
          <router-link to="/square" class="mobile-nav-item" :class="{ active: $route.path === '/square' }" @click="mobileOpen = false">
            Space
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
import logoIcon from '../logo/60060cf8-de2e-4faf-8818-668b5132988b.png'

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
  top: 14px;
  left: 0;
  right: 0;
  z-index: 100;
  padding: 0 22px;
  pointer-events: none;
}

.navbar-inner {
  width: min(100%, 1360px);
  min-height: 58px;
  margin: 0 auto;
  padding: 10px 12px 10px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  pointer-events: auto;
  background: rgba(7, 17, 31, 0.68);
  border: 1px solid rgba(255, 253, 248, 0.14);
  border-radius: 18px;
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.22);
  backdrop-filter: blur(22px);
  transition: min-height 0.2s var(--ease-cinema), background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.navbar.scrolled .navbar-inner {
  min-height: 50px;
  background: rgba(7, 17, 31, 0.82);
  border-color: rgba(255, 253, 248, 0.2);
  box-shadow: 0 14px 48px rgba(0, 0, 0, 0.28);
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 11px;
  flex-shrink: 0;
  color: var(--paper);
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-decoration: none;
}

.logo:hover {
  opacity: 0.92;
}

.logo-mark {
  width: 32px;
  height: 32px;
  display: block;
  object-fit: contain;
  filter: drop-shadow(0 8px 18px rgba(55, 138, 221, 0.22));
}

.nav-links {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid rgba(255, 253, 248, 0.1);
  border-radius: 999px;
  background: rgba(255, 253, 248, 0.06);
}

.nav-link {
  position: relative;
  font-size: 14px;
  padding: 8px 15px;
  border-radius: 999px;
  border: none;
  background: transparent;
  color: rgba(247, 243, 232, 0.72);
  cursor: pointer;
  font-family: var(--font-body);
  transition: background .18s ease, color .18s ease, transform .18s var(--ease-cinema);
  white-space: nowrap;
  text-decoration: none;
}

.nav-link:hover {
  color: var(--paper);
  background: rgba(255, 253, 248, 0.08);
  opacity: 1;
  transform: translateY(-1px);
}

.nav-link.active {
  background: var(--paper);
  color: var(--cinema);
}

.navbar-right,
.user-section {
  display: inline-flex;
  align-items: center;
  gap: 9px;
}

.upload-nav-btn,
.logout-btn,
.avatar-button {
  border: 1px solid rgba(255, 253, 248, 0.14);
  background: rgba(255, 253, 248, 0.08);
  color: var(--paper);
  border-radius: 999px;
  height: 38px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.16s var(--ease-cinema), background 0.18s ease, border-color 0.18s ease, box-shadow 0.2s ease;
}

.upload-nav-btn {
  background: rgba(247, 243, 232, 0.92);
  color: var(--cinema);
  border-color: rgba(247, 243, 232, 0.7);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.18);
}

.upload-nav-btn:hover,
.logout-btn:hover,
.avatar-button:hover {
  background: rgba(255, 253, 248, 0.16);
  border-color: rgba(255, 253, 248, 0.3);
  box-shadow: 0 0 0 4px rgba(239, 159, 39, 0.08);
}

.upload-nav-btn:hover {
  background: #fffdf8;
}

.upload-nav-btn:active,
.logout-btn:active,
.avatar-button:active {
  transform: scale(0.97);
}

.avatar-button {
  width: 38px;
  padding: 0;
  border-radius: 50%;
  overflow: hidden;
}

.nav-avatar {
  border: 1px solid rgba(255, 253, 248, 0.28);
}

.navbar :deep(.notification-bell) {
  color: rgba(247, 243, 232, 0.78);
  border-radius: 999px;
}

.navbar :deep(.notification-bell:hover) {
  color: var(--paper);
  background: rgba(255, 253, 248, 0.1);
}

.logout-btn {
  color: rgba(247, 243, 232, 0.72);
}

.mobile-toggle {
  display: none;
  width: 38px;
  height: 38px;
  border: 1px solid rgba(255, 253, 248, 0.16);
  border-radius: 999px;
  background: rgba(255, 253, 248, 0.08);
  cursor: pointer;
  padding: 0;
}

.hamburger-line,
.hamburger-line::before,
.hamburger-line::after {
  display: block;
  width: 18px;
  height: 2px;
  background: var(--paper);
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
  width: min(100%, 1360px);
  margin: 10px auto 0;
  padding: 12px;
  pointer-events: auto;
  background: rgba(7, 17, 31, 0.84);
  border: 1px solid rgba(255, 253, 248, 0.14);
  border-radius: 18px;
  box-shadow: var(--shadow-cinematic);
  backdrop-filter: blur(22px);
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
  border-top: 1px solid rgba(255, 253, 248, 0.1);
}

.mobile-nav-item {
  flex: 1 1 110px;
  padding: 11px 12px;
  color: rgba(247, 243, 232, 0.72);
  border-radius: 999px;
  text-align: center;
  font-size: 15px;
  font-weight: 600;
}

.mobile-nav-item.active,
.mobile-nav-item:hover {
  color: var(--cinema);
  background: var(--paper);
  opacity: 1;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: opacity 0.2s ease, transform 0.22s var(--ease-cinema);
}
.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@media (max-width: 860px) {
  .navbar {
    top: 10px;
    padding: 0 12px;
  }
  .navbar-inner {
    gap: 10px;
    min-height: 54px;
    border-radius: 16px;
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
    font-size: 13px;
  }
}

/* Anime paper override */
.navbar-inner {
  background: rgba(255, 244, 222, 0.86);
  border-color: rgba(17, 26, 53, 0.12);
  box-shadow: 0 14px 46px rgba(17, 26, 53, 0.12);
}

.navbar.scrolled .navbar-inner {
  background: rgba(255, 244, 222, 0.94);
  border-color: rgba(17, 26, 53, 0.16);
}

.logo {
  color: var(--cinema);
}

.nav-links {
  background: rgba(17, 26, 53, 0.05);
  border-color: rgba(17, 26, 53, 0.08);
}

.nav-link {
  color: rgba(17, 26, 53, 0.7);
}

.nav-link:hover {
  color: var(--cinema);
  background: rgba(255, 122, 184, 0.12);
}

.nav-link.active {
  background: var(--cinema);
  color: var(--paper);
}

.upload-nav-btn,
.logout-btn,
.avatar-button,
.mobile-toggle {
  background: rgba(255, 244, 222, 0.78);
  border-color: rgba(17, 26, 53, 0.12);
  color: var(--cinema);
}

.upload-nav-btn {
  background: var(--cinema);
  color: var(--paper);
}

.upload-nav-btn:hover,
.logout-btn:hover,
.avatar-button:hover,
.mobile-toggle:hover {
  background: rgba(255, 122, 184, 0.16);
  border-color: rgba(255, 122, 184, 0.34);
  color: var(--cinema);
}

.upload-nav-btn:hover {
  background: var(--anime-pink);
  color: var(--paper);
}

.hamburger-line,
.hamburger-line::before,
.hamburger-line::after {
  background: var(--cinema);
}

.hamburger-line.open {
  background: transparent;
}

.mobile-drawer {
  background: rgba(255, 244, 222, 0.94);
  border-color: rgba(17, 26, 53, 0.12);
}

.mobile-nav-item {
  color: rgba(17, 26, 53, 0.7);
}

.mobile-nav-item.active,
.mobile-nav-item:hover {
  color: var(--paper);
  background: var(--cinema);
}
</style>
