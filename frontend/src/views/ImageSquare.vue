<template>
  <div class="public-square-page">
    <NavBar />

    <div class="square-shell">
      <header class="square-hero">
        <div class="square-hero-copy">
          <span class="eyebrow">公开广场</span>
          <h1>发现大家分享的图片。</h1>
          <p>按分类、标签或关键词浏览公开作品，也可以进入作者主页查看更多内容。</p>
        </div>

        <div class="square-hero-metrics" aria-label="公开广场概览">
          <div class="metric-cell">
            <strong>{{ total }}</strong>
            <span>公开图片</span>
          </div>
          <div class="metric-cell">
            <strong>{{ categoryOptions.length }}</strong>
            <span>分类</span>
          </div>
          <div class="metric-cell">
            <strong>{{ squareStats.totalLikes }}</strong>
            <span>喜欢</span>
          </div>
        </div>
      </header>

      <section class="square-command" aria-label="公开广场筛选">
        <el-input
          v-model="query.keyword"
          placeholder="搜索公开图片"
          clearable
          class="square-search"
          @clear="onFilterChange"
          @keyup.enter="onFilterChange"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="categoryFilter"
          clearable
          placeholder="选择分类"
          class="square-select"
        >
          <el-option v-for="cat in categoryOptions" :key="cat" :label="cat" :value="cat" />
        </el-select>

        <div class="sort-segment" role="group" aria-label="图片排序">
          <button
            v-for="option in sortOptions"
            :key="option.value"
            type="button"
            :class="{ active: viewMode === option.value }"
            @click="setViewMode(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
      </section>

      <div class="square-layout">
        <main class="square-main">
          <div class="category-strip" v-if="categoryOptions.length">
            <button
              type="button"
              class="category-chip"
              :class="{ active: !categoryFilter }"
              @click="categoryFilter = ''"
            >
              全部
            </button>
            <button
              v-for="cat in categoryOptions"
              :key="cat"
              type="button"
              class="category-chip"
              :class="{ active: categoryFilter === cat }"
              @click="categoryFilter = cat"
            >
              {{ cat }}
            </button>
          </div>

          <div class="result-head">
            <div>
              <span>当前显示</span>
              <strong>{{ displayedImages.length }}</strong>
              <span>张图片</span>
            </div>
            <button type="button" class="text-command" @click="clearFilters">
              清空筛选
            </button>
          </div>

          <section v-if="loading" class="square-loading" aria-label="图片加载中">
            <div class="asset-skeleton" v-for="n in 9" :key="n"></div>
          </section>

          <section v-else-if="displayedImages.length === 0" class="empty-state square-empty">
            <el-icon><PictureFilled /></el-icon>
            <p>没有符合条件的公开图片。</p>
          </section>

          <section v-else class="square-grid">
            <ImageCard
              v-for="img in displayedImages"
              :key="img.uuid || img.id"
              :image="img"
              variant="square"
              open-mode="emit"
              @view="openDrawer"
              @like="handleLike"
              @favorite="handleFavorite"
            />
          </section>
        </main>

        <aside class="square-side" aria-label="公开广场发现">
          <section class="side-section">
            <div class="side-title">
              <span>当前筛选</span>
              <strong>{{ activeSortLabel }}</strong>
            </div>
            <div class="filter-summary">
              <div>
                <span>分类</span>
                <strong>{{ categoryFilter || '不限' }}</strong>
              </div>
              <div>
                <span>标签</span>
                <strong>{{ query.tags[0] || '不限' }}</strong>
              </div>
              <div>
                <span>关键词</span>
                <strong>{{ query.keyword || '未填写' }}</strong>
              </div>
            </div>
          </section>

          <section class="side-section">
            <div class="side-title">
              <span>标签</span>
              <strong>{{ tagOptions.length }}</strong>
            </div>
            <div class="tag-cloud" v-if="tagOptions.length">
              <button
                v-for="tag in tagOptions"
                :key="tag"
                type="button"
                class="tag-pill"
                :class="{ active: query.tags.includes(tag) }"
                @click="setTag(tag)"
              >
                #{{ tag }}
              </button>
            </div>
            <p v-else class="side-empty">本页暂无标签。</p>
          </section>

          <section class="side-section">
            <div class="side-title">
              <span>作者</span>
              <strong>{{ activeCreators.length }}</strong>
            </div>
            <div class="creator-list" v-if="activeCreators.length">
              <button
                v-for="creator in activeCreators"
                :key="creator.username"
                type="button"
                class="creator-row"
                @click="goCreatorProfile(creator.uuidOrId)"
              >
                <span class="creator-avatar" :style="{ '--avatar-color': creator.avatarColor }">
                  {{ creator.initial }}
                </span>
                <span>
                  <strong>{{ creator.displayName || creator.username }}</strong>
                  <small>{{ creator.likeCount }} 次喜欢</small>
                </span>
              </button>
            </div>
            <p v-else class="side-empty">暂无作者信息。</p>
          </section>

          <section class="side-section inspector-section" v-if="featuredImage">
            <div class="side-title">
              <span>推荐</span>
              <strong>{{ featuredImage.categoryName || '未分类' }}</strong>
            </div>
            <button class="featured-preview" type="button" @click="openDrawer(featuredImage)">
              <img v-if="featuredImageSrc" :src="featuredImageSrc" :alt="featuredImage.imageName" />
              <span v-else>
                <el-icon><PictureFilled /></el-icon>
              </span>
            </button>
            <h2>{{ featuredImage.imageName }}</h2>
            <p>{{ featuredImage.displayName || featuredImage.username || '匿名用户' }}</p>
          </section>
        </aside>
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
      <aside class="image-drawer" v-if="drawerVisible && activeImage">
        <button class="drawer-close" type="button" @click="drawerVisible = false" aria-label="关闭">
          <el-icon><Close /></el-icon>
        </button>
        <img v-if="drawerImageSrc" :src="drawerImageSrc" :alt="activeImage.imageName" class="drawer-img" />
        <div v-else class="drawer-placeholder">
          <el-icon><PictureFilled /></el-icon>
        </div>
        <div class="drawer-body">
          <span class="drawer-kicker">{{ activeImage.categoryName || '未分类' }}</span>
          <h2>{{ activeImage.imageName }}</h2>
          <div class="drawer-meta">
            <span>{{ activeImage.displayName || activeImage.username || '匿名用户' }}</span>
            <span>{{ activeImage.likeCount || 0 }} 次喜欢</span>
            <span>{{ activeTags.length }} 个标签</span>
          </div>
          <p v-if="activeImage.description" class="drawer-desc">{{ activeImage.description }}</p>
          <div class="drawer-tags" v-if="activeTags.length">
            <button v-for="tag in activeTags" :key="tag" type="button" @click="setTag(tag)">
              #{{ tag }}
            </button>
          </div>
          <div class="drawer-actions">
            <button class="primary-command compact" type="button" @click="goDetail(activeImage)">查看详情</button>
            <button class="secondary-command" type="button" @click="handleLike(activeImage)">
              {{ activeImage.likedByMe ? '取消喜欢' : '喜欢' }}
            </button>
            <button class="secondary-command" type="button" @click="handleFavorite(activeImage)">收藏</button>
          </div>
        </div>
      </aside>
    </transition>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '../components/NavBar.vue'
import ImageCard from '../components/ImageCard.vue'
import { getImageSquare, likeImage, unlikeImage } from '../api/image'
import {
  buildSquareParams,
  createRandomSeed,
  loadSquareSession,
  saveSquareSession
} from '../utils/squareFilters'
import { DEFAULT_IMAGE_PAGE_SIZE, IMAGE_PAGE_SIZES, getImagePreviewUrl } from '../utils/imageRequests'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()
const images = ref([])
const total = ref(0)
const loading = ref(false)
const categoryFilter = ref('')
const viewMode = ref('featured')
const drawerVisible = ref(false)
const activeImage = ref(null)
const savedQuery = loadSquareSession()

const query = reactive({
  page: savedQuery.page || 1,
  limit: savedQuery.limit || DEFAULT_IMAGE_PAGE_SIZE,
  keyword: savedQuery.keyword || '',
  tags: savedQuery.tags || [],
  sortField: savedQuery.sortField || '',
  randomSeed: createRandomSeed()
})

const sortOptions = [
  { label: '推荐', value: 'featured' },
  { label: '最新', value: 'latest' },
  { label: '喜欢较多', value: 'hot' }
]
const CREATOR_TONES = ['#38d5ff', '#b7ff3c', '#f5b84b', '#9b8cff', '#ff6b57']

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
  return Array.from(tags).slice(0, 14)
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
  totalLikes: images.value.reduce((sum, image) => sum + Number(image.likeCount || 0), 0)
}))

const activeSortLabel = computed(() => sortOptions.find(option => option.value === viewMode.value)?.label || '推荐')
const featuredImage = computed(() => displayedImages.value[0] || images.value[0] || null)
const featuredImageSrc = computed(() => featuredImage.value ? getImagePreviewUrl(featuredImage.value) : '')
const drawerImageSrc = computed(() => activeImage.value ? getImagePreviewUrl(activeImage.value) : '')
const activeTags = computed(() => String(activeImage.value?.tags || '').split('#').map(tag => tag.trim()).filter(Boolean))

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
  query.tags = query.tags.includes(tag) ? [] : [tag]
  onFilterChange()
}

function clearFilters() {
  categoryFilter.value = ''
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
  ElMessage.info('收藏功能稍后开放')
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
.public-square-page {
  min-height: 100vh;
  color: var(--ad-text);
}

.square-shell {
  width: min(100% - 28px, 1600px);
  margin: 0 auto;
  padding: 96px 0 112px;
}

.square-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 440px);
  gap: 24px;
  align-items: stretch;
  min-height: 260px;
  border: 1px solid var(--ad-line);
  border-radius: 18px 18px 0 0;
  background:
    linear-gradient(135deg, rgba(21, 25, 34, 0.96), rgba(13, 16, 22, 0.92)),
    linear-gradient(90deg, rgba(183, 255, 60, 0.08), rgba(56, 213, 255, 0.08));
}

.square-hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 38px;
}

.eyebrow {
  width: fit-content;
  margin-bottom: 16px;
  padding: 5px 9px;
  border: 1px solid rgba(183, 255, 60, 0.28);
  border-radius: 999px;
  background: rgba(183, 255, 60, 0.08);
  color: var(--ad-green);
  font-size: 11px;
  font-weight: 860;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.square-hero h1 {
  max-width: 860px;
  margin: 0;
  color: var(--ad-text);
  font-size: clamp(40px, 6vw, 82px);
  font-weight: 340;
  line-height: 0.92;
}

.square-hero p {
  max-width: 680px;
  margin: 18px 0 0;
  color: var(--ad-text-soft);
  font-size: 18px;
  line-height: 1.55;
}

.square-hero-metrics {
  display: grid;
  grid-template-columns: 1fr;
  border-left: 1px solid var(--ad-line);
}

.metric-cell {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 22px 26px;
  border-bottom: 1px solid var(--ad-line);
}

.metric-cell:last-child {
  border-bottom: 0;
}

.metric-cell strong {
  color: var(--ad-text);
  font-size: 38px;
  font-weight: 860;
  line-height: 1;
}

.metric-cell span {
  margin-top: 7px;
  color: var(--ad-muted);
  font-size: 12px;
  font-weight: 820;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.square-command {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 220px auto;
  gap: 10px;
  padding: 12px;
  border-right: 1px solid var(--ad-line);
  border-left: 1px solid var(--ad-line);
  background: rgba(17, 23, 34, 0.72);
}

.square-search,
.square-select {
  min-width: 0;
}

.sort-segment {
  display: inline-grid;
  grid-template-columns: repeat(3, minmax(72px, 1fr));
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--ad-line);
  border-radius: 12px;
  background: rgba(13, 16, 22, 0.56);
}

.sort-segment button,
.category-chip,
.tag-pill,
.text-command,
.creator-row,
.featured-preview,
.primary-command,
.secondary-command {
  font-family: var(--ad-font);
}

.sort-segment button {
  min-height: 34px;
  padding: 0 12px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--ad-text-soft);
  font-size: 13px;
  font-weight: 760;
  cursor: pointer;
}

.sort-segment button.active,
.sort-segment button:hover {
  background: var(--ad-green);
  color: #071014;
}

.square-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  border: 1px solid var(--ad-line);
  border-radius: 0 0 18px 18px;
  background: rgba(17, 23, 34, 0.54);
}

.square-main {
  min-width: 0;
  padding: 18px;
}

.category-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-bottom: 16px;
}

.category-chip {
  min-height: 30px;
  padding: 0 11px;
  border: 1px solid var(--ad-line);
  border-radius: 999px;
  background: rgba(13, 16, 22, 0.42);
  color: var(--ad-text-soft);
  font-size: 13px;
  font-weight: 760;
  cursor: pointer;
}

.category-chip.active,
.category-chip:hover {
  border-color: rgba(183, 255, 60, 0.48);
  background: rgba(183, 255, 60, 0.1);
  color: var(--ad-text);
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  color: var(--ad-muted);
  font-size: 13px;
  font-weight: 760;
}

.result-head div {
  display: flex;
  align-items: center;
  gap: 6px;
}

.result-head strong {
  color: var(--ad-green);
  font-size: 18px;
}

.text-command {
  border: 0;
  background: transparent;
  color: var(--ad-green);
  font-size: 13px;
  font-weight: 780;
  cursor: pointer;
}

.square-grid,
.square-loading {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}

.asset-skeleton {
  min-height: 300px;
  border: 1px solid var(--ad-line);
  border-radius: 14px;
  background:
    linear-gradient(90deg, rgba(244, 241, 232, 0.06), rgba(244, 241, 232, 0.13), rgba(244, 241, 232, 0.06));
  background-size: 220% 100%;
  animation: shimmer 1.3s ease-in-out infinite;
}

.square-empty {
  min-height: 340px;
  border: 1px solid var(--ad-line);
  border-radius: 14px;
  background: rgba(13, 16, 22, 0.38);
}

.square-side {
  display: flex;
  flex-direction: column;
  gap: 0;
  border-left: 1px solid var(--ad-line);
  background: rgba(13, 16, 22, 0.38);
}

.side-section {
  padding: 17px;
  border-bottom: 1px solid var(--ad-line);
}

.side-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.side-title span {
  color: rgba(244, 241, 232, 0.5);
  font-size: 11px;
  font-weight: 860;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.side-title strong {
  color: var(--ad-text);
  font-size: 12px;
  font-weight: 820;
}

.filter-summary {
  display: grid;
  gap: 8px;
}

.filter-summary div {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  min-height: 32px;
  padding: 0 10px;
  border: 1px solid var(--ad-line);
  border-radius: 10px;
  background: rgba(21, 25, 34, 0.54);
}

.filter-summary span {
  color: var(--ad-muted);
  font-size: 12px;
  font-weight: 720;
}

.filter-summary strong {
  overflow: hidden;
  color: var(--ad-text);
  font-size: 13px;
  font-weight: 780;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.tag-pill {
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--ad-line);
  border-radius: 999px;
  background: rgba(21, 25, 34, 0.58);
  color: var(--ad-text-soft);
  font-size: 12px;
  font-weight: 760;
  cursor: pointer;
}

.tag-pill.active,
.tag-pill:hover {
  border-color: rgba(56, 213, 255, 0.54);
  background: rgba(56, 213, 255, 0.1);
  color: var(--ad-text);
}

.creator-list {
  display: grid;
  gap: 8px;
}

.creator-row {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  width: 100%;
  min-height: 44px;
  padding: 7px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--ad-text);
  text-align: left;
  cursor: pointer;
}

.creator-row:hover {
  border-color: var(--ad-line);
  background: rgba(244, 241, 232, 0.05);
}

.creator-avatar {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: var(--avatar-color);
  color: #071014;
  font-size: 14px;
  font-weight: 860;
}

.creator-row strong,
.creator-row small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.creator-row strong {
  font-size: 13px;
  font-weight: 820;
}

.creator-row small,
.side-empty,
.inspector-section p {
  color: var(--ad-muted);
  font-size: 12px;
  font-weight: 680;
}

.featured-preview {
  overflow: hidden;
  width: 100%;
  aspect-ratio: 4 / 3;
  padding: 0;
  border: 1px solid var(--ad-line);
  border-radius: 12px;
  background: rgba(21, 25, 34, 0.66);
  cursor: pointer;
}

.featured-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.featured-preview span {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--ad-muted);
  font-size: 34px;
}

.inspector-section h2 {
  margin: 12px 0 4px;
  color: var(--ad-text);
  font-size: 18px;
  font-weight: 420;
  line-height: 1.2;
}

.inspector-section p {
  margin: 0;
}

.pagination-wrap {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 150;
  display: flex;
  justify-content: center;
  padding: 12px 24px calc(12px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--ad-line);
  background: rgba(13, 16, 22, 0.9);
  backdrop-filter: blur(18px);
}

.pagination-wrap :deep(.el-pagination) {
  max-width: min(100%, 1280px);
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
}

.image-drawer {
  position: fixed;
  top: 88px;
  right: 22px;
  bottom: 22px;
  z-index: 170;
  overflow: hidden auto;
  width: 420px;
  max-width: calc(100vw - 32px);
  border: 1px solid var(--ad-line);
  border-radius: 16px;
  background: rgba(21, 25, 34, 0.98);
  box-shadow: var(--ad-shadow);
}

.drawer-close {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  width: 36px;
  height: 36px;
  border: 1px solid var(--ad-line-strong);
  border-radius: 10px;
  background: rgba(13, 16, 22, 0.74);
  color: var(--ad-text);
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
  background: rgba(13, 16, 22, 0.76);
}

.drawer-placeholder {
  display: grid;
  place-items: center;
  color: var(--ad-muted);
  font-size: 42px;
}

.drawer-body {
  padding: 20px;
}

.drawer-kicker {
  display: inline-flex;
  margin-bottom: 10px;
  padding: 4px 8px;
  border: 1px solid var(--ad-line);
  border-radius: 999px;
  color: var(--ad-green);
  font-size: 11px;
  font-weight: 860;
  letter-spacing: 0.09em;
  text-transform: uppercase;
}

.drawer-body h2 {
  margin: 0;
  color: var(--ad-text);
  font-size: 30px;
  font-weight: 420;
  line-height: 1.15;
}

.drawer-meta,
.drawer-tags,
.drawer-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.drawer-meta span,
.drawer-tags button {
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border: 1px solid var(--ad-line);
  border-radius: 999px;
  background: rgba(13, 16, 22, 0.45);
  color: var(--ad-text-soft);
  font-size: 12px;
  font-weight: 760;
}

.drawer-tags button {
  cursor: pointer;
}

.drawer-desc {
  margin: 16px 0 0;
  color: var(--ad-text-soft);
  font-size: 15px;
  line-height: 1.7;
}

.primary-command,
.secondary-command {
  min-height: 38px;
  padding: 0 13px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 820;
  cursor: pointer;
}

.primary-command {
  border: 1px solid var(--ad-green);
  background: var(--ad-green);
  color: #071014;
}

.secondary-command {
  border: 1px solid var(--ad-line);
  background: rgba(244, 241, 232, 0.06);
  color: var(--ad-text);
}

.compact {
  min-height: 36px;
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

@keyframes shimmer {
  0% { background-position: 180% 0; }
  100% { background-position: -40% 0; }
}

@media (max-width: 1100px) {
  .square-hero,
  .square-layout {
    grid-template-columns: 1fr;
  }

  .square-hero-metrics {
    grid-template-columns: repeat(3, 1fr);
    border-top: 1px solid var(--ad-line);
    border-left: 0;
  }

  .metric-cell {
    border-right: 1px solid var(--ad-line);
    border-bottom: 0;
  }

  .metric-cell:last-child {
    border-right: 0;
  }

  .square-side {
    border-top: 1px solid var(--ad-line);
    border-left: 0;
  }
}

@media (max-width: 820px) {
  .square-shell {
    width: min(100% - 20px, 1600px);
    padding-top: 88px;
  }

  .square-hero-copy {
    padding: 28px 20px;
  }

  .square-hero h1 {
    font-size: 40px;
  }

  .square-command {
    grid-template-columns: 1fr;
  }

  .sort-segment {
    grid-template-columns: repeat(3, 1fr);
  }

  .square-main {
    padding: 14px;
  }

  .square-grid,
  .square-loading {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 10px;
  }

  .square-hero-metrics {
    grid-template-columns: 1fr;
  }

  .metric-cell {
    border-right: 0;
    border-bottom: 1px solid var(--ad-line);
  }

  .metric-cell:last-child {
    border-bottom: 0;
  }

  .image-drawer {
    top: 78px;
    right: 10px;
    bottom: 10px;
    max-width: calc(100vw - 20px);
  }

  .pagination-wrap {
    padding: 10px 10px calc(10px + env(safe-area-inset-bottom));
  }
}
</style>
