<template>
  <div class="square-page">
    <NavBar />
    <div class="page-container">
      <header class="page-header reveal">
        <div>
          <h1 class="page-title">Square</h1>
          <p class="page-desc">浏览公开作品，用轻量筛选发现同风格创作。</p>
        </div>
        <div class="square-toolbar">
          <el-input
            v-model="query.keyword"
            placeholder="输入图片名称"
            clearable
            @clear="onFilterChange"
            @keyup.enter="onFilterChange"
            class="square-search"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select
            v-model="categoryFilter"
            clearable
            placeholder="分类筛选"
            class="square-category"
          >
            <el-option v-for="cat in categoryOptions" :key="cat" :label="cat" :value="cat" />
          </el-select>
          <div class="mode-tabs" aria-label="排序方式">
            <button type="button" :class="{ active: viewMode === 'latest' }" @click="setViewMode('latest')">最新</button>
            <button type="button" :class="{ active: viewMode === 'hot' }" @click="setViewMode('hot')">最热</button>
            <button type="button" :class="{ active: viewMode === 'featured' }" @click="setViewMode('featured')">精选</button>
          </div>
        </div>
      </header>

      <div class="tag-row" v-if="tagOptions.length">
        <button
          type="button"
          class="tag-chip"
          :class="{ active: query.tags.length === 0 }"
          @click="setTag('')"
        >
          全部
        </button>
        <button
          v-for="tag in tagOptions"
          :key="tag"
          type="button"
          class="tag-chip"
          :class="{ active: query.tags.includes(tag) }"
          @click="setTag(tag)"
        >
          {{ tag }}
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

    </div>
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
</script>

<style scoped>
.square-page {
  min-height: 100vh;
  background: var(--gray1);
  display: flex;
  flex-direction: column;
}

.page-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-bottom: 112px;
}

.page-header {
  padding: 22px;
  margin-bottom: 14px;
  border: 1px solid rgba(229, 224, 212, 0.9);
  border-radius: 24px;
  background: rgba(255, 253, 248, 0.72);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}

.page-title {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 400;
  color: var(--nav-blue);
  letter-spacing: 0;
  line-height: 1.1;
  margin: 0;
}

.page-desc {
  font-size: 14px;
  color: var(--gray3);
  margin-top: 6px;
  font-weight: 300;
}

.square-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  flex: 1;
  justify-content: flex-end;
}

.square-search { width: 220px; }
.square-category { width: 150px; }

.mode-tabs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 40px;
  padding: 4px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid var(--gray2);
}

.mode-tabs button,
.tag-chip {
  border: 0;
  background: transparent;
  color: var(--gray3);
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 300;
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease, transform 0.16s ease;
}

.mode-tabs button {
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
}

.mode-tabs button:hover,
.mode-tabs button.active,
.tag-chip:hover,
.tag-chip.active {
  color: #fff;
  background: var(--nav-blue);
}

.mode-tabs button:active,
.tag-chip:active {
  transform: scale(0.98);
}

.tag-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: 0 0 18px;
}

.tag-chip {
  min-height: 34px;
  padding: 0 13px;
  border-radius: 999px;
  border: 1px solid var(--gray2);
  background: #fff;
}

.square-results,
.card-grid { flex: 1; }

.pagination-wrap {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 90;
  display: flex;
  justify-content: center;
  padding: 12px 24px calc(12px + env(safe-area-inset-bottom));
  margin-top: 0;
  border-top: 1px solid var(--gray2);
  background: rgba(245, 244, 237, 0.94);
  backdrop-filter: blur(12px);
  flex-shrink: 0;
}

.pagination-wrap :deep(.el-pagination) {
  max-width: min(100%, 1280px);
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
  padding-top: var(--space-md);
}
.skeleton-grid .skeleton {
  aspect-ratio: 1;
  border-radius: var(--radius-md);
}

.card-grid > :deep(.stagger-item) {
  animation: fadeUp 0.4s var(--ease-out) forwards;
  opacity: 0;
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
  border: 1px solid var(--gray2);
  border-radius: 24px;
  background: rgba(255, 253, 248, 0.96);
  box-shadow: 0 26px 70px rgba(15, 23, 42, 0.18);
}

.drawer-close {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 12px;
  background: rgba(3, 25, 47, 0.46);
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
  background: #e8f1fa;
}

.drawer-placeholder {
  display: grid;
  place-items: center;
  color: var(--accent);
  font-size: 40px;
}

.drawer-body {
  padding: 20px;
}

.drawer-body h2 {
  margin: 0;
  color: var(--nav-blue);
  font-size: 22px;
  font-weight: 400;
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
  border-radius: 999px;
  background: var(--blue-soft);
  color: var(--accent);
  font-size: 12px;
  font-weight: 300;
}

.drawer-desc {
  margin: 16px 0 0;
  color: var(--gray3);
  line-height: 1.8;
  font-size: 13px;
}

.drawer-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.drawer-tags span {
  background: #f2f0e8;
  color: var(--gray3);
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
  .page-header { flex-direction: column; align-items: stretch; }
  .page-title { font-size: 26px; }
  .square-toolbar { justify-content: stretch; }
  .square-search,
  .square-category,
  .mode-tabs { width: 100%; }
  .mode-tabs { justify-content: space-between; }
  .mode-tabs button { flex: 1; }
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
