<template>
  <header class="navbar" ref="navbarEl">
    <div class="navbar-inner">
      <router-link to="/home" class="logo" aria-label="AstralSpace 首页">
        <strong class="logo-wordmark">AstralSpace</strong>
      </router-link>

      <nav class="nav-links" role="navigation" aria-label="主导航">
        <router-link to="/square" class="nav-link" :class="{ active: $route.path === '/square' }">
          Explore
        </router-link>
        <router-link to="/home" class="nav-link" :class="{ active: $route.path === '/home' }">
          Images
        </router-link>
        <router-link v-if="token" :to="profilePath" class="nav-link" :class="{ active: $route.path.startsWith('/profile') }">
          Profile
        </router-link>
        <router-link v-if="showAuditEntry" to="/admin/audit-log" class="nav-link" :class="{ active: $route.path.startsWith('/admin/audit-log') }">
          Audit
        </router-link>
      </nav>

      <div class="navbar-right">
        <button v-if="token" class="upload-nav-btn" type="button" @click="goUpload">
          <el-icon><Plus /></el-icon>
          <span>上传</span>
        </button>

        <div class="user-section" v-if="token">
          <NotificationBell :active="!!token" />
          <button class="avatar-button" type="button" @click="goProfile" :title="avatarTitle">
            <el-avatar :size="30" :src="userInfo?.avatarUrl || userInfo?.avatar" class="nav-avatar">
              {{ initials }}
            </el-avatar>
          </button>
          <button class="logout-btn" type="button" @click="handleLogout">Exit</button>
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
            Explore
          </router-link>
          <router-link to="/home" class="mobile-nav-item" :class="{ active: $route.path === '/home' }" @click="mobileOpen = false">
            Images
          </router-link>
          <router-link v-if="token" :to="profilePath" class="mobile-nav-item" :class="{ active: $route.path.startsWith('/profile') }" @click="mobileOpen = false">
            Profile
          </router-link>
          <router-link v-if="showAuditEntry" to="/admin/audit-log" class="mobile-nav-item" :class="{ active: $route.path.startsWith('/admin/audit-log') }" @click="mobileOpen = false">
            Audit
          </router-link>
        </nav>
        <div class="mobile-actions" v-if="token">
          <button class="upload-nav-btn" type="button" @click="goUpload">
            <el-icon><Plus /></el-icon>
            <span>上传</span>
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
import { Plus } from '@element-plus/icons-vue'
import NotificationBell from './NotificationBell.vue'
import { isAllowedAdminDomain } from '../utils/adminDomain'
import { formatUserIdentityText } from '../utils/userIdentity'

const router = useRouter()
const userStore = useUserStore()

const token = computed(() => userStore.token)
const userInfo = computed(() => userStore.userInfo)
const profilePath = computed(() => `/profile/${userInfo.value?.uuid || 0}`)
const showAuditEntry = computed(() => Boolean(token.value && userInfo.value?.role === 'admin' && isAllowedAdminDomain()))
const avatarTitle = computed(() => userInfo.value ? formatUserIdentityText(userInfo.value, '个人主页') : '个人主页')
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
    const scrollRange = Math.max(document.documentElement.scrollHeight - window.innerHeight, 1)
    const scrollProgress = Math.min(window.scrollY / scrollRange, 1)
    navbarEl.value.style.setProperty('--astral-brand-position', `${Math.round(scrollProgress * 100)}%`)
  }
}

function goProfile() {
  mobileOpen.value = false
  router.push(profilePath.value)
}

function goUpload() {
  mobileOpen.value = false
  if (router.currentRoute.value.path === '/home') {
    window.dispatchEvent(new CustomEvent('image-space:open-upload'))
    return
  }
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
  --astral-brand-position: 0%;

  position: fixed;
  top: 0;
  right: 0;
  left: 0;
  z-index: var(--layer-nav);
  pointer-events: none;
  border-bottom: 1px solid var(--color-border-subtle);
  background: rgba(14, 16, 23, 0.88);
  backdrop-filter: blur(14px);
}

.navbar-inner {
  display: grid;
  width: min(100%, var(--page-wide));
  min-height: var(--nav-height);
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: var(--space-6);
  margin: 0 auto;
  padding: 0 var(--page-gutter);
  pointer-events: auto;
  background: transparent;
}

.logo {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  justify-self: start;
  color: var(--color-text-primary);
  text-decoration: none;
}

.logo-wordmark {
  background-image: linear-gradient(
    110deg,
    var(--astral-gold) 0%,
    var(--astral-rose) 24%,
    var(--astral-plum) 45%,
    var(--astral-teal) 66%,
    var(--astral-starlight) 82%,
    var(--astral-gold) 100%
  );
  background-position: var(--astral-brand-position) 50%;
  background-size: 240% 100%;
  background-clip: text;
  color: transparent;
  font-family: var(--font-title);
  font-size: 1.125rem;
  font-weight: 500;
  letter-spacing: -.025em;
  white-space: nowrap;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.nav-links { display: inline-flex; justify-self: center; align-items: center; gap: clamp(var(--space-6), 5vw, var(--space-8)); }

.nav-link {
  min-width: var(--control-height-md);
  min-height: var(--control-height-md);
  padding: 10px 2px 8px;
  border-bottom: 2px solid transparent;
  color: var(--color-text-secondary);
  font-family: var(--font-ui);
  font-size: var(--text-sm);
  font-weight: 600;
  line-height: 1;
  text-decoration: none;
  white-space: nowrap;
  transition: border-color var(--duration-fast) var(--ease-standard), color var(--duration-fast) var(--ease-standard);
}

.nav-link:hover { color: var(--color-text-primary); }
.nav-link.active { border-color: var(--color-vermilion); color: var(--color-text-primary); }

.navbar-right,
.user-section,
.mobile-actions {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.navbar-right { justify-self: end; }

.upload-nav-btn,
.logout-btn,
.avatar-button,
.mobile-toggle {
  display: inline-flex;
  height: var(--control-height-md);
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: 0 var(--space-3);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm);
  background: var(--color-surface-1);
  color: var(--color-text-primary);
  font-family: var(--font-ui);
  cursor: pointer;
}

.upload-nav-btn { border-color: var(--color-vermilion); background: var(--color-vermilion); color: var(--color-text-inverse); }
.upload-nav-btn:hover { border-color: var(--color-vermilion-hover); background: var(--color-vermilion-hover); }
.logout-btn:hover,.logout-btn:focus-visible { border-color: var(--color-error); background: var(--color-error); color: var(--color-text-inverse); outline: none; }
.avatar-button:hover,.mobile-toggle:hover { border-color: var(--color-border-strong); background: var(--color-surface-2); }
.avatar-button { width: var(--control-height-md); padding: 0; overflow: hidden; border-color: transparent; border-radius: 50%; background: transparent; }
.avatar-button:focus-visible { outline: 2px solid var(--color-urban); outline-offset: 2px; }
.nav-avatar { border: 0; background: var(--color-night); color: var(--color-text-inverse); }
.navbar :deep(.notification-bell) { width: var(--control-height-md); height: var(--control-height-md); border-radius: var(--radius-sm); color: var(--color-text-secondary); }
.navbar :deep(.notification-bell:hover) { background: var(--color-surface-2); color: var(--color-text-primary); }

.mobile-toggle { display: none; width: var(--control-height-md); padding: 0; }

.hamburger-line,
.hamburger-line::before,
.hamburger-line::after {
  display: block;
  width: 18px;
  height: 2px;
  background: var(--color-text-primary);
  content: '';
  transition: transform var(--duration-standard) var(--ease-standard), background-color var(--duration-standard) var(--ease-standard);
}

.hamburger-line { position: relative; }
.hamburger-line::before,.hamburger-line::after { position: absolute; left: 0; }
.hamburger-line::before { top: -6px; }
.hamburger-line::after { top: 6px; }
.hamburger-line.open { background: transparent; }
.hamburger-line.open::before { top: 0; transform: rotate(45deg); }
.hamburger-line.open::after { top: 0; transform: rotate(-45deg); }

.mobile-drawer {
  width: 100%;
  margin: 0;
  padding: var(--space-3) var(--page-gutter) var(--space-4);
  pointer-events: auto;
  border-top: 1px solid var(--color-border-subtle);
  border-bottom: 1px solid var(--color-border-subtle);
  background: var(--color-canvas);
  box-shadow: var(--shadow-float);
}

.mobile-nav { display: flex; align-items: center; gap: var(--space-2); flex-wrap: wrap; }
.mobile-actions { margin-top: var(--space-3); padding-top: var(--space-3); border-top: 1px solid var(--color-border-subtle); }
.mobile-nav-item { flex: 1 1 110px; min-height: var(--control-height-md); padding: 11px 12px; border-radius: var(--radius-sm); color: var(--color-text-secondary); font-family: var(--font-ui); text-align: center; text-decoration: none; }
.mobile-nav-item.active,.mobile-nav-item:hover { background: var(--color-surface-2); color: var(--color-text-primary); }

.slide-down-enter-active,.slide-down-leave-active { transition: opacity var(--duration-standard) var(--ease-standard), transform var(--duration-standard) var(--ease-standard); }
.slide-down-enter-from,.slide-down-leave-to { opacity: 0; transform: translateY(-10px); }

@media (max-width: 900px) {
  .navbar-inner { grid-template-columns: auto auto; min-height: 64px; }
  .nav-links,.user-section,.navbar-right > .upload-nav-btn { display: none; }
  .navbar-right { justify-self: end; }
  .mobile-toggle { display: inline-flex; width: var(--control-height-lg); height: var(--control-height-lg); }
}

@media (max-width: 500px) {
  .logo-wordmark { font-size: var(--text-md); }
  .mobile-nav-item { min-height: var(--control-height-lg); }
  .mobile-actions .upload-nav-btn,.mobile-actions .logout-btn { min-height: var(--control-height-lg); height: var(--control-height-lg); }
}

@media (prefers-reduced-motion: reduce) {
  .hamburger-line,.hamburger-line::before,.hamburger-line::after { transition: none; }
  .slide-down-enter-active,.slide-down-leave-active { transition: opacity 200ms ease; }
  .slide-down-enter-from,.slide-down-leave-to { opacity: 0; transform: none; }
}

@media (forced-colors: active) {
  .navbar {
    border-color: CanvasText;
    background: Canvas;
    backdrop-filter: none;
  }

  .logo-wordmark {
    background-image: none;
    color: CanvasText;
    -webkit-text-fill-color: CanvasText;
  }
}
</style>
