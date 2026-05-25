<template>
  <div class="square-page">
    <NavBar />
    <div class="page-container">
      <header class="page-header">
        <div>
          <h1 class="page-title">Square</h1>
          <p class="page-desc">发现公开图片</p>
        </div>
        <div class="square-toolbar">
          <el-input
            v-model="query.keyword"
            placeholder="精确输入图片名称"
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
            v-model="query.tags"
            multiple
            filterable
            allow-create
            default-first-option
            collapse-tags
            collapse-tags-tooltip
            placeholder="输入或选择标签"
            class="square-tags"
            @change="onFilterChange"
          >
            <el-option v-for="tag in tagOptions" :key="tag" :label="tag" :value="tag" />
          </el-select>
          <el-select
            v-model="query.sortField"
            class="square-sort"
            clearable
            placeholder="排序"
            @change="onFilterChange"
          >
            <el-option label="按名称" value="image_name" />
            <el-option label="按时间" value="upload_time" />
            <el-option label="按大小" value="file_size" />
          </el-select>
        </div>
      </header>

      <div v-if="loading" class="empty-state">
        <div class="skeleton-grid">
          <div class="skeleton" v-for="n in 6" :key="n" style="aspect-ratio:1;"></div>
        </div>
      </div>

      <div v-else-if="images.length === 0" class="empty-state">
        <el-icon><PictureFilled /></el-icon>
        <p>图片广场暂时没有内容</p>
      </div>

      <div v-else class="card-grid">
        <div v-for="(img, idx) in images" :key="img.id" class="stagger-item" :style="{ animationDelay: `${idx * 0.06}s` }">
          <ImageCard :image="img" :show-actions="false" @delete="handleDeleteImage" />
        </div>
      </div>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.limit"
          :page-sizes="IMAGE_PAGE_SIZES"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="onPageSizeChange"
          @current-change="fetchList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import NavBar from '../components/NavBar.vue'
import ImageCard from '../components/ImageCard.vue'
import { getImageSquare, deleteImage } from '../api/image'
import { ElMessage } from 'element-plus'
import {
  buildSquareParams,
  createRandomSeed,
  loadSquareSession,
  saveSquareSession
} from '../utils/squareFilters'
import { DEFAULT_IMAGE_PAGE_SIZE, IMAGE_PAGE_SIZES } from '../utils/imageRequests'

const images = ref([])
const total = ref(0)
const loading = ref(false)
const savedQuery = loadSquareSession()
const query = reactive({
  page: savedQuery.page || 1,
  limit: savedQuery.limit || DEFAULT_IMAGE_PAGE_SIZE,
  keyword: savedQuery.keyword || '',
  tags: savedQuery.tags || [],
  sortField: savedQuery.sortField || '',
  randomSeed: createRandomSeed()
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
  return Array.from(tags)
})

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

function onPageSizeChange(size) {
  query.limit = size
  query.page = 1
  fetchList()
}

async function handleDeleteImage(id) {
  try {
    await deleteImage(id)
    ElMessage.success('已删除损坏图片')
    images.value = images.value.filter(img => img.id !== id)
    total.value = Math.max(0, total.value - 1)
  } catch {}
}
</script>

<style scoped>
.square-page {
  min-height: 100vh;
  background: var(--bg-base);
  display: flex;
  flex-direction: column;
}

.page-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.page-header {
  padding: 22px 24px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 16px;
  margin-bottom: 22px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}

.page-title {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 750;
  color: var(--text-primary);
  letter-spacing: -0.3px;
  line-height: 1.15;
  margin: 0;
}

.page-desc {
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--text-muted);
  margin-top: var(--space-xs);
}

.square-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  flex: 1;
}

.square-search {
  width: 220px;
}

.square-tags {
  width: 260px;
}

.square-sort {
  width: 120px;
}

.card-grid {
  flex: 1;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 28px 0 var(--space-lg);
  margin-top: auto;
  flex-shrink: 0;
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

@media (max-width: 768px) {
  .page-container { padding: 20px 8px; }
  .page-header { padding: 20px; border-radius: var(--radius-md); align-items: stretch; }
  .page-title { font-size: 26px; }
  .square-toolbar { justify-content: stretch; }
  .square-search,
  .square-tags,
  .square-sort {
    width: 100%;
  }
}
</style>
