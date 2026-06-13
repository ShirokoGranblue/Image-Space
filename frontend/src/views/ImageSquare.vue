<template>
  <div class="square-page cinematic-shell">
    <NavBar />
    <div class="page-container">
      
      <!-- Redesigned sq-hero -->
      <header class="sq-hero reveal">
        <h1 class="sq-hero-title">Square</h1>
        <p class="sq-hero-sub">浏览社区公开作品，用标签发现同风格创作</p>
        <div class="sq-search-bar">
          <el-input
            v-model="query.keyword"
            placeholder="输入图片名称…"
            clearable
            @clear="onFilterChange"
            @keyup.enter="onFilterChange"
            class="sq-search-el"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          
          <el-select
            v-model="categoryFilter"
            clearable
            placeholder="分类筛选"
            class="sq-category-el"
          >
            <el-option v-for="cat in categoryOptions" :key="cat" :label="cat" :value="cat" />
          </el-select>

          <select class="sq-sort" v-model="viewModeSelect">
            <option value="featured">排序：精选</option>
            <option value="latest">排序：最新</option>
            <option value="hot">排序：最热</option>
          </select>
        </div>
      </header>

      <!-- Redesigned sq-body (Split columns on desktop) -->
      <div class="sq-body">
        
        <!-- Main gallery column -->
        <main class="sq-main">
          <!-- Horizontal Sort Pills (Category tags) -->
          <div class="sq-sort-row">
            <button
              type="button"
              class="sort-pill"
              :class="{ act: !categoryFilter }"
              @click="categoryFilter = ''"
            >
              全部
            </button>
            <button
              v-for="cat in categoryOptions"
              :key="cat"
              type="button"
              class="sort-pill"
              :class="{ act: categoryFilter === cat }"
              @click="categoryFilter = cat"
            >
              {{ cat }}
            </button>
          </div>

          <div v-if="loading" class="empty-state">
            <div class="skeleton-grid">
              <div class="skeleton" v-for="n in 6" :key="n" style="aspect-ratio:1;"></div>
            </div>
          </div>

          <div v-else-if="displayedImages.length === 0" class="empty-state">
            <el-icon><PictureFilled /></el-icon>
            <p>暂时没有内容</p>
          </div>

          <div v-else class="reveal visible square-results">
            <div class="card-grid">
              <div v-for="(img, idx) in displayedImages" :key="img.id" class="stagger-item" :style="{ animationDelay: `${idx * 0.06}s` }">
                <ImageCard
                  :image="img"
                  variant="square"
                  open-mode="emit"
                  @view="openDrawer"
                  @like="handleLike"
                  @favorite="handleFavorite"
                />
              </div>
            </div>
          </div>
        </main>

        <!-- Right Sidebar (Tags and Creators) -->
        <aside class="sq-sidebar">
          <div class="ss-head">热门标签</div>
          <div class="tag-cloud" v-if="tagOptions.length">
            <span
              v-for="tag in tagOptions"
              :key="tag"
              class="sq-tag"
              :class="{ active: query.tags.includes(tag) }"
              @click="setTag(tag)"
            >
              {{ tag }}
            </span>
          </div>
          <div class="tag-cloud" v-else>
            <span class="sq-tag-empty">暂无标签</span>
          </div>

          <div class="ss-head" style="margin-top: 24px;">活跃创作者</div>
          <div class="sq-creators">
            <div
              v-for="c in activeCreators"
              :key="c.username"
              class="sq-user-row"
              @click="goCreatorProfile(c.uuidOrId)"
            >
              <div class="su-av" :style="{ background: c.avatarColor }">
                {{ (c.displayName || c.username || '?').charAt(0).toUpperCase() }}
              </div>
              <span class="su-name">{{ c.displayName || c.username }}</span>
              <span class="su-cnt">{{ c.likeCount }} 赞</span>
            </div>
          </div>
        </aside>

      </div>
    </div>

    <!-- Teleport Pagination -->
    <Teleport to="body">
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.limit"
          :page-sizes="IMAGE_PAGE_SIZES"
          :total="total"
          :disabled="loading"
          layout="total, sizes, prev, pager, next"
          @size-change="onPageSizeChange"
          @current-change="fetchList"
        />
      </div>
    </Teleport>

    <!-- Detail Drawer -->
    <transition name="drawer-slide">
      <aside class="image-drawer" v-if="drawerVisible">
        <button class="drawer-close" type="button" @click="drawerVisible = false" aria-label="关闭">
          <el-icon><Close /></el-icon>
        </button>
        <img v-if="drawerImageSrc" :src="drawerImageSrc" :alt="activeImage.imageName" class="drawer-img" />
        <div v-else class="drawer-placeholder">
          <el-icon><PictureFilled /></el-icon>
        </div>
        <div class="drawer-body">
          <h2>{{ activeImage.imageName }}</h2>
          <div class="drawer-meta">
            <span>{{ activeImage.categoryName || '未分类' }}</span>
            <span>{{ activeImage.displayName || activeImage.username || 'Unknown' }}</span>
            <span>{{ activeImage.likeCount || 0 }} 喜欢</span>
          </div>
          <p v-if="activeImage.description" class="drawer-desc">{{ activeImage.description }}</p>
          <div class="drawer-tags" v-if="activeTags.length">
            <span v-for="tag in activeTags" :key="tag">{{ tag }}</span>
          </div>
          <div class="drawer-actions">
            <el-button type="primary" @click="goDetail(activeImage)">查看</el-button>
            <el-button @click="handleLike(activeImage)">喜欢</el-button>
            <el-button @click="handleFavorite(activeImage)">收藏</el-button>
          </div>
        </div>
      </aside>
    </transition>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import ImageCard from '../components/ImageCard.vue'
import { getImageSquare, likeImage, unlikeImage } from '../api/image'
import { ElMessage } from 'element-plus'
import {
  buildSquareParams,
  createRandomSeed,
  loadSquareSession,
  saveSquareSession
} from '../utils/squareFilters'
import { DEFAULT_IMAGE_PAGE_SIZE, IMAGE_PAGE_SIZES } from '../utils/imageRequests'
import { getImageDownloadUrl } from '../utils/imageRequests'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()
const images = ref([])
const total = ref(0)
const loading = ref(false)
const categoryFilter = ref('')
const viewMode = ref('featured')
const drawerVisible = ref(false)
const activeImage = ref({})
const savedQuery = loadSquareSession()
const query = reactive({
  page: savedQuery.page || 1,
  limit: savedQuery.limit || DEFAULT_IMAGE_PAGE_SIZE,
  keyword: savedQuery.keyword || '',
  tags: savedQuery.tags || [],
  sortField: savedQuery.sortField || '',
  randomSeed: createRandomSeed()
})

const BLUE_TONES = ['#042C53', '#0C447C', '#185FA5', '#378ADD', '#85B7EB']

const categoryOptions = computed(() => {
  const categories = new Set()
  for (const image of images.value) {
    if (image.categoryName) categories.add(image.categoryName)
  }
  return Array.from(categories)
})

const tagOptions = computed(() => {
  const tags = new Set()
  for (const image of images.value) {
    String(image.tags || '')
      .split('#')
      .map(tag => tag.trim())
      .filter(Boolean)
      .forEach(tag => tags.add(tag))
  }
  return Array.from(tags).slice(0, 10)
})

const displayedImages = computed(() => {
  let list = images.value
  if (categoryFilter.value) {
    list = list.filter(image => image.categoryName === categoryFilter.value)
  }
  if (viewMode.value === 'hot') {
    list = [...list].sort((a, b) => Number(b.likeCount || 0) - Number(a.likeCount || 0))
  }
  return list
})

const activeCreators = computed(() => {
  const map = {}
  for (const img of images.value) {
    const username = img.username || 'unknown'
    const uuidOrId = img.userUuid || img.userId
    if (!map[username] && uuidOrId) {
      map[username] = {
        username,
        displayName: img.displayName || img.username,
        uuidOrId,
        likeCount: 0,
        avatarColor: BLUE_TONES[Math.floor(Math.random() * BLUE_TONES.length)]
      }
    }
    if (map[username]) {
      map[username].likeCount += Number(img.likeCount || 0)
    }
  }
  return Object.values(map).sort((a, b) => b.likeCount - a.likeCount).slice(0, 5)
})

const viewModeSelect = computed({
  get() {
    return viewMode.value
  },
  set(val) {
    setViewMode(val)
  }
})

const drawerImageSrc = computed(() => getImageDownloadUrl(activeImage.value))
const activeTags = computed(() => String(activeImage.value.tags || '').split('#').map(tag => tag.trim()).filter(Boolean))

onMounted(() => fetchList())

async function fetchList() {
  saveSquareSession(query)
  loading.value = true
  try {
    const res = await getImageSquare(buildSquareParams(query))
    images.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {} finally {
    loading.value = false
  }
}

function onFilterChange() {
  query.page = 1
  fetchList()
}

function setViewMode(mode) {
  viewMode.value = mode
  query.sortField = mode === 'featured' ? '' : 'upload_time'
  query.randomSeed = mode === 'featured' ? createRandomSeed() : query.randomSeed
  onFilterChange()
}

function setTag(tag) {
  query.tags = tag ? [tag] : []
  onFilterChange()
}

function onPageSizeChange(size) {
  query.limit = size
  query.page = 1
  fetchList()
}

function openDrawer(image) {
  activeImage.value = image
  drawerVisible.value = true
}

function goDetail(image) {
  if (!image?.uuid) return
  router.push(`/image/${image.uuid}`)
}

async function handleLike(image) {
  if (!image?.uuid) return
  if (!userStore.token) {
    ElMessage.warning('请先登录后再喜欢')
    return
  }
  try {
    const res = image.likedByMe ? await unlikeImage(image.uuid) : await likeImage(image.uuid)
    image.likeCount = res.data.likeCount
    image.likedByMe = res.data.likedByMe
    ElMessage.success(image.likedByMe ? '已喜欢' : '已取消喜欢')
  } catch {}
}

function handleFavorite() {
  ElMessage.info('当前项目未接入收藏接口')
}

function goCreatorProfile(uuidOrId) {
  if (uuidOrId) {
    router.push(`/profile/${uuidOrId}`)
  }
}
</script>

<style scoped>
.square-page {
  min-height: 100vh;
  background: var(--paper);
  display: flex;
  flex-direction: column;
}

.page-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-bottom: 112px;
}

.sq-hero {
  background: var(--ink);
  padding: 28px 24px 20px;
  border-bottom: 0.5px solid var(--ink2);
  margin-top: 56px; /* Space under fixed navbar */
}

.sq-hero-title {
  font-family: var(--font-display);
  font-size: 28px;
  color: #fff;
  font-weight: 500;
  margin-bottom: 4px;
}

.sq-hero-sub {
  font-size: 18px;
  color: var(--ink5);
  font-family: var(--font-body);
}

.sq-search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.sq-search-el {
  flex: 1;
  min-width: 200px;
}

.sq-search-el :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, .1) !important;
  border: 0.5px solid rgba(255, 255, 255, .2) !important;
  border-radius: 8px !important;
  box-shadow: none !important;
  height: 36px;
  padding: 0 12px;
  transition: background .15s, border-color .15s;
}

.sq-search-el :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, .18) !important;
  border-color: var(--ink5) !important;
}

.sq-search-el :deep(.el-input__inner) {
  color: #fff !important;
  font-size: 18px;
}

.sq-search-el :deep(.el-input__inner::placeholder) {
  color: var(--ink6) !important;
}

.sq-category-el {
  width: 160px;
}

.sq-category-el :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, .08) !important;
  border: 0.5px solid rgba(255, 255, 255, .15) !important;
  border-radius: 8px !important;
  box-shadow: none !important;
  height: 36px;
  padding: 0 10px;
}

.sq-category-el :deep(.el-input__inner) {
  color: #fff !important;
  font-size: 16px;
}

.sq-sort {
  height: 36px;
  background: rgba(255, 255, 255, .08);
  border: 0.5px solid rgba(255, 255, 255, .15);
  border-radius: 8px;
  padding: 0 10px;
  font-size: 16px;
  color: #fff;
  font-family: var(--font-body);
  outline: none;
  cursor: pointer;
  width: 140px;
}

.sq-sort option {
  background: var(--ink);
  color: #fff;
}

.sq-body {
  display: grid;
  grid-template-columns: 1fr 220px;
  flex: 1;
}

.sq-main {
  padding: 18px;
}

.sq-sort-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.sort-pill {
  font-size: 16px;
  padding: 4px 12px;
  border-radius: 20px;
  border: 0.5px solid var(--paper3);
  background: #fff;
  color: var(--ink2);
  cursor: pointer;
  font-family: var(--font-body);
  transition: all .15s;
}

.sort-pill.act {
  background: var(--ink);
  color: var(--paper);
  border-color: var(--ink);
}

.sort-pill:hover:not(.act) {
  background: #fff;
}

.sq-sidebar {
  border-left: 0.5px solid var(--paper3);
  padding: 16px 14px;
  background: #fff;
}

.ss-head {
  font-size: 15px;
  letter-spacing: .08em;
  color: var(--ink3);
  text-transform: uppercase;
  margin-bottom: 8px;
  font-weight: 600;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-bottom: 16px;
}

.sq-tag {
  font-size: 15px;
  padding: 3px 9px;
  border-radius: 12px;
  background: #fff;
  color: var(--ink2);
  border: 0.5px solid var(--paper3);
  cursor: pointer;
  transition: background .15s, border-color .15s;
}

.sq-tag:hover,
.sq-tag.active {
  background: var(--ink7);
  border-color: var(--ink4);
  color: var(--ink);
}

.sq-tag-empty {
  font-size: 16px;
  color: var(--ink5);
  font-style: italic;
}

.sq-creators {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sq-user-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 4px;
  border-radius: 7px;
  cursor: pointer;
  transition: background .15s;
}

.sq-user-row:hover {
  background: #fff;
}

.su-av {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  flex-shrink: 0;
  color: #fff;
}

.su-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.su-cnt {
  font-size: 15px;
  color: var(--ink3);
}

.pagination-wrap {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 150;
  display: flex;
  justify-content: center;
  padding: 14px 24px calc(14px + env(safe-area-inset-bottom));
  margin-top: 0;
  border-top: 0.5px solid var(--paper3);
  background: #fff;
  flex-shrink: 0;
}

.pagination-wrap :deep(.el-pagination) {
  max-width: min(100%, 1280px);
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
}

.pagination-wrap :deep(.el-pager li) {
  width: 28px;
  height: 28px;
  border-radius: 6px !important;
  border: 0.5px solid var(--paper3) !important;
  background: #fff !important;
  color: var(--ink) !important;
  font-size: 16px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background .15s;
  font-family: var(--font-body);
  min-width: auto;
}

.pagination-wrap :deep(.el-pager li.is-active) {
  background: var(--ink) !important;
  color: var(--paper) !important;
  border-color: var(--ink) !important;
}

.pagination-wrap :deep(.el-pager li:hover:not(.is-active)) {
  background: #fff !important;
}

.pagination-wrap :deep(.btn-prev),
.pagination-wrap :deep(.btn-next) {
  background: #fff !important;
  border: 0.5px solid var(--paper3) !important;
  color: var(--ink) !important;
  border-radius: 6px !important;
  height: 28px !important;
  width: 28px !important;
  min-width: auto !important;
}

.pagination-wrap :deep(.btn-prev:hover),
.pagination-wrap :deep(.btn-next:hover) {
  background: #fff !important;
}

.image-drawer {
  position: fixed;
  top: 78px;
  right: 22px;
  bottom: 22px;
  width: 380px;
  max-width: calc(100vw - 32px);
  z-index: 120;
  overflow: hidden auto;
  border: 0.5px solid var(--paper3);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 20px 60px rgba(4, 44, 83, 0.15), 0 8px 24px rgba(4, 44, 83, 0.1), 0 2px 8px rgba(4, 44, 83, 0.06);
}

.drawer-close {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  width: 34px;
  height: 34px;
  border: 0.5px solid rgba(255, 255, 255, 0.24);
  border-radius: 8px;
  background: rgba(4, 44, 83, 0.6);
  color: #fff;
  display: grid;
  place-items: center;
  cursor: pointer;
}

.drawer-img,
.drawer-placeholder {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  display: block;
  background: #fff;
}

.drawer-placeholder {
  display: grid;
  place-items: center;
  color: var(--ink3);
  font-size: 40px;
}

.drawer-body {
  padding: 20px;
}

.drawer-body h2 {
  margin: 0;
  color: var(--ink);
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 500;
  line-height: 1.25;
}

.drawer-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.drawer-meta span,
.drawer-tags span {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 4px;
  background: #fff;
  color: var(--ink3);
  font-size: 18px;
  font-weight: 500;
  border: 0.5px solid var(--paper3);
}

.drawer-desc {
  margin: 16px 0 0;
  color: var(--ink2);
  line-height: 1.8;
  font-size: 18px;
}

.drawer-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.drawer-tags span {
  background: var(--paper3);
  color: var(--ink);
}

.drawer-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform 0.24s ease, opacity 0.2s ease;
}

.drawer-slide-enter-from,
.drawer-slide-leave-to {
  transform: translateX(42px);
  opacity: 0;
}

@media (max-width: 768px) {
  .page-container { padding-bottom: 148px; }
  .sq-hero { margin-top: 56px; padding: 20px 14px; }
  .sq-hero-title { font-size: 24px; }
  .sq-search-bar { flex-direction: column; align-items: stretch; }
  .sq-search-el,
  .sq-category-el,
  .sq-sort { width: 100% !important; }
  .sq-body {
    grid-template-columns: 1fr;
  }
  .sq-sidebar {
    border-left: none;
    border-top: 0.5px solid var(--paper3);
    margin-top: 20px;
  }
  .image-drawer {
    top: 74px;
    right: 12px;
    bottom: 12px;
    max-width: calc(100vw - 24px);
  }
  .pagination-wrap {
    padding: 10px 12px calc(10px + env(safe-area-inset-bottom));
  }
  .pagination-wrap :deep(.el-pagination) {
    --el-pagination-button-width: 28px;
    --el-pagination-button-height: 28px;
    font-size: 16px;
  }
}
/* Cinematic minimal override */
.square-page {
  min-height: 100vh;
  background: transparent;
  color: var(--paper);
}

.square-page .page-container {
  width: min(100%, 1440px);
  padding: 104px 32px 126px;
}

.sq-hero {
  position: relative;
  overflow: hidden;
  min-height: 320px;
  margin-top: 0;
  padding: 44px;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  align-items: end;
  gap: 34px;
  border: 1px solid rgba(255, 253, 248, 0.16);
  border-radius: 24px;
  background:
    linear-gradient(120deg, rgba(7, 17, 31, 0.92) 0%, rgba(7, 17, 31, 0.64) 48%, rgba(7, 17, 31, 0.86) 100%),
    radial-gradient(circle at 18% 24%, rgba(55, 138, 221, 0.4), transparent 34%),
    radial-gradient(circle at 86% 12%, rgba(239, 159, 39, 0.18), transparent 30%);
  box-shadow: var(--shadow-cinematic);
  backdrop-filter: blur(18px);
}

.sq-hero::before {
  content: 'PUBLIC GALLERY';
  position: absolute;
  top: 28px;
  right: 34px;
  color: rgba(247, 243, 232, 0.12);
  font-size: clamp(54px, 9vw, 132px);
  font-weight: 800;
  letter-spacing: -0.04em;
  line-height: 0.8;
  pointer-events: none;
}

.sq-hero::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(112deg, transparent 18%, rgba(255, 253, 248, 0.08) 46%, transparent 70%);
  opacity: 0.8;
  animation: softGlow 10s var(--ease-in-out) infinite;
  pointer-events: none;
}

.sq-hero-title {
  position: relative;
  z-index: 1;
  margin: 0;
  color: var(--paper);
  font-size: clamp(56px, 8vw, 116px);
  line-height: 0.86;
  letter-spacing: -0.05em;
}

.sq-hero-title::before {
  content: 'Public Gallery';
  display: block;
  margin-bottom: 18px;
  color: var(--gold2);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

.sq-hero-sub {
  position: relative;
  z-index: 1;
  max-width: 520px;
  margin-top: 20px;
  color: rgba(247, 243, 232, 0.68);
  font-size: 17px;
  line-height: 1.7;
}

.sq-search-bar {
  position: relative;
  z-index: 1;
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 190px 160px;
  align-items: center;
  gap: 10px;
  margin-top: 28px;
  padding: 10px;
  border: 1px solid rgba(255, 253, 248, 0.12);
  border-radius: 999px;
  background: rgba(255, 253, 248, 0.08);
  backdrop-filter: blur(16px);
}

.sq-search-el,
.sq-category-el {
  min-width: 0;
}

.sq-search-el :deep(.el-input__wrapper),
.sq-category-el :deep(.el-input__wrapper) {
  height: 44px;
  background: rgba(255, 253, 248, 0.1) !important;
  border: 1px solid rgba(255, 253, 248, 0.14) !important;
  border-radius: 999px !important;
  box-shadow: none !important;
}

.sq-search-el :deep(.el-input__inner),
.sq-category-el :deep(.el-input__inner) {
  color: var(--paper) !important;
  font-size: 14px;
}

.sq-search-el :deep(.el-input__inner::placeholder),
.sq-category-el :deep(.el-input__inner::placeholder) {
  color: rgba(247, 243, 232, 0.52) !important;
}

.sq-sort {
  width: 100%;
  height: 44px;
  padding: 0 18px;
  border: 1px solid rgba(255, 253, 248, 0.14);
  border-radius: 999px;
  background: rgba(255, 253, 248, 0.1);
  color: var(--paper);
  font-size: 14px;
  font-weight: 700;
}

.sq-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 22px;
  margin-top: 24px;
}

.sq-main {
  min-width: 0;
  padding: 22px;
  border: 1px solid rgba(255, 253, 248, 0.12);
  border-radius: 22px;
  background: rgba(7, 17, 31, 0.48);
  box-shadow: var(--shadow-cinematic-soft);
  backdrop-filter: blur(18px);
}

.sq-sort-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.sort-pill {
  min-height: 34px;
  padding: 7px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 253, 248, 0.14);
  background: rgba(255, 253, 248, 0.06);
  color: rgba(247, 243, 232, 0.72);
  font-size: 13px;
  font-weight: 700;
  transition: background .16s, color .16s, border-color .16s, transform .16s var(--ease-cinema);
}

.sort-pill:hover {
  transform: translateY(-1px);
  border-color: rgba(239, 159, 39, 0.4);
}

.sort-pill.act {
  background: var(--paper);
  color: var(--cinema);
  border-color: var(--paper);
}

.square-results .card-grid {
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 18px;
}

.sq-sidebar {
  position: sticky;
  top: 96px;
  align-self: start;
  padding: 20px;
  border: 1px solid rgba(255, 253, 248, 0.12);
  border-radius: 22px;
  background: rgba(255, 253, 248, 0.1);
  color: var(--paper);
  box-shadow: var(--shadow-cinematic-soft);
  backdrop-filter: blur(18px);
}

.ss-head {
  margin-bottom: 12px;
  color: var(--gold2);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 22px;
}

.sq-tag,
.sq-tag-empty {
  min-height: 30px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 253, 248, 0.13);
  background: rgba(7, 17, 31, 0.2);
  color: rgba(247, 243, 232, 0.72);
  font-size: 12px;
  font-weight: 700;
}

.sq-tag {
  cursor: pointer;
  transition: background .16s, color .16s, border-color .16s;
}

.sq-tag:hover,
.sq-tag.active {
  background: rgba(239, 159, 39, 0.18);
  border-color: rgba(239, 159, 39, 0.42);
  color: var(--paper);
}

.sq-creators {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sq-user-row {
  width: 100%;
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border: 1px solid transparent;
  border-radius: 14px;
  background: transparent;
  color: var(--paper);
  cursor: pointer;
  transition: background .16s, border-color .16s;
}

.sq-user-row:hover {
  background: rgba(255, 253, 248, 0.08);
  border-color: rgba(255, 253, 248, 0.12);
}

.su-av {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 13px;
  font-weight: 800;
}

.su-name {
  min-width: 0;
  overflow: hidden;
  color: rgba(247, 243, 232, 0.86);
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.su-cnt {
  color: rgba(247, 243, 232, 0.48);
  font-size: 12px;
  font-weight: 800;
}

.pagination-wrap {
  border-top: 1px solid rgba(255, 253, 248, 0.12);
  background: rgba(7, 17, 31, 0.78);
  backdrop-filter: blur(18px);
}

.pagination-wrap :deep(.el-pagination) {
  color: var(--paper);
}

.pagination-wrap :deep(.el-pager li),
.pagination-wrap :deep(.btn-prev),
.pagination-wrap :deep(.btn-next) {
  background: rgba(255, 253, 248, 0.08) !important;
  border: 1px solid rgba(255, 253, 248, 0.12) !important;
  color: rgba(247, 243, 232, 0.78) !important;
}

.pagination-wrap :deep(.el-pager li.is-active) {
  background: var(--paper) !important;
  color: var(--cinema) !important;
  border-color: var(--paper) !important;
}

.image-drawer {
  top: 92px;
  right: 26px;
  bottom: 26px;
  width: 410px;
  border: 1px solid rgba(255, 253, 248, 0.16);
  border-radius: 22px;
  background: rgba(7, 17, 31, 0.82);
  color: var(--paper);
  box-shadow: var(--shadow-cinematic);
  backdrop-filter: blur(22px);
}

.drawer-close {
  border-radius: 999px;
  background: rgba(7, 17, 31, 0.64);
}

.drawer-img,
.drawer-placeholder {
  aspect-ratio: 4 / 5;
  background: var(--cinema2);
}

.drawer-body {
  padding: 24px;
}

.drawer-body h2 {
  color: var(--paper);
  font-size: 32px;
  line-height: 1.05;
  letter-spacing: -0.02em;
}

.drawer-meta span,
.drawer-tags span {
  min-height: 28px;
  border-radius: 999px;
  background: rgba(255, 253, 248, 0.08);
  border: 1px solid rgba(255, 253, 248, 0.12);
  color: rgba(247, 243, 232, 0.72);
  font-size: 12px;
  font-weight: 800;
}

.drawer-desc {
  color: rgba(247, 243, 232, 0.68);
  font-size: 15px;
}

.drawer-actions :deep(.el-button) {
  height: 38px;
}

@media (max-width: 980px) {
  .square-page .page-container {
    padding: 92px 16px 148px;
  }
  .sq-hero {
    grid-template-columns: 1fr;
    min-height: auto;
    padding: 30px 22px;
  }
  .sq-search-bar {
    grid-template-columns: 1fr;
    border-radius: 20px;
  }
  .sq-body {
    grid-template-columns: 1fr;
  }
  .sq-sidebar {
    position: static;
  }
}

@media (max-width: 560px) {
  .sq-hero-title {
    font-size: 54px;
  }
  .square-results .card-grid {
    grid-template-columns: 1fr;
  }
  .image-drawer {
    top: 78px;
    right: 12px;
    bottom: 12px;
    width: calc(100vw - 24px);
  }
}
</style>
