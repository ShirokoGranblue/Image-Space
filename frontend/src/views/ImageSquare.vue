<template>
  <div class="square-page">
    <NavBar />
    <div class="page-container">
      <header class="page-header reveal">
        <div>
          <h1 class="page-title">Square</h1>
          <p class="page-desc">浏览社区公开作品，用标签发现同风格创作</p>
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
        <p>暂时没有内容</p>
      </div>

      <div v-else class="reveal visible square-results">
        <div class="card-grid">
          <div v-for="(img, idx) in images" :key="img.id" class="stagger-item" :style="{ animationDelay: `${idx * 0.06}s` }">
            <ImageCard :image="img" :show-actions="false" @delete="handleDeleteImage" />
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

async function handleDeleteImage(image) {
  const uuid = typeof image === 'object' ? image?.uuid : image
  if (!uuid) return
  try {
    await deleteImage(uuid)
    ElMessage.success('已删除损坏图片')
    images.value = images.value.filter(img => img.uuid !== uuid)
    total.value = Math.max(0, total.value - 1)
  } catch {}
}
</script>

<style scoped>
.square-page {
  min-height: 100vh;
  background: var(--white);
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
  padding: 0 0 20px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--gray2);
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 18px;
  flex-wrap: wrap;
}

.page-title {
  font-family: var(--font-display);
  font-size: 38px;
  font-weight: 400;
  color: var(--black);
  letter-spacing: 0.04em;
  line-height: 1;
  margin: 0;
}

.page-desc {
  font-size: 14px;
  color: var(--gray3);
  margin-top: 6px;
  font-weight: 400;
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
.square-tags { width: 260px; }
.square-sort { width: 120px; }

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
  background: rgba(250, 250, 250, 0.94);
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

@media (max-width: 768px) {
  .page-container { padding-bottom: 148px; }
  .page-header { flex-direction: column; align-items: stretch; }
  .page-title { font-size: 26px; }
  .square-toolbar { justify-content: stretch; }
  .square-search,
  .square-tags,
  .square-sort { width: 100%; }
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
