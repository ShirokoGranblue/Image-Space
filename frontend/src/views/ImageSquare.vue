<template>
  <div class="public-square-page">
    <NavBar />

    <div class="square-shell">
      <SquareHeroFilters
        :total="total"
        :keyword="query.keyword"
        :category-id="categoryFilter"
        :category-options="categoryOptions"
        :tag-options="tagOptions"
        :view-mode="viewMode"
        :sort-options="sortOptions"
        :active-tag="query.tags[0] || ''"
        :active-category-label="activeCategoryLabel"
        :active-sort-label="activeSortLabel"
        :has-active-filters="hasActiveFilters"
        @update:keyword="query.keyword = $event"
        @search="onFilterChange"
        @select-category="setCategory"
        @select-tag="setTag"
        @select-sort="setViewMode"
        @clear-keyword="clearKeyword"
        @clear-tag="clearTag"
        @clear-all="clearFilters"
      />

      <div class="square-layout">
        <main class="square-main">
          <div class="category-strip" v-if="categoryOptions.length" aria-label="本页分类">
            <span class="category-strip__label">本页分类</span>
            <button
              type="button"
              class="category-chip"
              :class="{ active: !categoryFilter }"
              @click="setCategory(null)"
            >
              全部
            </button>
            <button
              v-for="cat in categoryOptions"
              :key="cat.id"
              type="button"
              class="category-chip"
              :class="{ active: categoryFilter === cat.id }"
              @click="setCategory(cat.id)"
            >
              {{ cat.name }}
            </button>
          </div>

          <div class="result-head" aria-live="polite">
            <div>
              <span>当前显示</span>
              <strong>{{ squareImages.length }}</strong>
              <span>张图片</span>
            </div>
          </div>

          <GalleryGrid
            :items="squareImages"
            :loading="loading"
            :error="loadError"
            :loading-count="9"
            :initial-eager-count="4"
            density="compact"
            variant="square"
            open-mode="emit"
            empty-title="没有符合条件的公开图片"
            empty-description="可以清空筛选条件，或稍后再来看看。"
            error-description="公开图片请求失败，请检查连接后重试。"
            @retry="fetchList"
          >
            <template #item="{ item, priority }">
              <ImageCard
                :image="item"
                :priority="priority"
                variant="square"
                open-mode="emit"
                @view="openDrawer"
                @like="handleLike"
              />
            </template>
          </GalleryGrid>
        </main>

        <SquareDiscoveryAside
          :active-sort-label="activeSortLabel"
          :active-category-label="activeCategoryLabel"
          :active-tag="query.tags[0] || ''"
          :keyword="query.keyword"
          :tag-options="tagOptions"
          :active-creators="activeCreators"
          :featured-image="featuredImage"
          :featured-image-src="featuredImageSrc"
          @select-tag="setTag"
          @select-creator="goCreatorProfile"
          @view-image="openDrawer"
        />
      </div>

      <Teleport to="body">
        <div class="pagination-wrap" v-if="total > 0">
          <el-pagination
            v-model:current-page="query.page"
            :page-size="query.limit"
            :page-sizes="IMAGE_PAGE_SIZES"
            :pager-count="5"
            :total="total"
            :disabled="loading"
            layout="total, sizes, prev, pager, next"
            @size-change="onPageSizeChange"
            @current-change="fetchList"
          />
        </div>
      </Teleport>
    </div>

    <SquareImageDrawer :visible="drawerVisible" :image="activeImage" :image-src="drawerImageSrc" :tags="activeTags" @close="closeDrawer" @view="openViewer" @detail="goDetail" @like="handleLike" @select-tag="setTag" />
    <ImageViewer ref="viewerRef" :items="squareImages" />
  </div>
</template>

<script setup>
import { computed, reactive, ref, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import ImageCard from '../components/ImageCard.vue'
import GalleryGrid from '../components/gallery/GalleryGrid.vue'
import ImageViewer from '../components/ImageViewer.vue'
import SquareHeroFilters from '../components/square/SquareHeroFilters.vue'
import SquareDiscoveryAside from '../components/square/SquareDiscoveryAside.vue'
import SquareImageDrawer from '../components/square/SquareImageDrawer.vue'
import { getImageSquare, likeImage, unlikeImage } from '../api/image'
import {
  buildSquareParams,
  createRandomSeed,
  loadSquareSession,
  saveSquareSession
} from '../utils/squareFilters'
import { fireSmallSideCannons } from '../utils/confettiEffect'
import { DEFAULT_IMAGE_PAGE_SIZE, IMAGE_PAGE_SIZES, getImageDisplayUrl, getImagePreviewUrl } from '../utils/imageRequests'
import { useUserStore } from '../store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const savedQuery = loadSquareSession()
const images = ref([])
const total = ref(0)
const loading = ref(false)
const loadError = ref(false)
const categoryFilter = ref(savedQuery.categoryId || null)
const viewMode = ref('featured')
const drawerVisible = ref(false)
const activeImage = ref(null)
const drawerTrigger = ref(null)
const viewerRef = ref(null)

const query = reactive({
  page: savedQuery.page || 1,
  limit: savedQuery.limit || DEFAULT_IMAGE_PAGE_SIZE,
  keyword: savedQuery.keyword || '',
  categoryId: savedQuery.categoryId || null,
  tags: savedQuery.tags || [],
  sortField: savedQuery.sortField || '',
  randomSeed: createRandomSeed()
})

const sortOptions = [
  { label: '推荐', value: 'featured' },
  { label: '最新', value: 'latest' }
]
const CREATOR_TONES = ['#38d5ff', '#b7ff3c', '#f5b84b', '#9b8cff', '#ff6b57']
const demoImages = [
  {
    id: 'demo-1',
    uuid: 'demo-image-hover-1',
    imageName: '山色试映.jpg',
    visibility: 'PUBLIC',
    categoryName: '风景',
    username: 'demo',
    displayName: 'Demo',
    userId: 'demo-user',
    userUuid: 'demo-user-1',
    imageUrl: 'https://picsum.photos/seed/image-space-1/900/1100',
    mediumUrl: 'https://picsum.photos/seed/image-space-1/900/1100',
    thumbUrl: 'https://picsum.photos/seed/image-space-1/640/800',
    tags: '#自然#预览',
    likeCount: 12
  },
  {
    id: 'demo-2',
    uuid: 'demo-image-hover-2',
    imageName: '城市霓虹.png',
    visibility: 'SPECIFIED',
    categoryName: '夜景',
    username: 'demo',
    displayName: 'Demo',
    userId: 'demo-user',
    userUuid: 'demo-user-1',
    imageUrl: 'https://picsum.photos/seed/image-space-2/900/1100',
    mediumUrl: 'https://picsum.photos/seed/image-space-2/900/1100',
    thumbUrl: 'https://picsum.photos/seed/image-space-2/640/800',
    tags: '#城市#霓虹',
    likeCount: 28
  },
  {
    id: 'demo-3',
    uuid: 'demo-image-hover-3',
    imageName: '静物练习.webp',
    visibility: 'PRIVATE',
    categoryName: '创作',
    username: 'demo',
    displayName: 'Demo',
    userId: 'demo-user',
    userUuid: 'demo-user-1',
    imageUrl: 'https://picsum.photos/seed/image-space-3/900/1100',
    mediumUrl: 'https://picsum.photos/seed/image-space-3/900/1100',
    thumbUrl: 'https://picsum.photos/seed/image-space-3/640/800',
    tags: '#静物#测试',
    likeCount: 7
  }
]
const isDemoCards = computed(() => import.meta.env.DEV && route.query.demoCards === '1')
const squareImages = computed(() => isDemoCards.value ? demoImages : images.value)

const categoryOptions = computed(() => {
  const categories = new Map()
  for (const image of squareImages.value) {
    if (!image.categoryName) continue
    const categoryId = image.categoryId != null ? Number(image.categoryId) : image.categoryName
    categories.set(categoryId, image.categoryName)
  }
  return Array.from(categories, ([id, name]) => ({ id, name }))
})

const tagOptions = computed(() => {
  const tags = new Set()
  for (const image of squareImages.value) {
    String(image.tags || '')
      .split('#')
      .map(tag => tag.trim())
      .filter(Boolean)
      .forEach(tag => tags.add(tag))
  }
  return Array.from(tags).slice(0, 14)
})

const displayedImages = computed(() => {
  let list = squareImages.value
  if (categoryFilter.value) {
    list = list.filter(image => {
      const categoryId = image.categoryId != null ? Number(image.categoryId) : image.categoryName
      return categoryId === categoryFilter.value || image.categoryName === categoryFilter.value
    })
  }
  return list
})
const activeCreators = computed(() => {
  const map = {}
  for (const img of squareImages.value) {
    const username = img.username || 'unknown'
    const uuidOrId = img.userUuid || img.userId
    if (!map[username] && uuidOrId) {
      const toneIndex = Math.abs(hashString(username)) % CREATOR_TONES.length
      const name = img.displayName || img.username || '?'
      map[username] = {
        username,
        displayName: img.displayName || img.username,
        uuidOrId,
        likeCount: 0,
        avatarColor: CREATOR_TONES[toneIndex],
        initial: String(name).charAt(0).toUpperCase()
      }
    }
    if (map[username]) {
      map[username].likeCount += Number(img.likeCount || 0)
    }
  }
  return Object.values(map).sort((a, b) => b.likeCount - a.likeCount).slice(0, 5)
})

const squareStats = computed(() => ({
  totalLikes: squareImages.value.reduce((sum, image) => sum + Number(image.likeCount || 0), 0)
}))

const activeSortLabel = computed(() => sortOptions.find(option => option.value === viewMode.value)?.label || '推荐')
const activeCategoryLabel = computed(() => categoryOptions.value.find(option => option.id === categoryFilter.value)?.name || '不限')
const hasActiveFilters = computed(() => Boolean(query.keyword || categoryFilter.value || query.tags.length || viewMode.value !== 'featured'))
const featuredImage = computed(() => displayedImages.value[0] || squareImages.value[0] || null)
const featuredImageSrc = computed(() => featuredImage.value ? getImageDisplayUrl(featuredImage.value) : '')
const drawerImageSrc = computed(() => activeImage.value ? getImagePreviewUrl(activeImage.value) : '')
const activeTags = computed(() => String(activeImage.value?.tags || '').split('#').map(tag => tag.trim()).filter(Boolean))

onMounted(() => fetchList())

async function fetchList() {
  saveSquareSession(query)
  if (isDemoCards.value) {
    total.value = demoImages.length
    loading.value = false
    return
  }
  loading.value = true
  loadError.value = false
  try {
    const res = await getImageSquare(buildSquareParams(query))
    images.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    loadError.value = true
  } finally {
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
  const normalizedTag = String(tag || '').trim()
  query.tags = normalizedTag ? [normalizedTag] : []
  onFilterChange()
}

function setCategory(categoryId) {
  categoryFilter.value = categoryId || null
  query.categoryId = categoryFilter.value
  onFilterChange()
}

function clearKeyword() {
  query.keyword = ''
  onFilterChange()
}

function clearTag() {
  query.tags = []
  onFilterChange()
}

function clearFilters() {
  categoryFilter.value = null
  query.categoryId = null
  query.keyword = ''
  query.tags = []
  viewMode.value = 'featured'
  query.sortField = ''
  query.randomSeed = createRandomSeed()
  onFilterChange()
}

function onPageSizeChange(size) {
  query.limit = size
  query.page = 1
  fetchList()
}

function openDrawer(image) {
  const trigger = document.activeElement
  drawerTrigger.value = trigger instanceof HTMLElement ? trigger : null
  activeImage.value = image
  drawerVisible.value = true
}

function closeDrawer() {
  drawerVisible.value = false
  const trigger = drawerTrigger.value
  drawerTrigger.value = null
  nextTick(() => {
    if (trigger?.isConnected) trigger.focus()
  })
}

function openViewer(image) {
  const index = Math.max(0, images.value.findIndex(item => item.uuid === image?.uuid))
  viewerRef.value?.open({ index, trigger: document.activeElement })
}

function goDetail(image) {
  if (!image?.uuid) return
  router.push(`/image/${image.uuid}`)
}

async function handleLike(image) {
  if (!image?.uuid) return
  if (!userStore.token) {
    ElMessage.warning('请先登录后再进行操作')
    return
  }
  try {
    const shouldCelebrate = !image.likedByMe
    const res = image.likedByMe ? await unlikeImage(image.uuid) : await likeImage(image.uuid)
    image.likeCount = res.data.likeCount
    image.likedByMe = res.data.likedByMe
    ElMessage.success(image.likedByMe ? '已喜欢' : '已取消喜欢')
    if (shouldCelebrate && image.likedByMe) fireSmallSideCannons()
  } catch {}
}

function goCreatorProfile(uuidOrId) {
  if (uuidOrId) {
    router.push(`/profile/${uuidOrId}`)
  }
}

function hashString(value) {
  return String(value).split('').reduce((hash, char) => ((hash << 5) - hash) + char.charCodeAt(0), 0)
}
</script>

<style scoped>
.public-square-page { min-height: 100vh; background: var(--color-canvas); color: var(--color-text-primary); }
.square-shell { width: min(calc(100% - (2 * var(--page-gutter))), var(--page-wide)); margin-inline: auto; padding: calc(var(--nav-height) + var(--space-5)) 0 112px; }
.square-layout { display: grid; grid-template-columns: minmax(0, 1fr) var(--panel-aside-width); gap: var(--space-6); align-items: start; margin-top: var(--space-5); }
.square-main { min-width: 0; }
.category-strip { display: flex; flex-wrap: wrap; gap: var(--space-2); margin-bottom: var(--space-5); }
.category-strip__label { align-self: center; margin-right: var(--space-1); color: var(--color-text-muted); font-size: var(--text-xs); white-space: nowrap; }
.category-chip { min-height: 40px; padding: 0 var(--space-3); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-round); background: var(--color-surface-1); color: var(--color-text-secondary); font-family: var(--font-ui); cursor: pointer; }
.category-chip.active,.category-chip:hover { border-color: var(--color-border-strong); background: var(--color-surface-2); color: var(--color-text-primary); }
.category-chip:focus-visible { outline: 2px solid var(--color-urban); outline-offset: 2px; }
.result-head { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); margin-bottom: var(--space-4); color: var(--color-text-muted); font-size: var(--text-sm); }
.result-head div { display: flex; align-items: center; gap: var(--space-1); }
.result-head strong { color: var(--color-vermilion); font-size: var(--text-lg); }
.pagination-wrap { position: fixed; z-index: var(--layer-floating); right: 0; bottom: 0; left: 0; display: flex; justify-content: center; padding: var(--space-3) var(--space-4) calc(var(--space-3) + env(safe-area-inset-bottom)); border-top: 1px solid var(--color-border-subtle); background: rgba(248,245,238,.96); }
.pagination-wrap :deep(.el-pagination) { max-width: 100%; flex-wrap: wrap; justify-content: center; gap: var(--space-1); }
.pagination-wrap :deep(.el-pagination button),.pagination-wrap :deep(.el-pager li) { min-width: 40px; min-height: 40px; }
@media (max-width: 1100px) { .square-layout { grid-template-columns: minmax(0, 1fr); gap: var(--space-6); } }
@media (max-width: 820px) { .square-shell { width: calc(100% - (2 * var(--page-gutter))); padding-top: calc(var(--nav-height) + var(--space-4)); } .category-chip,.pagination-wrap :deep(.el-pagination button),.pagination-wrap :deep(.el-pager li) { min-height: 44px; } .pagination-wrap :deep(.el-pagination button),.pagination-wrap :deep(.el-pager li) { min-width: 44px; } }
@media (max-width: 479px) { .category-strip { margin-bottom: var(--space-3); } .pagination-wrap { padding-inline: var(--space-2); } .pagination-wrap :deep(.el-pagination__total),.pagination-wrap :deep(.el-pagination__sizes) { display: none; } }
</style>
