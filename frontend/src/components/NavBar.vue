<template>
  <header class="navbar" ref="navbarEl">
    <div class="navbar-inner">
      <div class="navbar-left">
        <router-link to="/home" class="logo" aria-label="ImageSpace 首页">IMAGESPACE</router-link>
      </div>

      <div class="navbar-right">
        <nav class="nav-links" role="navigation" aria-label="主导航">
          <router-link to="/home" class="nav-link" :class="{ active: $route.path === '/home' }">
            Images
          </router-link>
          <router-link to="/square" class="nav-link" :class="{ active: $route.path === '/square' }">
            Square
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
            Square
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
    navbarEl.value.classList.toggle('scrolled', window.scrollY > 60)
  }
}

function goProfile() {
  router.push(`/profile/${userStore.userInfo?.uuid || 0}`)
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
  top: 0; left: 0; right: 0;
  z-index: 100;
  padding: 1.2rem 2.5rem;
  border-bottom: 1px solid transparent;
  transition: all 0.4s ease;
  background: rgba(250,250,250,0);
}
.navbar.scrolled {
  background: rgba(250,250,250,0.92);
  border-color: var(--gray2);
  backdrop-filter: blur(12px);
}

.navbar-inner {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  font-family: var(--font-display);
  font-size: 1.6rem;
  letter-spacing: 0.05em;
  color: var(--black);
  text-decoration: none;
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
  gap: 2rem;
}

.nav-link {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--gray3);
  text-decoration: none;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  transition: color 0.2s;
  padding: 0;
}
.nav-link:hover,
.nav-link.active {
  color: var(--black);
}

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
  color: var(--gray4);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-btn {
  font-size: 0.75rem;
  font-weight: 500;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--gray3);
  background: none;
  border: 1px solid var(--gray2);
  padding: 0.45rem 1rem;
  cursor: pointer;
  transition: all 0.2s;
  font-family: var(--font-body);
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
  background: var(--black);
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
  top: 100%; left: 0; right: 0;
  background: var(--white);
  border-bottom: 1px solid var(--gray2);
  padding: 12px 24px 20px;
  z-index: 99;
}

.mobile-nav {
  display: flex; flex-direction: column; gap: 2px;
}
.mobile-nav-item {
  display: flex; align-items: center;
  padding: 12px;
  text-decoration: none;
  color: var(--gray4);
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 0.04em;
  transition: color 0.15s;
}
.mobile-nav-item:hover,
.mobile-nav-item.active { color: var(--black); }

.mobile-user {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 0 0; margin-top: 8px;
  border-top: 1px solid var(--gray2);
}
.mobile-user span { flex: 1; font-size: 13px; font-weight: 500; color: var(--black); }

.slide-down-enter-active,
.slide-down-leave-active { transition: all 0.2s var(--ease-out); }
.slide-down-enter-from,
.slide-down-leave-to { opacity: 0; transform: translateY(-8px); }

@media (max-width: 768px) {
  .navbar { padding: 1rem 1.5rem; }
  .nav-links { display: none; }
  .user-section { display: none; }
  .logout-btn { display: none; }
  .mobile-toggle { display: flex; align-items: center; justify-content: center; width: 32px; height: 28px; }
}

@media (max-width: 480px) {
  .navbar { padding: 0.8rem 1rem; }
  .logo { font-size: 1.3rem; }
}
</style>
