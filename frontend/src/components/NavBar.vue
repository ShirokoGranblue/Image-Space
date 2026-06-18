<template>
  <header class="navbar" ref="navbarEl">
    <div class="navbar-inner">
      <router-link to="/home" class="logo" aria-label="图像空间首页">
        <span class="logo-mark-frame" aria-hidden="true">
          <img class="logo-mark" :src="logoIcon" alt="" />
        </span>
        <span class="logo-copy">
          <strong>图像空间</strong>
          <small>私人图库</small>
        </span>
      </router-link>

      <nav class="nav-links" role="navigation" aria-label="主导航">
        <router-link to="/square" class="nav-link" :class="{ active: $route.path === '/square' }">
          广场
        </router-link>
        <router-link to="/home" class="nav-link" :class="{ active: $route.path === '/home' }">
          图片
        </router-link>
        <router-link v-if="token" :to="profilePath" class="nav-link" :class="{ active: $route.path.startsWith('/profile') }">
          我的
        </router-link>
      </nav>

      <div class="navbar-right">
        <button v-if="token" class="upload-nav-btn" type="button" @click="goUpload">
          <el-icon><Plus /></el-icon>
          <span>上传</span>
        </button>

        <div class="user-section" v-if="token">
          <NotificationBell :active="!!token" />
          <button class="avatar-button" type="button" @click="goProfile" :title="userInfo?.displayName || userInfo?.username || '个人主页'">
            <el-avatar :size="30" :src="userInfo?.avatarUrl || userInfo?.avatar" class="nav-avatar">
              {{ initials }}
            </el-avatar>
          </button>
          <button class="logout-btn" type="button" @click="handleLogout">退出</button>
        </div>

        <button class="mobile-toggle" @click="mobileOpen = !mobileOpen" :aria-expanded="String(mobileOpen)" aria-label="菜单">
          <span class="hamburger-line" :class="{ open: mobileOpen }"></span>
        </button>
      </div>
    </div>

    <transition name="slide-down">
      <div class="mobile-drawer" v-if="mobileOpen">
        <nav class="mobile-nav">
          <router-link to="/square" class="mobile-nav-item" :class="{ active: $route.path === '/square' }" @click="mobileOpen = false">
            广场
          </router-link>
          <router-link to="/home" class="mobile-nav-item" :class="{ active: $route.path === '/home' }" @click="mobileOpen = false">
            图片
          </router-link>
          <router-link v-if="token" :to="profilePath" class="mobile-nav-item" :class="{ active: $route.path.startsWith('/profile') }" @click="mobileOpen = false">
            我的
          </router-link>
        </nav>
        <div class="mobile-actions" v-if="token">
          <button class="upload-nav-btn" type="button" @click="goUpload">
            <el-icon><Plus /></el-icon>
            <span>上传</span>
          </button>
          <NotificationBell :active="!!token" />
          <button class="logout-btn" type="button" @click="handleLogout">退出</button>
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
import logoIcon from '../logo/60060cf8-de2e-4faf-8818-668b5132988b.svg'

const router = useRouter()
const userStore = useUserStore()

const token = computed(() => userStore.token)
const userInfo = computed(() => userStore.userInfo)
const profilePath = computed(() => `/profile/${userInfo.value?.uuid || 0}`)
const initials = computed(() => {
  const source = userInfo.value?.displayName || userInfo.value?.username || '图像'
  return String(source).slice(0, 2).toUpperCase()
})
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
    navbarEl.value.classList.toggle('scrolled', window.scrollY > 24)
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
  padding: 0 18px;
  pointer-events: none;
}

.navbar-inner {
  width: min(100%, 1560px);
  min-height: 60px;
  margin: 0 auto;
  padding: 10px 12px 10px 16px;
  display: grid;
  grid-template-columns: auto minmax(220px, 1fr) auto;
  align-items: center;
  gap: 18px;
  pointer-events: auto;
  background: rgba(13, 16, 22, 0.9);
  border: 1px solid var(--ad-line);
  border-radius: 16px;
  box-shadow: 0 18px 52px rgba(0, 0, 0, 0.26);
  backdrop-filter: blur(18px);
  transition: min-height 0.18s var(--ad-ease), background 0.18s ease, border-color 0.18s ease;
}

.navbar.scrolled .navbar-inner {
  min-height: 54px;
  background: rgba(13, 16, 22, 0.96);
  border-color: var(--ad-line-strong);
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
  color: var(--ad-text);
  text-decoration: none;
}

.logo:hover {
  opacity: 1;
}

.logo-mark-frame {
  position: relative;
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border: 1px solid var(--ad-line-strong);
  border-radius: 11px;
  background: var(--ad-surface);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

.logo-mark-frame::before,
.logo-mark-frame::after {
  content: '';
  position: absolute;
  border-radius: 999px;
}

.logo-mark-frame::before {
  inset: 8px;
  border: 2px solid var(--ad-green);
  box-shadow: 0 0 18px rgba(183, 255, 60, 0.22);
}

.logo-mark-frame::after {
  width: 8px;
  height: 8px;
  top: 15px;
  left: 15px;
  background: var(--ad-cyan);
}

.logo-mark {
  position: relative;
  z-index: 1;
  width: 26px;
  height: 26px;
  object-fit: contain;
  opacity: 0.01;
}

.logo-copy {
  display: grid;
  min-width: 0;
  gap: 1px;
}

.logo-copy strong {
  color: var(--ad-text);
  font-size: 15px;
  font-weight: 820;
  letter-spacing: 0.14em;
  white-space: nowrap;
}

.logo-copy small {
  color: var(--ad-muted);
  font-size: 11px;
  font-weight: 560;
  line-height: 1;
  white-space: nowrap;
}

.nav-links {
  justify-self: center;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px;
  border: 1px solid var(--ad-line);
  border-radius: 999px;
  background: rgba(244, 241, 232, 0.04);
}

.nav-link {
  min-height: 34px;
  padding: 7px 15px;
  border-radius: 999px;
  color: var(--ad-muted);
  font-size: 14px;
  font-weight: 720;
  line-height: 1;
  text-decoration: none;
  white-space: nowrap;
  transition: background .16s ease, color .16s ease, transform .16s var(--ad-ease);
}

.nav-link:hover {
  color: var(--ad-text);
  background: rgba(244, 241, 232, 0.08);
  opacity: 1;
  transform: translateY(-1px);
}

.nav-link.active {
  background: var(--ad-green);
  color: #071014;
}

.navbar-right,
.user-section {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.upload-nav-btn,
.logout-btn,
.avatar-button {
  height: 40px;
  border: 1px solid var(--ad-line);
  border-radius: 10px;
  background: rgba(21, 25, 34, 0.92);
  color: var(--ad-text-soft);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 13px;
  font-family: var(--ad-font);
  font-size: 13px;
  font-weight: 820;
  cursor: pointer;
  transition: transform 0.16s var(--ad-ease), background 0.16s ease, border-color 0.16s ease, color 0.16s ease;
}

.upload-nav-btn {
  background: var(--ad-green);
  border-color: var(--ad-green);
  color: #071014;
}

.upload-nav-btn:hover {
  background: var(--ad-green-2);
  border-color: var(--ad-green-2);
  transform: translateY(-1px);
}

.logout-btn:hover,
.avatar-button:hover {
  background: var(--ad-surface-2);
  color: var(--ad-text);
  border-color: var(--ad-line-strong);
  transform: translateY(-1px);
}

.avatar-button {
  width: 40px;
  padding: 0;
  overflow: hidden;
}

.nav-avatar {
  border: 0;
  background: linear-gradient(135deg, var(--ad-cyan), var(--ad-violet));
  color: #071014;
  font-size: 12px;
  font-weight: 820;
}

.navbar :deep(.notification-bell) {
  color: var(--ad-text-soft);
  border-radius: 10px;
}

.navbar :deep(.notification-bell:hover) {
  color: var(--ad-text);
  background: var(--ad-surface-2);
}

.mobile-toggle {
  display: none;
  width: 40px;
  height: 40px;
  border: 1px solid var(--ad-line);
  border-radius: 10px;
  background: rgba(21, 25, 34, 0.92);
  cursor: pointer;
  padding: 0;
}

.hamburger-line,
.hamburger-line::before,
.hamburger-line::after {
  display: block;
  width: 18px;
  height: 2px;
  background: var(--ad-text);
  border-radius: 999px;
  transition: all 0.22s var(--ad-ease);
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
  width: min(100%, 1560px);
  margin: 10px auto 0;
  padding: 12px;
  pointer-events: auto;
  background: rgba(13, 16, 22, 0.96);
  border: 1px solid var(--ad-line);
  border-radius: 16px;
  box-shadow: var(--ad-shadow);
  backdrop-filter: blur(18px);
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
  border-top: 1px solid var(--ad-line);
}

.mobile-nav-item {
  flex: 1 1 110px;
  min-height: 40px;
  padding: 11px 12px;
  color: var(--ad-muted);
  border-radius: 10px;
  text-align: center;
  font-size: 15px;
  font-weight: 720;
}

.mobile-nav-item.active,
.mobile-nav-item:hover {
  color: #071014;
  background: var(--ad-green);
  opacity: 1;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: opacity 0.2s ease, transform 0.22s var(--ad-ease);
}
.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@media (max-width: 900px) {
  .navbar {
    top: 10px;
    padding: 0 10px;
  }
  .navbar-inner {
    grid-template-columns: auto auto;
    gap: 10px;
  }
  .nav-links,
  .user-section,
  .navbar-right > .upload-nav-btn {
    display: none;
  }
  .navbar-right {
    justify-self: end;
  }
  .mobile-toggle {
    display: grid;
    place-items: center;
  }
}

@media (max-width: 500px) {
  .logo-copy strong {
    font-size: 13px;
  }
  .logo-copy small {
    display: none;
  }
}
</style>
