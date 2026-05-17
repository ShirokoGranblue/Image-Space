<template>
  <div class="square-page">
    <NavBar />
    <div class="page-container">
      <header class="page-header">
        <h1 class="page-title">Gallery</h1>
        <p class="page-desc">创作者公开作品</p>
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
          <ImageCard :image="img" :show-actions="false" />
        </div>
      </div>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          :page-size="limit"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import NavBar from '../components/NavBar.vue'
import ImageCard from '../components/ImageCard.vue'
import { getImageSquare } from '../api/image'

const images = ref([])
const page = ref(1)
const limit = 12
const total = ref(0)
const loading = ref(false)

onMounted(() => fetchList())

async function fetchList() {
  loading.value = true
  try {
    const res = await getImageSquare(page.value, limit)
    images.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {} finally {
    loading.value = false
  }
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
  padding-top: 28px;
}

.page-header {
  padding: 28px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  margin-bottom: 24px;
  background: var(--bg-surface);
  box-shadow: var(--shadow-md);
}

.page-title {
  font-family: var(--font-display);
  font-size: 34px;
  font-weight: 750;
  color: var(--text-primary);
  letter-spacing: -0.3px;
  line-height: 1.15;
}

.page-desc {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--text-muted);
  margin-top: var(--space-xs);
}

.card-grid {
  flex: 1;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: var(--space-xl) 0 var(--space-lg);
  margin-top: auto;
  flex-shrink: 0;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
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
  .page-container { padding: 20px var(--space-md); }
  .page-header { padding: 20px; border-radius: var(--radius-md); }
  .page-title { font-size: 26px; }
}
</style>
