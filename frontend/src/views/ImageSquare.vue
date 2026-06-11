<template>
  <div class="square-page">
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
  font-size: 24px;
  color: #fff;
  font-weight: 500;
  margin-bottom: 4px;
}

.sq-hero-sub {
  font-size: 14px;
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
  font-size: 14px;
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
  font-size: 12px;
}

.sq-sort {
  height: 36px;
  background: rgba(255, 255, 255, .08);
  border: 0.5px solid rgba(255, 255, 255, .15);
  border-radius: 8px;
  padding: 0 10px;
  font-size: 12px;
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
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 20px;
  border: 0.5px solid var(--paper3);
  background: var(--paper);
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
  background: var(--paper2);
}

.sq-sidebar {
  border-left: 0.5px solid var(--paper3);
  padding: 16px 14px;
  background: var(--paper);
}

.ss-head {
  font-size: 11px;
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
  font-size: 11px;
  padding: 3px 9px;
  border-radius: 12px;
  background: var(--paper2);
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
  font-size: 12px;
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
  background: var(--paper2);
}

.su-av {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
  color: #fff;
}

.su-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--ink);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.su-cnt {
  font-size: 11px;
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
  background: var(--paper2);
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
  background: var(--paper) !important;
  color: var(--ink) !important;
  font-size: 12px;
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
  background: var(--paper) !important;
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
  background: var(--paper);
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
  background: var(--paper2);
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
  font-size: 24px;
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
  background: var(--paper2);
  color: var(--ink3);
  font-size: 14px;
  font-weight: 500;
  border: 0.5px solid var(--paper3);
}

.drawer-desc {
  margin: 16px 0 0;
  color: var(--ink2);
  line-height: 1.8;
  font-size: 14px;
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
  .sq-hero-title { font-size: 20px; }
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
    font-size: 12px;
  }
}
</style>
